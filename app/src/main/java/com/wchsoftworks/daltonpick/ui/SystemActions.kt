package com.wchsoftworks.daltonpick.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.wchsoftworks.daltonpick.DaltonAccessibilityService
import com.wchsoftworks.daltonpick.R

/** The system screens and pages the main app sends people to. Each one falls back if it can't open. */
object SystemActions {
    /**
     * The Accessibility list. The fragment-args extras are an unofficial but long-standing Settings
     * convention that scrolls to and highlights DaltonPick's row; settings apps that ignore them
     * just show the list.
     */
    fun openAccessibility(context: Context) {
        val component = ComponentName(context, DaltonAccessibilityService::class.java).flattenToString()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(EXTRA_FRAGMENT_ARG_KEY, component)
            .putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, Bundle().apply { putString(EXTRA_FRAGMENT_ARG_KEY, component) })
        start(context, intent)
    }

    private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
    private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

    /** The Accessibility list itself, where Android's own Color correction lives. */
    fun openAccessibilitySettingsList(context: Context) {
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /** App info, where sideloaded apps on Android 13+ unlock "restricted settings" (⋮ menu). */
    fun openAppInfo(context: Context) {
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }

    fun openOverlayPermission(context: Context) {
        start(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
    }

    fun openNotificationSettings(context: Context) {
        start(context, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }

    /** Opens a web page in the user's browser; the app itself never goes online. */
    fun openUrl(context: Context, url: String) {
        if (!start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
            Toast.makeText(context, R.string.no_browser, Toast.LENGTH_LONG).show()
        }
    }

    /** The screen-recording consent dialog. Android 14+ is told to offer only the whole screen. */
    fun screenCaptureIntent(context: Context): Intent {
        val mpm = context.getSystemService(MediaProjectionManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mpm.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            mpm.createScreenCaptureIntent()
        }
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
