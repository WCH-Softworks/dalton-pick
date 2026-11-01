package com.wchsoftworks.daltonpick.camera

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.wchsoftworks.daltonpick.colorengine.ColorLabel
import com.wchsoftworks.daltonpick.colorengine.ColorLocalization
import com.wchsoftworks.daltonpick.colorengine.HysteresisNamer
import com.wchsoftworks.daltonpick.colorengine.Lang
import com.wchsoftworks.daltonpick.colorengine.Srgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What the crosshair is on: the averaged color and its name. */
data class Reading(val rgb: Int, val label: ColorLabel)

/**
 * Names the color at the center of each camera frame (spec §4.4). Frames are read in memory and
 * dropped; nothing is stored. While [frozen], live frames are ignored and [sampleFrozen] reads from
 * the held picture instead.
 */
class CameraLens(@Volatile var lang: Lang) : ImageAnalysis.Analyzer {
    private val namer = HysteresisNamer()
    private val scratch = IntArray((2 * FrameSampler.RADIUS + 1) * (2 * FrameSampler.RADIUS + 1))
    private val _reading = MutableStateFlow<Reading?>(null)
    val reading: StateFlow<Reading?> = _reading

    @Volatile
    var frozen = false

    private var lastUpdate = 0L

    /** Runs on the analysis thread. The crop rect is the part the preview shows, so its center is the crosshair. */
    override fun analyze(image: ImageProxy) {
        image.use {
            if (frozen) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastUpdate < UPDATE_INTERVAL_MS) return
            lastUpdate = now
            val plane = it.planes[0]
            val crop = it.cropRect
            val rgb = FrameSampler.sampleRgba(
                plane.buffer, it.width, it.height, plane.rowStride,
                crop.centerX(), crop.centerY(), FrameSampler.RADIUS, scratch,
            )
            publish(rgb)
        }
    }

    /** Reads the held picture at ([x], [y]) in bitmap pixels. Main thread. */
    fun sampleFrozen(bitmap: Bitmap, x: Int, y: Int) {
        val r = FrameSampler.RADIUS
        val pixels = IntArray((2 * r + 1) * (2 * r + 1))
        var n = 0
        for (dy in -r..r) for (dx in -r..r) {
            pixels[n++] = bitmap.getPixel((x + dx).coerceIn(0, bitmap.width - 1), (y + dy).coerceIn(0, bitmap.height - 1))
        }
        publish(Srgb.average(pixels, n))
    }

    /** Language changed, or the user started over: name the next color from scratch. */
    fun reset() = synchronized(this) { namer.reset() }

    private fun publish(rgb: Int) = synchronized(this) {
        _reading.value = Reading(rgb, ColorLocalization.label(namer.match(rgb), lang))
    }

    companion object {
        /** About 8 name updates a second: smooth enough to aim, calm enough to read. */
        private const val UPDATE_INTERVAL_MS = 120L
    }
}
