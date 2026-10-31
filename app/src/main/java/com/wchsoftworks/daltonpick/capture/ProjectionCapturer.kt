package com.wchsoftworks.daltonpick.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager

/**
 * Backend B: MediaProjection mirrored into an ImageReader.
 *
 * The virtual display only produces frames when the screen changes, so we keep the newest
 * frame around. Hiding the overlay before a capture changes the screen, which delivers a
 * fresh frame without the lens in it.
 */
class ProjectionCapturer(context: Context, private val projection: MediaProjection) : ScreenCapturer {
    override val name = "Projection"

    private val thread = HandlerThread("DaltonPick-capture").apply { start() }
    private val handler = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())

    private val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
    private val width = bounds.width()
    private val height = bounds.height()

    private val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
    private val display: VirtualDisplay?

    // Only touched on [handler]'s thread.
    private var latest: Image? = null
    private var pending: Pending? = null

    private class Pending(val start: Long, val onResult: (Result<Snapshot>) -> Unit)

    init {
        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            latest?.close()
            latest = image
            pending?.let {
                pending = null
                deliver(image, it.start, it.onResult)
            }
        }, handler)
        display = projection.createVirtualDisplay(
            "DaltonPick",
            width,
            height,
            context.resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler,
        )
    }

    override fun capture(onResult: (Result<Snapshot>) -> Unit) {
        val start = SystemClock.elapsedRealtime()
        handler.post {
            val image = latest
            if (image != null) {
                deliver(image, start, onResult)
            } else {
                val request = Pending(start, onResult)
                pending = request
                handler.postDelayed({
                    if (pending === request) {
                        pending = null
                        main.post { onResult(Result.failure(IllegalStateException("no frame from the screen yet"))) }
                    }
                }, FIRST_FRAME_TIMEOUT_MS)
            }
        }
    }

    private fun deliver(image: Image, start: Long, onResult: (Result<Snapshot>) -> Unit) {
        val result = runCatching { Snapshot(image.toBitmap(), SystemClock.elapsedRealtime() - start, name) }
        main.post { onResult(result) }
    }

    private fun Image.toBitmap(): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer.apply { rewind() }
        val paddedWidth = plane.rowStride / plane.pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(buffer)
        if (paddedWidth == width) return padded
        return Bitmap.createBitmap(padded, 0, 0, width, height).also { padded.recycle() }
    }

    override fun release() {
        handler.post {
            latest?.close()
            latest = null
            display?.release()
            reader.close()
            thread.quitSafely()
        }
        projection.stop()
    }

    companion object {
        private const val FIRST_FRAME_TIMEOUT_MS = 500L
    }
}
