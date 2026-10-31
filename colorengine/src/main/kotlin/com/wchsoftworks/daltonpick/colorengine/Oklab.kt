package com.wchsoftworks.daltonpick.colorengine

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/** A color in the OKLab perceptual space (Björn Ottosson, 2020). */
data class Oklab(val l: Double, val a: Double, val b: Double) {
    val chroma: Double get() = sqrt(a * a + b * b)

    /** Hue angle in degrees, 0..360. Meaningless when [chroma] is near zero. */
    val hue: Double get() = (Math.toDegrees(atan2(b, a)) + 360.0) % 360.0

    fun distanceTo(other: Oklab): Double {
        val dl = l - other.l
        val da = a - other.a
        val db = b - other.b
        return sqrt(dl * dl + da * da + db * db)
    }

    companion object {
        /** Converts a packed 0xRRGGBB (alpha ignored) sRGB color to OKLab. */
        fun fromRgb(rgb: Int): Oklab = Srgb.toOklab(rgb)
    }
}

/** sRGB helpers. All packed colors are 0x(AA)RRGGBB; alpha is ignored. */
object Srgb {
    private val toLinearLut = DoubleArray(256) { i ->
        val c = i / 255.0
        if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    fun toLinear(channel: Int): Double = toLinearLut[channel and 0xFF]

    fun fromLinear(linear: Double): Int {
        val c = linear.coerceIn(0.0, 1.0)
        val s = if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055
        return (s * 255.0 + 0.5).toInt().coerceIn(0, 255)
    }

    fun toOklab(rgb: Int): Oklab {
        val r = toLinear(rgb shr 16)
        val g = toLinear(rgb shr 8)
        val b = toLinear(rgb)

        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)

        return Oklab(
            l = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            b = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
        )
    }

    /**
     * Averages packed sRGB pixels in linear light, which is what the eye sees when
     * neighbouring pixels blur together (dithering, anti-aliasing).
     */
    fun average(pixels: IntArray, count: Int = pixels.size): Int {
        require(count in 1..pixels.size)
        var r = 0.0
        var g = 0.0
        var b = 0.0
        for (i in 0 until count) {
            val p = pixels[i]
            r += toLinear(p shr 16)
            g += toLinear(p shr 8)
            b += toLinear(p)
        }
        return (fromLinear(r / count) shl 16) or (fromLinear(g / count) shl 8) or fromLinear(b / count)
    }

    fun toHex(rgb: Int): String = "#%06X".format(rgb and 0xFFFFFF)
}
