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
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.capture.CaptureException
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
    /** The system refused an overlay window (e.g. "display over other apps" was revoked). */
    private val onWindowsLost: () -> Unit,
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

    /** Screen minus system bars only. Sideways, the lens may cross a cutout band; it just avoids the cutout itself. */
    private val bars: Rect
        get() {
            val insets = wm.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            return Rect(insets.left, insets.top, insets.right, insets.bottom)
        }

    /** The camera holes and notches themselves, in screen coordinates. */
    private val cutouts: List<Rect>
        get() = wm.currentWindowMetrics.windowInsets.displayCutout?.boundingRects.orEmpty()

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

    /** Adds an overlay window; false (and [onWindowsLost]) when the system refuses it. */
    private fun addWindow(view: View, params: WindowManager.LayoutParams): Boolean = try {
        wm.addView(view, params)
        true
    } catch (e: RuntimeException) {
        // BadTokenException or SecurityException: the overlay permission is gone.
        Log.w(TAG, "overlay window refused", e)
        onWindowsLost()
        false
    }

    /** Moves or resizes a window, ignoring one the system has already taken away. */
    private fun relayout(view: View, params: WindowManager.LayoutParams) {
        try {
            wm.updateViewLayout(view, params)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "window no longer attached", e)
        }
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
        if (!addWindow(tab, tabParams)) return
        showing = true
        wakeTab()
    }

    private fun hide() {
        if (capturing) {
            captureId++ // drop the pending result
            capturing = false
            main.removeCallbacks(captureTimeout)
        }
        dismissLens(animated = false)
        main.removeCallbacks(dimTab)
        snapAnimator?.cancel()
        if (showing && tab.isAttachedToWindow) wm.removeView(tab)
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
        relayout(tab, tabParams)
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
        clearOfCutout((fraction * screen.height()).toInt().coerceIn(tabYRange()), dockedX(onRight))

    /**
     * In landscape a camera hole can sit on the tab's edge. Slides the tab just past it, to
     * whichever side is nearer and still on screen.
     */
    private fun clearOfCutout(y: Int, x: Int): Int {
        val range = tabYRange()
        val hole = cutouts.firstOrNull { Rect.intersects(it, Rect(x, y, x + tabWidthPx, y + tabHeightPx)) } ?: return y
        val above = hole.top - tabHeightPx
        val below = hole.bottom
        val options = listOf(above, below).filter { it in range }
        return options.minByOrNull { kotlin.math.abs(it - y) } ?: y
    }

    private fun fractionOf(y: Int) = y.toFloat() / screen.height()

    fun moveTabTo(y: Int) {
        tabParams.y = y.coerceIn(tabYRange())
        relayout(tab, tabParams)
    }

    fun saveTabPosition() {
        settleTab()
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
        relayout(tab, tabParams)
    }

    /** Snaps to whichever edge is nearer and remembers it. */
    fun endCarry(rawX: Float, rawY: Float) {
        val right = rawX > screen.width() / 2f
        val edge = if (right) Edge.RIGHT else Edge.LEFT
        settings = settings.copy(edge = edge)
        dockTab(animated = true)
        val fraction = fractionOf(tabParams.y)
        settings = settings.copy(tabPosition = fraction)
        persist { it.copy(edge = edge, tabPosition = fraction) }
    }

    /** After a drag along the edge: if the tab ended up under a camera hole, move it clear. */
    private fun settleTab() {
        val y = clearOfCutout(tabParams.y, tabParams.x)
        if (y == tabParams.y) return
        tabParams.y = y
        relayout(tab, tabParams)
    }

    private fun dockTab(animated: Boolean) {
        tab.onRightEdge = onRight
        val target = dockedX(onRight)
        tabParams.y = clearOfCutout(tabParams.y, target)
        snapAnimator?.cancel()
        if (!animated || !animationsEnabled()) {
            tabParams.x = target
            relayout(tab, tabParams)
            return
        }
        snapAnimator = ValueAnimator.ofInt(tabParams.x, target).apply {
            duration = SNAP_MS
            addUpdateListener {
                if (!showing) return@addUpdateListener
                tabParams.x = it.animatedValue as Int
                relayout(tab, tabParams)
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

    /** A finger is on the lens. Automatic refreshes wait for it to lift, so the lens doesn't blink mid-drag. */
    private var lensTouched = false

    fun beginLensDrag() {
        dragStartX = centerX
        dragStartY = centerY
        lensTouched = true
        wakeTab()
    }

    /** [precise] drags never offer the drop zone: that's how to sample right at the edge. */
    fun dragLensBy(dx: Float, dy: Float, precise: Boolean) {
        centerX = dragStartX + dx
        centerY = dragStartY + dy
        placeLens()
        // Fade the lens over the drop zone, so letting go there clearly puts it away.
        lens?.alpha = if (!precise && inDropZone()) DROP_ZONE_ALPHA else 1f
    }

    fun endLensDrag() = announceSoon()

    fun lensTouchEnded() {
        lensTouched = false
        lens?.alpha = 1f
        if (refreshWaiting) {
            refreshWaiting = false
            scheduleRefresh(SETTLE_AFTER_TOUCH_MS)
        }
    }

    /** The lens centre is at the tab's edge: releasing it there puts it away. */
    fun inDropZone(): Boolean {
        val zone = DROP_ZONE_DP * density
        return if (onRight) centerX >= screen.width() - zone else centerX <= zone
    }

    fun isFlingTowardTab(velocityX: Float): Boolean {
        val threshold = FLING_DP_PER_S * density
        return if (onRight) velocityX > threshold else velocityX < -threshold
    }

    fun refresh() = takeSnapshot { placeLens() }

    // --- Keeping the snapshot current. The app underneath can scroll or change screen while the
    // lens is open (the user lets go of the lens and scrolls), so take a new snapshot once it settles.

    private var refreshWaiting = false

    private val autoRefresh = Runnable {
        when {
            lens == null -> Unit
            lensTouched -> refreshWaiting = true
            capturing -> scheduleRefresh(SETTLE_AFTER_SCROLL_MS)
            else -> refresh()
        }
    }

    /** The accessibility service saw the app underneath scroll or change screen. */
    fun onContentChanged() = scheduleRefresh(SETTLE_AFTER_SCROLL_MS)

    /** A touch landed outside the lens: probably a scroll or tap on the app underneath. */
    fun onOutsideTouch() = scheduleRefresh(SETTLE_AFTER_TOUCH_MS)

    private fun scheduleRefresh(delayMs: Long) {
        if (lens == null) return
        main.removeCallbacks(autoRefresh)
        main.postDelayed(autoRefresh, delayMs)
    }

    fun dismissLens(animated: Boolean = true) {
        val view = lens ?: return
        lens = null
        lensParams = null
        val snap = snapshot
        snapshot = null
        namer.reset()
        main.removeCallbacks(announce)
        main.removeCallbacks(autoRefresh)
        refreshWaiting = false
        lensTouched = false
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
        val params = overlayParams(view.windowWidthPx, view.windowHeightPx).apply {
            // One ACTION_OUTSIDE per touch elsewhere: the cue that the app underneath may have moved.
            flags = flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        }
        positionLens(view, params)
        if (!addWindow(view, params)) return
        lens = view
        lensParams = params
        updateSample(view)
        animateIn(view)
    }

    /** Settings that change the lens's size or label layout: rebuild it around the same snapshot. */
    private fun rebuildLens() {
        val old = lens ?: return
        old.animate().cancel()
        if (old.isAttachedToWindow) wm.removeView(old)
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
        relayout(view, params)
        updateSample(view)
        announceSoon()
    }

    /**
     * Keeps the lens centre on the screen and the whole label clear of the system bars and of any
     * camera cutout. The label goes below the lens unless that would run past the bottom or onto a
     * cutout, then above.
     *
     * Sideways the window only avoids the system bars, not the whole cutout band: in landscape that
     * band runs the full height of the screen, and clamping to it would clip the lens well short of
     * the screen edge.
     */
    private fun positionLens(view: LensView, params: WindowManager.LayoutParams) {
        val bounds = screen
        val s = safe
        val b = bars
        centerX = centerX.coerceIn(0f, bounds.width() - 1f)
        centerY = centerY.coerceIn(0f, bounds.height() - 1f)
        val r = view.lensSizePx / 2f

        val minX = b.left
        val maxX = maxOf(minX, bounds.width() - b.right - view.windowWidthPx)
        params.x = (centerX - view.windowWidthPx / 2f).toInt().coerceIn(minX, maxX)
        view.lensCenterX = centerX - params.x

        val belowTop = (centerY + r + view.gapPx).toInt()
        val aboveTop = (centerY - r - view.gapPx - view.labelHeightPx).toInt()
        val belowOnScreen = belowTop + view.labelHeightPx <= bounds.height() - s.bottom
        val belowFits = belowOnScreen && !hitsCutout(params.x, belowTop, view)
        val aboveFits = aboveTop >= s.top && !hitsCutout(params.x, aboveTop, view)
        view.labelAbove = when {
            belowFits -> false
            aboveFits -> true
            else -> !belowOnScreen // Neither is clear: at least stay on screen.
        }
        params.y = if (view.labelAbove) aboveTop else (centerY - r).toInt()
        view.pivotX = view.lensCenterX
        view.pivotY = view.lensCenterY
    }

    private fun hitsCutout(x: Int, labelTop: Int, view: LensView): Boolean {
        val label = Rect(x, labelTop, x + view.windowWidthPx, labelTop + view.labelHeightPx)
        return cutouts.any { Rect.intersects(it, label) }
    }

    private fun updateSample(view: LensView) {
        val snap = snapshot ?: return
        val bounds = screen
        val bx = (centerX * snap.bitmap.width / bounds.width()).toInt()
        val by = (centerY * snap.bitmap.height / bounds.height()).toInt()
        val rgb = snap.sampleSrgb(bx, by, view.sampleRadius)
        val label = if (snap.looksProtected) {
            ColorLabel(context.getString(R.string.protected_content), context.getString(R.string.protected_content_detail))
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

    /** Bumped per capture; a result for an older id arrived after the timeout and is dropped. */
    private var captureId = 0

    private fun takeSnapshot(then: () -> Unit) {
        if (capturing) return
        capturing = true
        setOverlayAlpha(0f)
        val id = ++captureId
        main.postDelayed(captureTimeout, CAPTURE_TIMEOUT_MS)

        // Wait two frames (plus a short delay) so the hidden overlay has left the screen.
        val choreographer = Choreographer.getInstance()
        choreographer.postFrameCallback {
            choreographer.postFrameCallback { main.postDelayed({ requestCapture(id, then, retriesLeft = 1) }, EXTRA_CAPTURE_DELAY_MS) }
        }
    }

    private fun requestCapture(id: Int, then: () -> Unit, retriesLeft: Int) {
        if (id != captureId || !capturing) return
        capturer.capture { result ->
            if (id != captureId || !capturing) {
                result.getOrNull()?.recycle()
                return@capture
            }
            val error = result.exceptionOrNull()
            if (error is CaptureException && error.reason == CaptureException.Reason.TOO_SOON && retriesLeft > 0) {
                // Android allows about three screenshots a second; a quick refresh just waits its turn.
                main.postDelayed({ requestCapture(id, then, retriesLeft - 1) }, RETRY_DELAY_MS)
                return@capture
            }
            endCapture()
            result.onSuccess { snap ->
                val old = snapshot
                snapshot = snap
                old?.recycle()
                namer.reset()
                Log.i(TAG, "capture ok: ${snap.describe()}")
                then()
            }.onFailure(::showCaptureError)
        }
    }

    /** A capture that never answers must not leave the overlay invisible. */
    private val captureTimeout = Runnable {
        if (!capturing) return@Runnable
        captureId++
        endCapture()
        showCaptureError(CaptureException(CaptureException.Reason.OTHER, "timed out"))
    }

    private fun endCapture() {
        capturing = false
        main.removeCallbacks(captureTimeout)
        setOverlayAlpha(1f)
    }

    private fun showCaptureError(e: Throwable) {
        Log.w(TAG, "capture failed", e)
        val message = if (e is CaptureException && e.reason == CaptureException.Reason.NO_ACCESS) {
            R.string.capture_no_access
        } else {
            R.string.capture_failed
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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

        /** Scroll events keep coming while content moves; refresh this long after the last one. */
        private const val SETTLE_AFTER_SCROLL_MS = 250L

        /** Without scroll events (fallback host, apps that don't send them), allow for a fling to finish. */
        private const val SETTLE_AFTER_TOUCH_MS = 700L

        private const val DROP_ZONE_DP = 24
        private const val DROP_ZONE_ALPHA = 0.45f
        private const val FLING_DP_PER_S = 1500
        private const val EXTRA_CAPTURE_DELAY_MS = 32L
        private const val RETRY_DELAY_MS = 350L
        private const val CAPTURE_TIMEOUT_MS = 3000L
    }
}
