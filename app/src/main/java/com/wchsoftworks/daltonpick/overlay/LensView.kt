package com.wchsoftworks.daltonpick.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import com.wchsoftworks.daltonpick.capture.Snapshot
import com.wchsoftworks.daltonpick.colorengine.ColorLabel

/**
 * The round lens with a gapped crosshair, plus the name label (spec §4.2).
 * Draws a magnified region of the frozen [Snapshot]; the controller decides what to show.
 */
@SuppressLint("ViewConstructor")
class LensView(context: Context, private val controller: OverlayController) : View(context) {
    private val density = resources.displayMetrics.density
    private val scaledDensity = density * resources.configuration.fontScale

    val lensSizePx = (LENS_DP * density).toInt()
    val labelHeightPx = (LABEL_DP * density * resources.configuration.fontScale.coerceAtLeast(1f)).toInt()
    val gapPx = (8 * density).toInt()
    val windowWidthPx = maxOf(lensSizePx, (WINDOW_MIN_WIDTH_DP * density).toInt())
    val windowHeightPx = lensSizePx + gapPx + labelHeightPx

    var zoom = 3f
    var sampleRadius = 1

    /** True when the label sits above the lens (lens near the bottom of the screen). */
    var labelAbove = false

    /**
     * Lens centre within this window. The window is kept on screen so the label never gets cut
     * off, which moves the lens off-centre when it is near a side edge.
     */
    var lensCenterX = windowWidthPx / 2f

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
    private val crossDark = stroke(3f, 0xCC000000.toInt())
    private val crossLight = stroke(1.25f, 0xFFFFFFFF.toInt())
    private val labelBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF0202020.toInt() }
    private val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val swatchRing = stroke(1.5f, 0xFFFFFFFF.toInt())
    private val nameText = text(18f, 0xFFFFFFFF.toInt(), Typeface.DEFAULT_BOLD)
    private val detailText = text(13f, 0xFFE0E0E0.toInt(), Typeface.DEFAULT)
    private val debugText = text(10f, 0xFFB0B0B0.toInt(), Typeface.MONOSPACE)

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
        contentDescription = label.name
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
        val cy = lensTop + r

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

        val textX = swatchCx + swatchR + pad
        val maxWidth = width - textX - pad
        var y = top + pad - nameText.ascent()
        canvas.drawText(ellipsize(label.name, nameText, maxWidth), textX, y, nameText)
        if (label.detail.isNotEmpty()) {
            y += nameText.descent() - detailText.ascent() + 2 * density
            canvas.drawText(ellipsize(label.detail, detailText, maxWidth), textX, y, detailText)
        }
        debugLine?.let {
            y += detailText.descent() - debugText.ascent() + 2 * density
            canvas.drawText(ellipsize(it, debugText, maxWidth), textX, y, debugText)
        }
    }

    private fun ellipsize(s: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(s) <= maxWidth) return s
        var end = s.length
        while (end > 0 && paint.measureText(s, 0, end) + paint.measureText("…") > maxWidth) end--
        return s.substring(0, end) + "…"
    }

    // --- Touch: drag anywhere to move, tap the label to refresh, fling sideways to dismiss.

    private var downRawX = 0f
    private var downRawY = 0f
    private var moved = false
    private var velocity: VelocityTracker? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                moved = false
                velocity = VelocityTracker.obtain().also { it.addMovement(event) }
                controller.beginLensDrag()
            }

            MotionEvent.ACTION_MOVE -> {
                velocity?.addMovement(event)
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (!moved && dx * dx + dy * dy > (6 * density) * (6 * density)) moved = true
                if (moved) controller.dragLensBy(dx, dy)
            }

            MotionEvent.ACTION_UP -> {
                velocity?.addMovement(event)
                velocity?.computeCurrentVelocity(1000)
                val vx = velocity?.xVelocity ?: 0f
                velocity?.recycle()
                velocity = null
                val tappedLabel = event.y >= labelTop && event.y <= labelTop + labelHeightPx
                when {
                    moved && controller.isFlingTowardTab(vx) -> controller.dismissLens()
                    moved -> controller.endLensDrag()
                    tappedLabel -> controller.refresh()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                velocity?.recycle()
                velocity = null
            }
        }
        return true
    }

    companion object {
        const val LENS_DP = 140
        const val LABEL_DP = 76
        const val WINDOW_MIN_WIDTH_DP = 240
    }
}
