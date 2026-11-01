package com.wchsoftworks.daltonpick

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/** What the lens needs, as the main app shows it (spec §4.5). Read again whenever the app resumes. */
data class AppStatus(
    /** The accessibility service is switched on in system settings (the main capture path). */
    val accessibilityOn: Boolean,
    /** Notifications can be posted (always true before Android 13). */
    val notificationsAllowed: Boolean,
    /** "Display over other apps", needed only by the screen-recording fallback. */
    val overlayAllowed: Boolean,
) {
    companion object {
        fun read(context: Context): AppStatus {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            val notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            return AppStatus(
                accessibilityOn = isServiceEnabled(enabled, context.packageName, DaltonAccessibilityService::class.java.name),
                notificationsAllowed = notifications,
                overlayAllowed = Settings.canDrawOverlays(context),
            )
        }

        /**
         * The system setting is a ':'-separated list of flattened component names, in either the short
         * ("pkg/.Cls") or the full ("pkg/pkg.Cls") form.
         */
        fun isServiceEnabled(setting: String?, packageName: String, className: String): Boolean =
            setting.orEmpty().split(':').any { entry ->
                val pkg = entry.substringBefore('/', "")
                val cls = entry.substringAfter('/', "")
                pkg == packageName && (cls == className || pkg + cls == className)
            }
    }
}
