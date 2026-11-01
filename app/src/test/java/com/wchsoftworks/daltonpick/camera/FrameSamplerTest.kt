package com.wchsoftworks.daltonpick.camera

import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameSamplerTest {
    /** An RGBA frame with padding at the end of each row, as camera buffers often have. */
    private fun frame(width: Int, height: Int, padding: Int, color: (x: Int, y: Int) -> Int): Pair<ByteBuffer, Int> {
        val stride = width * 4 + padding
        val buffer = ByteBuffer.allocate(stride * height)
        for (y in 0 until height) for (x in 0 until width) {
            val rgb = color(x, y)
            val i = y * stride + x * 4
            buffer.put(i, (rgb shr 16).toByte())
            buffer.put(i + 1, (rgb shr 8).toByte())
            buffer.put(i + 2, rgb.toByte())
            buffer.put(i + 3, 0xFF.toByte())
        }
        return buffer to stride
    }

    private val scratch = IntArray(25)

    @Test
    fun `flat color reads back exactly`() {
        val (buf, stride) = frame(8, 6, padding = 12) { _, _ -> 0x4CAF50 }
        assertEquals(0x4CAF50, FrameSampler.sampleRgba(buf, 8, 6, stride, 4, 3, 2, scratch))
    }

    @Test
    fun `row padding is skipped`() {
        // Left half red, right half blue; sampling well inside the blue half must not see red.
        val (buf, stride) = frame(10, 10, padding = 16) { x, _ -> if (x < 5) 0xFF0000 else 0x0000FF }
        assertEquals(0x0000FF, FrameSampler.sampleRgba(buf, 10, 10, stride, 8, 5, 1, scratch))
    }

    @Test
    fun `edges repeat instead of reading outside the frame`() {
        val (buf, stride) = frame(4, 4, padding = 0) { _, _ -> 0x123456 }
        assertEquals(0x123456, FrameSampler.sampleRgba(buf, 4, 4, stride, 0, 0, 2, scratch))
        assertEquals(0x123456, FrameSampler.sampleRgba(buf, 4, 4, stride, 3, 3, 2, scratch))
    }

    @Test
    fun `a 1x1 radius reads only the center`() {
        val (buf, stride) = frame(5, 5, padding = 4) { x, y -> if (x == 2 && y == 2) 0xFFFFFF else 0x000000 }
        assertEquals(0xFFFFFF, FrameSampler.sampleRgba(buf, 5, 5, stride, 2, 2, 0, scratch))
    }
}
