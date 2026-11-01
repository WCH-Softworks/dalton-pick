package com.wchsoftworks.daltonpick.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.capture.Snapshot
import com.wchsoftworks.daltonpick.colorengine.ColorLabel
import com.wchsoftworks.daltonpick.colorengine.Srgb
import com.wchsoftworks.daltonpick.settings.Settings

/**
 * The round lens with a gapped crosshair, plus the name label (spec §4.2).
 * Draws a magnified region of the frozen [Snapshot]; the controller decides what to show.
 */
@SuppressLint("ViewConstructor")
class LensView(
    context: Context,
    private val controller: OverlayController,
    settings: Settings,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val fontScale = resources.configuration.fontScale.coerceIn(1f, 2f)
    private val scaledDensity = density * fontScale

    private val showHex = settings.showHex
    private val showDebug = settings.showDebug

    val lensSizePx = (settings.lensSizeDp * density).toInt()
    val labelHeightPx = (labelHeightDp(showHex, showDebug) * density * fontScale).toInt()
    val gapPx = (8 * density).toInt()
    val windowWidthPx = maxOf(lensSizePx, (WINDOW_MIN_WIDTH_DP * density).toInt())
    val windowHeightPx = lensSizePx + gapPx + labelHeightPx

    var zoom = settings.zoom.toFloat()
    var sampleRadius = settings.sampleRadius

    /** True when the label sits above the lens (lens near the bottom of the screen). */
    var labelAbove = false

    /**
     * Lens centre within this window. The window is kept inside the safe area so the label never
     * gets cut off, which moves the lens off-centre when it is near a side edge.
     */
    var lensCenterX = windowWidthPx / 2f

    val lensCenterY: Float get() = lensTop + lensSizePx / 2f

    /** True while fine-adjust mode is on: the lens moves at a fraction of finger speed. */
    private var precision = false

    private var snapshot: Snapshot? = null
    private var sampleX = 0
    private var sampleY = 0
    private var sampledRgb = 0
    var label: ColorLabel? = null
        private set
    private var debugLine: String? = null

    private val bitmapPaint = Paint().apply { isFilterBitmap = false } // nearest neighbour: crisp pixels
    private val emptyPaint = Paint().apply { color = 0xFF404040.toInt() }
    private val ringDark = stroke(4f, 0xE6000000.toInt())
    private val ringLight = stroke(2f, 0xFFFFFFFF.toInt())
    private val precisionDark = stroke(3f, 0xE6000000.toInt())
    private val precisionLight = stroke(1f, 0xFFFFFFFF.toInt())
    private val crossDark = stroke(3f, 0xCC000000.toInt())
    private val crossLight = stroke(1.25f, 0xFFFFFFFF.toInt())
    private val labelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF0202020.toInt() }
    private val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val swatchRing = stroke(1.5f, 0xFFFFFFFF.toInt())
    private val nameText = text(18f, 0xFFFFFFFF.toInt(), Typeface.DEFAULT_BOLD)
    private val detailText = text(13f, 0xFFE0E0E0.toInt(), Typeface.DEFAULT)
    private val smallText = text(11f, 0xFFB8B8B8.toInt(), Typeface.MONOSPACE)
    private val closePaint = stroke(2f, 0xFFE0E0E0.toInt()).apply { strokeCap = Paint.Cap.ROUND }
    private val closeSizePx = (CLOSE_SIZE_DP * density).toInt()
    private val closeRect = RectF()

    private val clip = Path()
    private val src = Rect()
    private val dst = RectF()
    private val labelRect = RectF()

    private fun stroke(widthDp: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = widthDp * density
        this.color = color
    }

    private fun text(sizeSp: Float, color: Int, face: Typeface) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizeSp * scaledDensity
        this.color = color
        typeface = face
    }

    fun bind(snapshot: Snapshot, sampleX: Int, sampleY: Int, rgb: Int, label: ColorLabel, debugLine: String?) {
        this.snapshot = snapshot
        this.sampleX = sampleX
        this.sampleY = sampleY
        this.sampledRgb = rgb
        this.label = label
        this.debugLine = debugLine
        contentDescription = if (label.detail.isEmpty()) label.name else "${label.name}, ${label.detail}"
        invalidate()
    }

    private val lensTop get() = if (labelAbove) labelHeightPx + gapPx else 0
    private val labelTop get() = if (labelAbove) 0 else lensSizePx + gapPx

    override fun onDraw(canvas: Canvas) {
        drawLens(canvas)
        drawLabel(canvas)
    }

    private fun drawLens(canvas: Canvas) {
        val r = lensSizePx / 2f
        val cx = lensCenterX
        val cy = lensCenterY

        canvas.save()
        clip.reset()
        clip.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.clipPath(clip)
        canvas.drawCircle(cx, cy, r, emptyPaint)

        val bmp = snapshot?.bitmap
        if (bmp != null && !bmp.isRecycled) {
            // Source square centred on the sample pixel, clipped to the bitmap, mapped to the lens.
            val half = r / zoom
            val left = sampleX + 0.5f - half
            val top = sampleY + 0.5f - half
            src.set(
                maxOf(0, left.toInt()), maxOf(0, top.toInt()),
                minOf(bmp.width, (sampleX + 0.5f + half).toInt() + 1), minOf(bmp.height, (sampleY + 0.5f + half).toInt() + 1),
            )
            if (!src.isEmpty) {
                dst.set(
                    cx - r + (src.left - left) * zoom, cy - r + (src.top - top) * zoom,
                    cx - r + (src.right - left) * zoom, cy - r + (src.bottom - top) * zoom,
                )
                canvas.drawBitmap(bmp, src, dst, bitmapPaint)
            }
        }
        canvas.restore()

        drawCrosshair(canvas, cx, cy)
        canvas.drawCircle(cx, cy, r - ringDark.strokeWidth / 2, ringDark)
        canvas.drawCircle(cx, cy, r - ringDark.strokeWidth / 2, ringLight)
        if (precision) {
            // A second, inner ring shows fine-adjust mode without relying on color.
            val inner = r - ringDark.strokeWidth - 4 * density
            canvas.drawCircle(cx, cy, inner, precisionDark)
            canvas.drawCircle(cx, cy, inner, precisionLight)
        }
    }

    private fun drawCrosshair(canvas: Canvas, cx: Float, cy: Float) {
        // A box around the sampled pixels, then four short arms with a gap so nothing covers the sample.
        val box = (sampleRadius + 0.5f) * zoom
        val gap = box + 3 * density
        val arm = 9 * density
        for (paint in arrayOf(crossDark, crossLight)) {
            canvas.drawRect(cx - box, cy - box, cx + box, cy + box, paint)
            canvas.drawLine(cx - gap - arm, cy, cx - gap, cy, paint)
            canvas.drawLine(cx + gap, cy, cx + gap + arm, cy, paint)
            canvas.drawLine(cx, cy - gap - arm, cx, cy - gap, paint)
            canvas.drawLine(cx, cy + gap, cx, cy + gap + arm, paint)
        }
    }

    private fun drawLabel(canvas: Canvas) {
        val label = label ?: return
        val pad = 12 * density
        val top = labelTop.toFloat()
        labelRect.set(0f, top, width.toFloat(), top + labelHeightPx)
        val corner = 16 * density
        canvas.drawRoundRect(labelRect, corner, corner, labelBg)

        val swatchR = 14 * density
        val swatchCx = pad + swatchR
        val swatchCy = top + labelHeightPx / 2f
        swatchPaint.color = 0xFF000000.toInt() or sampledRgb
        canvas.drawCircle(swatchCx, swatchCy, swatchR, swatchPaint)
        canvas.drawCircle(swatchCx, swatchCy, swatchR, swatchRing)

        // ✕ on the right end of the label: the obvious way to put the lens away.
        val closeLeft = width - closeSizePx
        closeRect.set(closeLeft.toFloat(), top, width.toFloat(), top + labelHeightPx)
        val cx = closeRect.centerX()
        val cy = top + minOf(labelHeightPx / 2f, pad + closeSizePx / 2f)
        val arm = 7 * density
        canvas.drawLine(cx - arm, cy - arm, cx + arm, cy + arm, closePaint)
        canvas.drawLine(cx - arm, cy + arm, cx + arm, cy - arm, closePaint)

        val textX = swatchCx + swatchR + pad
        val maxWidth = closeLeft - textX
        var y = top + pad - nameText.ascent()
        canvas.drawText(ellipsize(label.name, nameText, maxWidth), textX, y, nameText)
        if (label.detail.isNotEmpty()) {
            y += nameText.descent() - detailText.ascent() + 2 * density
            canvas.drawText(ellipsize(label.detail, detailText, maxWidth), textX, y, detailText)
            if (showHex) {
                y += detailText.descent() - smallText.ascent() + 2 * density
                val hex = "${Srgb.toHex(sampledRgb)} · ${sampledRgb shr 16 and 0xFF}, ${sampledRgb shr 8 and 0xFF}, ${sampledRgb and 0xFF}"
                canvas.drawText(ellipsize(hex, smallText, maxWidth), textX, y, smallText)
            }
        }
        if (showDebug) {
            debugLine?.let {
                y += smallText.descent() - smallText.ascent() + 2 * density
                canvas.drawText(ellipsize(it, smallText, maxWidth), textX, y, smallText)
            }
        }
    }

    private fun ellipsize(s: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(s) <= maxWidth) return s
        var end = s.length
        while (end > 0 && paint.measureText(s, 0, end) + paint.measureText("…") > maxWidth) end--
        return s.substring(0, end) + "…"
    }

    // --- Touch. Drag from the lens to move it, hold to fine-adjust, drag from outside the circle
    // (the label or window corners) to fine-adjust, tap the label to refresh, fling to dismiss.

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var baseRawX = 0f
    private var baseRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var moved = false
    private var startedOutside = false
    private var velocity: VelocityTracker? = null

    private val enterPrecisionOnHold = Runnable {
        if (!moved && !precision) enterPrecision()
    }

    private fun enterPrecision() {
        precision = true
        moved = false
        baseRawX = lastRawX
        baseRawY = lastRawY
        controller.beginLensDrag()
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        invalidate()
    }

    private fun insideCircle(x: Float, y: Float): Boolean {
        val dx = x - lensCenterX
        val dy = y - lensCenterY
        val r = lensSizePx / 2f
        return dx * dx + dy * dy <= r * r
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        lastRawX = event.rawX
        lastRawY = event.rawY
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                baseRawX = event.rawX
                baseRawY = event.rawY
                moved = false
                precision = false
                startedOutside = !insideCircle(event.x, event.y)
                velocity = VelocityTracker.obtain().also { it.addMovement(event) }
                controller.beginLensDrag()
                if (!startedOutside) postDelayed(enterPrecisionOnHold, ViewConfiguration.getLongPressTimeout().toLong())
            }

            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(event)
                val dx = event.rawX - baseRawX
                val dy = event.rawY - baseRawY
                if (!moved && dx * dx + dy * dy > touchSlop * touchSlop) {
                    moved = true
                    removeCallbacks(enterPrecisionOnHold)
                    if (startedOutside && !precision) {
                        // Restart from here so the lens doesn't jump by the slop distance.
                        enterPrecision()
                        moved = true
                        return true
                    }
                }
                if (moved) {
                    val f = if (precision) PRECISION_FACTOR else 1f
                    controller.dragLensBy(dx * f, dy * f, precise = precision)
                }
            }

            MotionEvent.ACTION_UP -> {
                removeCallbacks(enterPrecisionOnHold)
                velocity?.addMovement(event)
                velocity?.computeCurrentVelocity(1000)
                val vx = velocity?.xVelocity ?: 0f
                velocity?.recycle()
                velocity = null
                val tappedLabel = event.y >= labelTop && event.y <= labelTop + labelHeightPx
                val tappedClose = tappedLabel && event.x >= width - closeSizePx
                val dragged = moved && !precision
                controller.lensTouchEnded()
                when {
                    dragged && (controller.isFlingTowardTab(vx) || controller.inDropZone()) -> controller.dismissLens()
                    moved || precision -> controller.endLensDrag()
                    tappedClose -> controller.dismissLens()
                    tappedLabel -> controller.refresh()
                }
                endPrecision()
            }

            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(enterPrecisionOnHold)
                velocity?.recycle()
                velocity = null
                controller.lensTouchEnded()
                endPrecision()
            }

            // A touch somewhere else on screen (the app underneath may be scrolling).
            MotionEvent.ACTION_OUTSIDE -> controller.onOutsideTouch()
        }
        return true
    }

    private fun endPrecision() {
        if (precision) {
            precision = false
            invalidate()
        }
    }

    // TalkBack: the lens is read as its color name; these put it away or take a new snapshot.
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.action_close_lens, context.getString(R.string.lens_action_close)))
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(R.id.action_refresh_lens, context.getString(R.string.lens_action_refresh)))
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean = when (action) {
        R.id.action_close_lens -> true.also { controller.dismissLens() }
        R.id.action_refresh_lens -> true.also { controller.refresh() }
        else -> super.performAccessibilityAction(action, arguments)
    }

    companion object {
        const val WINDOW_MIN_WIDTH_DP = 240
        const val PRECISION_FACTOR = 0.25f

        /** Touch area for the ✕ at the right end of the label. */
        const val CLOSE_SIZE_DP = 44

        /** Name and family lines, plus one line each for the optional hex and debug text. */
        fun labelHeightDp(showHex: Boolean, showDebug: Boolean): Int =
            64 + (if (showHex) 16 else 0) + (if (showDebug) 14 else 0)
    }
}
