package com.wchsoftworks.daltonpick.colorengine

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OklabTest {
    private fun assertLab(expected: Oklab, rgb: Int, tolerance: Double = 1e-4) {
        val actual = Oklab.fromRgb(rgb)
        assertTrue(
            abs(actual.l - expected.l) < tolerance &&
                abs(actual.a - expected.a) < tolerance &&
                abs(actual.b - expected.b) < tolerance,
            "${Srgb.toHex(rgb)}: expected $expected, got $actual",
        )
    }

    @Test
    fun `matches published reference values`() {
        // Reference values from Björn Ottosson's OKLab implementation.
        assertLab(Oklab(1.0, 0.0, 0.0), 0xFFFFFF)
        assertLab(Oklab(0.0, 0.0, 0.0), 0x000000)
        assertLab(Oklab(0.627955, 0.224863, 0.125846), 0xFF0000)
        assertLab(Oklab(0.866440, -0.233888, 0.179498), 0x00FF00)
        assertLab(Oklab(0.452014, -0.032457, -0.311528), 0x0000FF)
    }

    @Test
    fun `grays have no chroma`() {
        for (v in 0..255 step 17) {
            val rgb = (v shl 16) or (v shl 8) or v
            assertTrue(Oklab.fromRgb(rgb).chroma < 1e-4, "gray ${Srgb.toHex(rgb)} has chroma")
        }
    }

    @Test
    fun `linear round trip is exact for every channel value`() {
        for (c in 0..255) assertEquals(c, Srgb.fromLinear(Srgb.toLinear(c)))
    }

    @Test
    fun `average blends in linear light`() {
        // Half black, half white looks like ~#BCBCBC, not #808080.
        val avg = Srgb.average(intArrayOf(0x000000, 0xFFFFFF))
        assertEquals(0xBCBCBC, avg)
        assertEquals(0x123456, Srgb.average(IntArray(9) { 0x123456 }))
    }

    @Test
    fun `alpha is ignored`() {
        assertEquals(Oklab.fromRgb(0x336699), Oklab.fromRgb(0x80336699.toInt()))
        assertEquals("#336699", Srgb.toHex(0xFF336699.toInt()))
    }
}
