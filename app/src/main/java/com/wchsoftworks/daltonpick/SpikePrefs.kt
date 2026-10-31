package com.wchsoftworks.daltonpick

import android.content.Context

/** Settings for the M0 test build. Plain SharedPreferences; DataStore comes with the real app (M4). */
class SpikePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("spike", Context.MODE_PRIVATE)

    var hideBeforeCapture: Boolean
        get() = prefs.getBoolean("hide_before_capture", true)
        set(value) = prefs.edit().putBoolean("hide_before_capture", value).apply()

    var extraDelayMs: Int
        get() = prefs.getInt("extra_delay_ms", 32)
        set(value) = prefs.edit().putInt("extra_delay_ms", value).apply()

    var tabOnRight: Boolean
        get() = prefs.getBoolean("tab_on_right", true)
        set(value) = prefs.edit().putBoolean("tab_on_right", value).apply()

    /** Tab's top edge in screen pixels, or -1 for the default position. */
    var tabY: Int
        get() = prefs.getInt("tab_y", -1)
        set(value) = prefs.edit().putInt("tab_y", value).apply()

    var lastCapture: String?
        get() = prefs.getString("last_capture", null)
        set(value) = prefs.edit().putString("last_capture", value).apply()

    companion object {
        val EXTRA_DELAY_STEPS = intArrayOf(0, 16, 32, 64, 100)
    }
}
