package com.wchsoftworks.daltonpick.camera

import com.wchsoftworks.daltonpick.colorengine.Srgb
import java.nio.ByteBuffer

/** Reads colors out of CameraX's RGBA_8888 analysis frames (spec §4.4). */
object FrameSampler {
    /** Camera mode always averages 5×5: camera noise is much stronger than screen dithering. */
    const val RADIUS = 2

    /**
     * Averages the (2r+1)² pixels around ([cx], [cy]) in linear light and returns 0xRRGGBB.
     * Pixels past the edge repeat the edge. [scratch] must hold at least (2r+1)² ints.
     */
    fun sampleRgba(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        cx: Int,
        cy: Int,
        radius: Int,
        scratch: IntArray,
    ): Int {
        var n = 0
        for (dy in -radius..radius) {
            val y = (cy + dy).coerceIn(0, height - 1)
            for (dx in -radius..radius) {
                val x = (cx + dx).coerceIn(0, width - 1)
                val i = y * rowStride + x * 4
                val r = buffer.get(i).toInt() and 0xFF
                val g = buffer.get(i + 1).toInt() and 0xFF
                val b = buffer.get(i + 2).toInt() and 0xFF
                scratch[n++] = (r shl 16) or (g shl 8) or b
            }
        }
        return Srgb.average(scratch, n)
    }
}
