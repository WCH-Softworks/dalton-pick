package com.wchsoftworks.daltonpick.capture

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.Display

/** Backend A: AccessibilityService.takeScreenshot (API 30+). No per-session consent dialog. */
class A11yCapturer(private val service: AccessibilityService) : ScreenCapturer {
    override val name = "A11y"

    override fun capture(onResult: (Result<Snapshot>) -> Unit) {
        val start = SystemClock.elapsedRealtime()
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            service.mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val buffer = result.hardwareBuffer
                    val software = try {
                        val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                        hardware?.copy(Bitmap.Config.ARGB_8888, false).also { hardware?.recycle() }
                    } finally {
                        buffer.close()
                    }
                    if (software == null) {
                        onResult(Result.failure(IllegalStateException("could not read screenshot")))
                    } else {
                        onResult(Result.success(Snapshot(software, SystemClock.elapsedRealtime() - start, name)))
                    }
                }

                override fun onFailure(errorCode: Int) {
                    onResult(Result.failure(IllegalStateException(describeError(errorCode))))
                }
            },
        )
    }

    private fun describeError(code: Int) = when (code) {
        AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR -> "internal error"
        AccessibilityService.ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS -> "no screenshot access"
        AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT -> "too soon after the last capture"
        AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY -> "invalid display"
        else -> "error $code"
    }
}
