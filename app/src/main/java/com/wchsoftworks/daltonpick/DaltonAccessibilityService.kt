package com.wchsoftworks.daltonpick

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.wchsoftworks.daltonpick.capture.A11yCapturer
import com.wchsoftworks.daltonpick.overlay.OverlayController

/**
 * Backend A host. An accessibility service can add TYPE_ACCESSIBILITY_OVERLAY windows without
 * the "display over other apps" permission and can take screenshots without a consent dialog.
 */
class DaltonAccessibilityService : AccessibilityService() {
    private var controller: OverlayController? = null

    override fun onServiceConnected() {
        instance = this
    }

    fun showOverlay() {
        val c = controller ?: OverlayController(
            this,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            A11yCapturer(this),
        ).also { controller = it }
        c.show()
    }

    fun hideOverlay() {
        controller?.hide()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        hideOverlay()
        controller = null
        instance = null
        return super.onUnbind(intent)
    }

    companion object {
        /** The connected service, or null when the user hasn't enabled it. */
        var instance: DaltonAccessibilityService? = null
            private set
    }
}
