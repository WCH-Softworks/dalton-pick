package com.wchsoftworks.daltonpick.overlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.Log
import com.wchsoftworks.daltonpick.capture.Snapshot
import com.wchsoftworks.daltonpick.colorengine.Srgb

/**
 * What the lens shows: the latest [Snapshot], plus the content of the scrolling band stitched
 * from earlier snapshots, so the lens can follow a scroll before the next snapshot arrives.
 * The row bookkeeping lives in [map]; this class holds the bitmaps. Main thread only.
 */
class ScreenContent(snapshot: Snapshot) {
    var snapshot: Snapshot = snapshot
        private set
    val map = ScrollMap(snapshot.bitmap.height)
    private var buffer: Bitmap? = null

    private val copyPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }
    private val src = Rect()
    private val dst = Rect()

    val width get() = snapshot.bitmap.width
    val height get() = snapshot.bitmap.height

    /** A new snapshot of the same screen. The band and buffer stay until registration says otherwise. */
    fun replace(next: Snapshot) {
        snapshot = next
        map.shift = 0
        val buf = buffer
        if (next.bitmap.height != map.height || (buf != null && buf.width != next.bitmap.width)) clear()
    }

    /** Forgets the band and everything stitched. */
    fun clear() {
        map.reset()
        buffer?.recycle()
        buffer = null
    }

    fun recycle() = clear()

    /**
     * Copies the band of [from] into the buffer at scroll position [pos]. Starts a buffer of
     * [BUFFER_BANDS] band heights first if there is none or the band outgrew it.
     */
    fun merge(from: Snapshot, pos: Int) {
        if (!map.hasBand || from.bitmap.isRecycled) return
        val bandHeight = map.bandBottom - map.bandTop
        var buf = buffer
        if (buf == null || map.capacity < bandHeight) {
            buf?.recycle()
            buf = try {
                Bitmap.createBitmap(
                    from.bitmap.width, bandHeight * BUFFER_BANDS, Bitmap.Config.ARGB_8888, false,
                    from.bitmap.colorSpace ?: ColorSpace.get(ColorSpace.Named.SRGB),
                )
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "no memory for the scroll buffer", e)
                null
            }
            buffer = buf
            if (buf == null) {
                map.startBuffer(0)
                return
            }
            map.startBuffer(buf.height)
        }
        val canvas = Canvas(buf)
        var y = map.bandTop
        while (y < map.bandBottom) {
            // Copy up to the buffer's wrap point, then continue from its top.
            val ring = map.bufferRow(y + pos)
            val rows = minOf(map.bandBottom - y, map.capacity - ring)
            src.set(0, y, buf.width, y + rows)
            dst.set(0, ring, buf.width, ring + rows)
            canvas.drawBitmap(from.bitmap, src, dst, copyPaint)
            y += rows
        }
        map.stored(map.bandTop + pos, map.bandBottom + pos)
    }

    private fun bitmapFor(kind: Int): Bitmap? = when (kind) {
        ScrollMap.SNAPSHOT -> snapshot.bitmap
        ScrollMap.BUFFER -> buffer
        else -> null
    }?.takeIf { !it.isRecycled }

    /** Calls [block] for each run of screen rows [top, bottom) with the bitmap it comes from (null: not seen yet) and the row in it. */
    fun forEachRun(top: Int, bottom: Int, block: (bitmap: Bitmap?, srcTop: Int, top: Int, count: Int) -> Unit) {
        map.runs(top, bottom) { kind, srcTop, runTop, count -> block(bitmapFor(kind), srcTop, runTop, count) }
    }

    private val sampleBuffer = IntArray((2 * MAX_SAMPLE_RADIUS + 1) * (2 * MAX_SAMPLE_RADIUS + 1))

    /**
     * Averages the (2r+1)² pixels around ([x], [y]) in linear light, skipping any not seen yet.
     * Null when the centre itself hasn't been seen. [Bitmap.getPixel] converts to sRGB.
     */
    fun sampleSrgb(x: Int, y: Int, radius: Int): Int? {
        val r = radius.coerceIn(0, MAX_SAMPLE_RADIUS)
        if (pixel(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1)) == null) return null
        var n = 0
        for (dy in -r..r) for (dx in -r..r) {
            val p = pixel((x + dx).coerceIn(0, width - 1), (y + dy).coerceIn(0, height - 1)) ?: continue
            sampleBuffer[n++] = p
        }
        return Srgb.average(sampleBuffer, n)
    }

    private fun pixel(x: Int, y: Int): Int? {
        val kind = map.locate(y)
        val bmp = bitmapFor(kind) ?: return null
        return bmp.getPixel(x, map.row)
    }

    /** For the debug line: band rows, stitched rows, live shift. */
    fun describe(): String {
        if (!map.hasBand) return "no band"
        val guess = if (map.provisional) "?" else ""
        return "band ${map.bandTop}–${map.bandBottom}$guess · kept ${map.stored} · ${if (map.shift >= 0) "+" else ""}${map.shift}"
    }

    companion object {
        private const val TAG = "DaltonPick"
        const val MAX_SAMPLE_RADIUS = 2

        /** Buffer height in band heights: content kept for scrolling back without waiting. */
        const val BUFFER_BANDS = 3
    }
}

/**
 * One hash per pixel row, for [ScrollRegistration]. Leaves out a strip at each side, where
 * scrollbars appear and fade during a scroll. Safe on a worker thread while nothing recycles [bitmap].
 */
fun rowHashes(bitmap: Bitmap): IntArray {
    val w = bitmap.width
    val margin = (w * SCROLLBAR_MARGIN).toInt()
    val row = IntArray(w)
    return IntArray(bitmap.height) { y ->
        bitmap.getPixels(row, 0, w, 0, y, w, 1)
        ScrollRegistration.rowHash(row, margin, w - margin)
    }
}

private const val SCROLLBAR_MARGIN = 0.04f
