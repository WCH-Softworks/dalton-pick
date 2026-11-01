package com.wchsoftworks.daltonpick.capture

import android.graphics.Bitmap
import android.graphics.Color

/** Takes one full-screen snapshot on request (spec §5.2, snapshot-on-pull). */
interface ScreenCapturer {
    /** Short name shown in the lens debug line, e.g. "A11y". */
    val name: String

    /** Captures the screen and calls [onResult] on the main thread. */
    fun capture(onResult: (Result<Snapshot>) -> Unit)

    /** The screen was rotated or resized: drop anything sized for the old screen. */
    fun onScreenChanged() {}

    fun release() {}
}

/** Why a capture failed, so the overlay can retry, explain, or give up. */
class CaptureException(val reason: Reason, message: String) : Exception(message) {
    enum class Reason {
        /** The system rate-limits screenshots; trying again shortly works. */
        TOO_SOON,

        /** Screen access is gone: the accessibility service lost its rights, or the projection stopped. */
        NO_ACCESS,

        /** Anything else: no frame yet, a bad buffer, an internal error. */
        OTHER,
    }
}

/** A frozen software copy of the screen. Pixels read back in sRGB. */
class Snapshot(val bitmap: Bitmap, val latencyMs: Long, val source: String) {
    val colorSpaceName: String = bitmap.colorSpace?.name ?: "unknown"

    /** True when the whole screen reads as pure black, which is what FLAG_SECURE windows return. */
    val looksProtected: Boolean by lazy {
        val steps = 8
        (0 until steps).all { gy ->
            (0 until steps).all { gx ->
                val x = (bitmap.width - 1) * gx / (steps - 1)
                val y = (bitmap.height - 1) * gy / (steps - 1)
                bitmap.getPixel(x, y) == Color.BLACK
            }
        }
    }

    fun describe(): String = "$source · $latencyMs ms · ${bitmap.width}×${bitmap.height} · $colorSpaceName"

    fun recycle() = bitmap.recycle()
}
