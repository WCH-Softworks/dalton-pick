package com.wchsoftworks.daltonpick.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings.Global
import android.util.Log
import android.view.Choreographer
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.capture.ScreenCapturer
import com.wchsoftworks.daltonpick.capture.Snapshot
import com.wchsoftworks.daltonpick.colorengine.ColorLabel
import com.wchsoftworks.daltonpick.colorengine.ColorLocalization
import com.wchsoftworks.daltonpick.colorengine.HysteresisNamer
import com.wchsoftworks.daltonpick.colorengine.Lang
import com.wchsoftworks.daltonpick.colorengine.Srgb
import com.wchsoftworks.daltonpick.settings.Edge
import com.wchsoftworks.daltonpick.settings.Settings

/**
 * Owns the overlay windows (edge tab + lens) and the snapshot-on-pull flow (spec §5.2).
 * The same controller runs on either capture backend; only [windowType] and [capturer] differ.
 *
 * All state comes from [apply]: the host feeds it every [Settings] change, and the controller
 * writes back through [persist] (tab position and edge).
 */
class OverlayController(
    private val context: Context,
    private val windowType: Int,
    private val capturer: ScreenCapturer,
    private val persist: ((Settings) -> Settings) -> Unit,
) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val accessibility = context.getSystemService(AccessibilityManager::class.java)
    private val density = context.resources.displayMetrics.density
    private val main = Handler(Looper.getMainLooper())
    private val namer = HysteresisNamer()

    private var settings = Settings()

    private val tab = EdgeTabView(context, this)
    private val tabWidthPx = (EdgeTabView.TOUCH_WIDTH_DP * density).toInt()
    private val tabHeightPx = (EdgeTabView.HEIGHT_DP * density).toInt()
    private val tabParams = overlayParams(tabWidthPx, tabHeightPx)
    private var lens: LensView? = null
    private var lensParams: WindowManager.LayoutParams? = null
    private var snapshot: Snapshot? = null

    private var showing = false
    private var capturing = false
    private var centerX = 0f
    private var centerY = 0f
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var lastAnnounced: String? = null
    private var snapAnimator: ValueAnimator? = null

    val tabY: Int get() = tabParams.y

    private val screen get() = wm.currentWindowMetrics.bounds
    private val lang get() = Lang.fromTag(context.resources.configuration.locales[0].toLanguageTag())
    private val onRight get() = settings.edge == Edge.RIGHT

    /** Screen minus system bars and display cutouts: where the label and tab must stay. */
    private val safe: Rect
        get() {
            val insets = wm.currentWindowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            return Rect(insets.left, insets.top, insets.right, insets.bottom)
        }

    /** Lens floats this far above the finger while pulling, so the finger doesn't cover it. */
    private val pullOffsetY get() = (settings.lensSizeDp / 2 + 32) * density

    private fun overlayParams(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h, windowType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }

    // --- Settings and lifecycle

    fun apply(new: Settings) {
        val old = settings
        settings = new

        if (!new.tabVisible) {
            hide()
            return
        }
        if (!showing) {
            show()
        } else if (new.edge != old.edge) {
            dockTab(animated = false)
        }
        if (new.autoDim != old.autoDim) wakeTab()

        val view = lens ?: return
        if (new.lensSizeDp != old.lensSizeDp || new.showHex != old.showHex || new.showDebug != old.showDebug) {
            rebuildLens()
        } else if (new.zoom != old.zoom || new.sampleSize != old.sampleSize) {
            view.zoom = new.zoom.toFloat()
            view.sampleRadius = new.sampleRadius
            updateSample(view)
        }
    }

    private fun show() {
        if (showing) return
        tab.onRightEdge = onRight
        tabParams.y = tabYFromFraction(settings.tabPosition)
        tabParams.x = dockedX(onRight)
        wm.addView(tab, tabParams)
        showing = true
        wakeTab()
    }

    private fun hide() {
        dismissLens(animated = false)
        main.removeCallbacks(dimTab)
        snapAnimator?.cancel()
        if (showing) wm.removeView(tab)
        showing = false
    }

    fun destroy() = hide()

    /** Rotation, fold or window resize: the snapshot no longer matches the screen. */
    fun onScreenChanged() {
        dismissLens(animated = false)
        capturer.onScreenChanged()
        if (!showing) return
        snapAnimator?.cancel()
        tabParams.y = tabYFromFraction(settings.tabPosition)
        tabParams.x = dockedX(onRight)
        wm.updateViewLayout(tab, tabParams)
    }

    // --- Edge tab

    private fun dockedX(right: Boolean) = if (right) screen.width() - tabWidthPx else 0

    private fun tabYRange(): IntRange {
        val s = safe
        val min = s.top
        val max = (screen.height() - s.bottom - tabHeightPx).coerceAtLeast(min)
        return min..max
    }

    private fun tabYFromFraction(fraction: Float) =
        (fraction * screen.height()).toInt().coerceIn(tabYRange())

    private fun fractionOf(y: Int) = y.toFloat() / screen.height()

    fun moveTabTo(y: Int) {
        tabParams.y = y.coerceIn(tabYRange())
        wm.updateViewLayout(tab, tabParams)
    }

    fun saveTabPosition() {
        val fraction = fractionOf(tabParams.y)
        persist { it.copy(tabPosition = fraction) }
    }

    /** Tap: put the lens away, or open it at screen centre (also what TalkBack's double-tap does). */
    fun tapTab() {
        if (lens != null) {
            dismissLens()
        } else {
            val b = screen
            openLensAt(b.width() / 2f, b.height() / 2f)
        }
    }

    fun switchEdge() {
        persist { it.copy(edge = if (it.edge == Edge.RIGHT) Edge.LEFT else Edge.RIGHT) }
    }

    fun beginCarry() {
        snapAnimator?.cancel()
    }

    fun carryTabTo(rawX: Float, rawY: Float) {
        tabParams.x = (rawX - tabWidthPx / 2f).toInt().coerceIn(0, screen.width() - tabWidthPx)
        tabParams.y = (rawY - tabHeightPx / 2f).toInt().coerceIn(tabYRange())
        wm.updateViewLayout(tab, tabParams)
    }

    /** Snaps to whichever edge is nearer and remembers it. */
    fun endCarry(rawX: Float, rawY: Float) {
        val right = rawX > screen.width() / 2f
        val edge = if (right) Edge.RIGHT else Edge.LEFT
        val fraction = fractionOf(tabParams.y)
        settings = settings.copy(edge = edge, tabPosition = fraction)
        tab.onRightEdge = right
        dockTab(animated = true)
        persist { it.copy(edge = edge, tabPosition = fraction) }
    }

    private fun dockTab(animated: Boolean) {
        tab.onRightEdge = onRight
        val target = dockedX(onRight)
        snapAnimator?.cancel()
        if (!animated || !animationsEnabled()) {
            tabParams.x = target
            wm.updateViewLayout(tab, tabParams)
            return
        }
        snapAnimator = ValueAnimator.ofInt(tabParams.x, target).apply {
            duration = SNAP_MS
            addUpdateListener {
                if (!showing) return@addUpdateListener
                tabParams.x = it.animatedValue as Int
                wm.updateViewLayout(tab, tabParams)
            }
            start()
        }
    }

    // --- Auto-dim

    private val dimTab = Runnable {
        tab.animate().alpha(DIMMED_ALPHA).setDuration(DIM_FADE_MS).start()
    }

    /** Full opacity now; dims again after [DIM_AFTER_MS] without interaction (spec §4.1). */
    private fun wakeTab() {
        main.removeCallbacks(dimTab)
        tab.animate().cancel()
        tab.alpha = 1f
        if (settings.autoDim && showing) main.postDelayed(dimTab, DIM_AFTER_MS)
    }

    fun onTabTouched() {
        main.removeCallbacks(dimTab)
        tab.animate().cancel()
        tab.alpha = 1f
    }

    fun onTabReleased() = wakeTab()

    // --- Pull

    fun startPull(rawX: Float, rawY: Float) {
        centerX = rawX
        centerY = rawY - pullOffsetY
        takeSnapshot { showLens() }
    }

    private fun openLensAt(x: Float, y: Float) {
        centerX = x
        centerY = y
        takeSnapshot {
            showLens()
            announceSoon()
        }
    }

    fun dragPull(rawX: Float, rawY: Float) {
        centerX = rawX
        centerY = rawY - pullOffsetY
        if (lens != null) placeLens()
    }

    fun endPull() = announceSoon()

    // --- Lens

    fun beginLensDrag() {
        dragStartX = centerX
        dragStartY = centerY
        wakeTab()
    }

    fun dragLensBy(dx: Float, dy: Float) {
        centerX = dragStartX + dx
        centerY = dragStartY + dy
        placeLens()
    }

    fun endLensDrag() = announceSoon()

    fun isFlingTowardTab(velocityX: Float): Boolean {
        val threshold = 2500 * density
        return if (onRight) velocityX > threshold else velocityX < -threshold
    }

    fun refresh() = takeSnapshot { placeLens() }

    fun dismissLens(animated: Boolean = true) {
        val view = lens ?: return
        lens = null
        lensParams = null
        val snap = snapshot
        snapshot = null
        namer.reset()
        main.removeCallbacks(announce)
        lastAnnounced = null

        val release = Runnable {
            if (view.isAttachedToWindow) wm.removeView(view)
            snap?.recycle()
        }
        if (animated && showing && animationsEnabled()) {
            view.animate().alpha(0f).scaleX(0.3f).scaleY(0.3f).setDuration(DISMISS_MS)
                .withEndAction(release).start()
        } else {
            view.animate().cancel()
            release.run()
        }
    }

    private fun showLens() {
        if (lens != null) return placeLens()
        val view = LensView(context, this, settings)
        val params = overlayParams(view.windowWidthPx, view.windowHeightPx)
        lens = view
        lensParams = params
        positionLens(view, params)
        wm.addView(view, params)
        updateSample(view)
        animateIn(view)
    }

    /** Settings that change the lens's size or label layout: rebuild it around the same snapshot. */
    private fun rebuildLens() {
        val old = lens ?: return
        old.animate().cancel()
        wm.removeView(old)
        lens = null
        lensParams = null
        showLens()
    }

    /** Spring from a small, faded lens to full size (skipped when the user turned animations off). */
    private fun animateIn(view: LensView) {
        if (!animationsEnabled()) return
        view.alpha = 0f
        view.scaleX = 0.25f
        view.scaleY = 0.25f
        view.animate().alpha(1f).setDuration(120).start()
        for (property in arrayOf(DynamicAnimation.SCALE_X, DynamicAnimation.SCALE_Y)) {
            SpringAnimation(view, property, 1f).apply {
                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                spring.stiffness = SpringForce.STIFFNESS_MEDIUM
                start()
            }
        }
    }

    private fun animationsEnabled() =
        Global.getFloat(context.contentResolver, Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

    private fun placeLens() {
        val view = lens ?: return
        val params = lensParams ?: return
        positionLens(view, params)
        wm.updateViewLayout(view, params)
        updateSample(view)
        announceSoon()
    }

    /**
     * Keeps the lens centre on the screen and the whole label inside the safe area. The label goes
     * below the lens unless that would run past the bottom, then above.
     */
    private fun positionLens(view: LensView, params: WindowManager.LayoutParams) {
        val bounds = screen
        val s = safe
        centerX = centerX.coerceIn(0f, bounds.width() - 1f)
        centerY = centerY.coerceIn(0f, bounds.height() - 1f)
        val r = view.lensSizePx / 2f
        val belowFits = centerY + r + view.gapPx + view.labelHeightPx <= bounds.height() - s.bottom
        view.labelAbove = !belowFits
        val minX = s.left
        val maxX = maxOf(minX, bounds.width() - s.right - view.windowWidthPx)
        params.x = (centerX - view.windowWidthPx / 2f).toInt().coerceIn(minX, maxX)
        view.lensCenterX = centerX - params.x
        params.y = if (view.labelAbove) {
            (centerY - r - view.gapPx - view.labelHeightPx).toInt()
        } else {
            (centerY - r).toInt()
        }
        view.pivotX = view.lensCenterX
        view.pivotY = view.lensCenterY
    }

    private fun updateSample(view: LensView) {
        val snap = snapshot ?: return
        val bounds = screen
        val bx = (centerX * snap.bitmap.width / bounds.width()).toInt()
        val by = (centerY * snap.bitmap.height / bounds.height()).toInt()
        val rgb = snap.sampleSrgb(bx, by, view.sampleRadius)
        val label = if (snap.looksProtected) {
            ColorLabel(context.getString(R.string.protected_content), "")
        } else {
            ColorLocalization.label(namer.match(rgb), lang)
        }
        val changed = label.name != view.label?.name
        view.bind(snap, bx, by, rgb, label, debugLine = "${snap.describe()} · ${Srgb.toHex(rgb)}")
        if (changed && settings.haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    // --- TalkBack: announce the name once the lens has settled, and only if it changed.

    private val announce = Runnable {
        val view = lens ?: return@Runnable
        val label = view.label ?: return@Runnable
        if (label.name == lastAnnounced) return@Runnable
        lastAnnounced = label.name
        @Suppress("DEPRECATION")
        view.announceForAccessibility(view.contentDescription)
    }

    private fun announceSoon() {
        main.removeCallbacks(announce)
        if (accessibility.isTouchExplorationEnabled) main.postDelayed(announce, ANNOUNCE_DEBOUNCE_MS)
    }

    // --- Snapshot-on-pull

    private fun takeSnapshot(then: () -> Unit) {
        if (capturing) return
        capturing = true
        setOverlayAlpha(0f)

        val capture = {
            capturer.capture { result ->
                capturing = false
                setOverlayAlpha(1f)
                result.onSuccess { snap ->
                    val old = snapshot
                    snapshot = snap
                    old?.recycle()
                    namer.reset()
                    Log.i(TAG, "capture ok: ${snap.describe()}")
                    then()
                }.onFailure { e ->
                    Log.w(TAG, "capture failed", e)
                    Toast.makeText(context, context.getString(R.string.capture_failed, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Wait two frames (plus a short delay) so the hidden overlay has left the screen.
        val choreographer = Choreographer.getInstance()
        choreographer.postFrameCallback {
            choreographer.postFrameCallback { main.postDelayed(capture, EXTRA_CAPTURE_DELAY_MS) }
        }
    }

    private fun setOverlayAlpha(alpha: Float) {
        tab.animate().cancel()
        tab.alpha = alpha
        lens?.alpha = alpha
        if (alpha == 1f) wakeTab()
    }

    companion object {
        private const val TAG = "DaltonPick"
        private const val DIM_AFTER_MS = 3000L
        private const val DIM_FADE_MS = 300L
        private const val DIMMED_ALPHA = 0.3f
        private const val SNAP_MS = 160L
        private const val DISMISS_MS = 140L
        private const val ANNOUNCE_DEBOUNCE_MS = 700L
        private const val EXTRA_CAPTURE_DELAY_MS = 32L
    }
}
