package com.wchsoftworks.daltonpick

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.wchsoftworks.daltonpick.capture.A11yCapturer
import com.wchsoftworks.daltonpick.overlay.OverlayRuntime

/**
 * Main host. An accessibility service can add TYPE_ACCESSIBILITY_OVERLAY windows without the
 * "display over other apps" permission and can take screenshots without a consent dialog.
 * The tab appears as soon as the service is enabled, unless the user hid it.
 */
class DaltonAccessibilityService : AccessibilityService() {
    private var runtime: OverlayRuntime? = null

    override fun onServiceConnected() {
        instance = this
        runtime = OverlayRuntime(
            this,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            A11yCapturer(this),
            withStopAction = false,
        ).also { it.start() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        runtime?.onScreenChanged()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        runtime?.stop()
        runtime = null
        instance = null
        return super.onUnbind(intent)
    }

    companion object {
        /** The connected service, or null when the user has not enabled it. */
        var instance: DaltonAccessibilityService? = null
            private set
    }
}
