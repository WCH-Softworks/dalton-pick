package com.wchsoftworks.daltonpick.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.widget.Toast
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.SpikePrefs
import com.wchsoftworks.daltonpick.capture.ScreenCapturer
import com.wchsoftworks.daltonpick.capture.Snapshot
import com.wchsoftworks.daltonpick.colorengine.ColorLabel
import com.wchsoftworks.daltonpick.colorengine.ColorLocalization
import com.wchsoftworks.daltonpick.colorengine.HysteresisNamer
import com.wchsoftworks.daltonpick.colorengine.Lang

/**
 * Owns the overlay windows (edge tab + lens) and the snapshot-on-pull flow (spec §5.2).
 * The same controller runs on either capture backend; only [windowType] and [capturer] differ.
 */
class OverlayController(
    private val context: Context,
    private val windowType: Int,
    private val capturer: ScreenCapturer,
) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val prefs = SpikePrefs(context)
    private val density = context.resources.displayMetrics.density
    private val main = Handler(Looper.getMainLooper())
    private val namer = HysteresisNamer()

    private val tab = EdgeTabView(context, this)
    private val tabParams = overlayParams(
        (EdgeTabView.TOUCH_WIDTH_DP * density).toInt(),
        (EdgeTabView.HEIGHT_DP * density).toInt(),
    )
    private var lens: LensView? = null
    private var lensParams: WindowManager.LayoutParams? = null
    private var snapshot: Snapshot? = null

    private var showing = false
    private var capturing = false
    private var centerX = 0f
    private var centerY = 0f
    private var dragStartX = 0f
    private var dragStartY = 0f

    val tabY: Int get() = tabParams.y

    private val screen get() = wm.currentWindowMetrics.bounds
    private val lang get() = Lang.fromTag(context.resources.configuration.locales[0].toLanguageTag())

    /** Lens floats this far above the finger while pulling, so the finger doesn't cover it. */
    private val pullOffsetY get() = (LensView.LENS_DP / 2 + 32) * density

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

    fun show() {
        if (showing) return
        tab.onRightEdge = prefs.tabOnRight
        tabParams.x = if (tab.onRightEdge) screen.width() - tabParams.width else 0
        tabParams.y = prefs.tabY.takeIf { it >= 0 } ?: (screen.height() / 3)
        wm.addView(tab, tabParams)
        showing = true
    }

    fun hide() {
        dismissLens()
        if (showing) wm.removeView(tab)
        showing = false
    }

    // --- Edge tab

    fun moveTabTo(y: Int) {
        tabParams.y = y.coerceIn(0, screen.height() - tabParams.height)
        wm.updateViewLayout(tab, tabParams)
    }

    fun saveTabPosition() {
        prefs.tabY = tabParams.y
    }

    fun tapTab() {
        if (lens != null) dismissLens()
    }

    fun startPull(rawX: Float, rawY: Float) {
        centerX = rawX
        centerY = rawY - pullOffsetY
        takeSnapshot { showLens() }
    }

    fun dragPull(rawX: Float, rawY: Float) {
        centerX = rawX
        centerY = rawY - pullOffsetY
        if (lens != null) placeLens()
    }

    fun endPull() = announce()

    // --- Lens

    fun beginLensDrag() {
        dragStartX = centerX
        dragStartY = centerY
    }

    fun dragLensBy(dx: Float, dy: Float) {
        centerX = dragStartX + dx
        centerY = dragStartY + dy
        placeLens()
    }

    fun endLensDrag() = announce()

    fun isFlingTowardTab(velocityX: Float): Boolean {
        val threshold = 2500 * density
        return if (prefs.tabOnRight) velocityX > threshold else velocityX < -threshold
    }

    fun refresh() = takeSnapshot { placeLens() }

    fun dismissLens() {
        lens?.let { wm.removeView(it) }
        lens = null
        lensParams = null
        snapshot?.recycle()
        snapshot = null
        namer.reset()
    }

    private fun showLens() {
        if (lens != null) return placeLens()
        val view = LensView(context, this)
        val params = overlayParams(view.windowWidthPx, view.windowHeightPx)
        lens = view
        lensParams = params
        positionLens(view, params)
        wm.addView(view, params)
        updateSample(view)
    }

    private fun placeLens() {
        val view = lens ?: return
        val params = lensParams ?: return
        positionLens(view, params)
        wm.updateViewLayout(view, params)
        updateSample(view)
    }

    private fun positionLens(view: LensView, params: WindowManager.LayoutParams) {
        val bounds = screen
        centerX = centerX.coerceIn(0f, bounds.width() - 1f)
        centerY = centerY.coerceIn(0f, bounds.height() - 1f)
        val r = view.lensSizePx / 2f
        view.labelAbove = centerY + r + view.gapPx + view.labelHeightPx > bounds.height()
        params.x = (centerX - view.windowWidthPx / 2f).toInt()
            .coerceIn(0, maxOf(0, bounds.width() - view.windowWidthPx))
        view.lensCenterX = centerX - params.x
        params.y = if (view.labelAbove) {
            (centerY - r - view.gapPx - view.labelHeightPx).toInt()
        } else {
            (centerY - r).toInt()
        }
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
        view.bind(snap, bx, by, rgb, label, debugLine = "${snap.describe()} · ${"#%06X".format(rgb)}")
        if (changed) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun announce() {
        lens?.let { view -> view.label?.let { view.announceForAccessibility(it.name) } }
    }

    // --- Snapshot-on-pull

    private fun takeSnapshot(then: () -> Unit) {
        if (capturing) return
        capturing = true
        val hideFirst = prefs.hideBeforeCapture
        if (hideFirst) setOverlayAlpha(0f)

        val capture = {
            capturer.capture { result ->
                capturing = false
                setOverlayAlpha(1f)
                result.onSuccess { snap ->
                    val old = snapshot
                    snapshot = snap
                    old?.recycle()
                    namer.reset()
                    prefs.lastCapture = snap.describe() + if (hideFirst) " · hidden" else " · visible"
                    Log.i(TAG, "capture ok: ${prefs.lastCapture}")
                    then()
                }.onFailure { e ->
                    Log.w(TAG, "capture failed", e)
                    Toast.makeText(context, context.getString(R.string.capture_failed, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (hideFirst) {
            // Wait two frames (plus an adjustable delay) so the hidden overlay has left the screen.
            val choreographer = Choreographer.getInstance()
            choreographer.postFrameCallback {
                choreographer.postFrameCallback { main.postDelayed(capture, prefs.extraDelayMs.toLong()) }
            }
        } else {
            capture()
        }
    }

    private fun setOverlayAlpha(alpha: Float) {
        tab.alpha = alpha
        lens?.alpha = alpha
    }

    companion object {
        private const val TAG = "DaltonPick"
    }
}
