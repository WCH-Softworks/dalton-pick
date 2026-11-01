package com.wchsoftworks.daltonpick

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.util.Log
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

    private val debuggable get() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    override fun onServiceConnected() {
        instance = this
        // Debug builds also listen to their own TalkBack announcements, so tests can read them from logcat.
        if (debuggable) serviceInfo = serviceInfo.apply { eventTypes = eventTypes or AccessibilityEvent.TYPE_ANNOUNCEMENT }
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

    /**
     * Only the event type and, for scrolls, the distance scrolled are used, never the content of other
     * apps (canRetrieveWindowContent is off): the lens follows the scroll, and a scroll or a new screen
     * underneath means its snapshot is out of date.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.packageName == packageName) {
            if (event.eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT) Log.i("DaltonPick", "announced: ${event.text.joinToString(" ")}")
            return
        }
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED ->
                runtime?.onScrolled(event.scrollDeltaX, event.scrollDeltaY, event.scrollY, event.className)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> runtime?.onWindowChanged()
        }
    }

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
