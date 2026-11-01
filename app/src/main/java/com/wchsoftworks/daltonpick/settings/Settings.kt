package com.wchsoftworks.daltonpick.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

enum class Edge { LEFT, RIGHT }

/** Everything the overlay reads from the user's settings (spec §4.5). */
data class Settings(
    val lensSizeDp: Int = DEFAULT_LENS_SIZE_DP,
    val zoom: Int = DEFAULT_ZOOM,
    val edge: Edge = Edge.RIGHT,
    val autoDim: Boolean = true,
    /** Side of the sampled square in pixels: 1, 3 or 5 (spec §5.3). */
    val sampleSize: Int = DEFAULT_SAMPLE_SIZE,
    val showHex: Boolean = false,
    val haptics: Boolean = true,
    /** Developer setting: adds the capture debug line to the label. */
    val showDebug: Boolean = false,
    /** Whether the edge tab is shown. The notification and Quick Settings tile flip this. */
    val tabVisible: Boolean = true,
    /** Tab's top edge as a fraction of the screen height, so it survives rotation. */
    val tabPosition: Float = DEFAULT_TAB_POSITION,
    /** First-run onboarding finished (or skipped); the app opens on the home screen after that. */
    val onboardingDone: Boolean = false,
) {
    val sampleRadius: Int get() = sampleSize / 2

    companion object {
        const val DEFAULT_LENS_SIZE_DP = 140
        const val DEFAULT_ZOOM = 3
        const val DEFAULT_SAMPLE_SIZE = 3
        const val DEFAULT_TAB_POSITION = 0.33f
        val LENS_SIZE_RANGE_DP = 100..200
        val ZOOM_RANGE = 2..8
        val SAMPLE_SIZES = intArrayOf(1, 3, 5)
    }
}

internal object SettingsKeys {
    val lensSize = intPreferencesKey("lens_size_dp")
    val zoom = intPreferencesKey("zoom")
    val edge = stringPreferencesKey("edge")
    val autoDim = booleanPreferencesKey("auto_dim")
    val sampleSize = intPreferencesKey("sample_size")
    val showHex = booleanPreferencesKey("show_hex")
    val haptics = booleanPreferencesKey("haptics")
    val showDebug = booleanPreferencesKey("show_debug")
    val tabVisible = booleanPreferencesKey("tab_visible")
    val tabPosition = floatPreferencesKey("tab_position")
    val onboardingDone = booleanPreferencesKey("onboarding_done")
}

/** Reads stored values, falling back to defaults and clamping anything out of range. */
internal fun Preferences.toSettings(): Settings {
    val d = Settings()
    val sample = this[SettingsKeys.sampleSize] ?: d.sampleSize
    return Settings(
        lensSizeDp = (this[SettingsKeys.lensSize] ?: d.lensSizeDp).coerceIn(Settings.LENS_SIZE_RANGE_DP),
        zoom = (this[SettingsKeys.zoom] ?: d.zoom).coerceIn(Settings.ZOOM_RANGE),
        edge = this[SettingsKeys.edge]?.let { name -> Edge.entries.firstOrNull { it.name == name } } ?: d.edge,
        autoDim = this[SettingsKeys.autoDim] ?: d.autoDim,
        sampleSize = if (sample in Settings.SAMPLE_SIZES) sample else d.sampleSize,
        showHex = this[SettingsKeys.showHex] ?: d.showHex,
        haptics = this[SettingsKeys.haptics] ?: d.haptics,
        showDebug = this[SettingsKeys.showDebug] ?: d.showDebug,
        tabVisible = this[SettingsKeys.tabVisible] ?: d.tabVisible,
        tabPosition = (this[SettingsKeys.tabPosition] ?: d.tabPosition).coerceIn(0f, 1f),
        onboardingDone = this[SettingsKeys.onboardingDone] ?: d.onboardingDone,
    )
}

internal fun MutablePreferences.write(s: Settings) {
    this[SettingsKeys.lensSize] = s.lensSizeDp
    this[SettingsKeys.zoom] = s.zoom
    this[SettingsKeys.edge] = s.edge.name
    this[SettingsKeys.autoDim] = s.autoDim
    this[SettingsKeys.sampleSize] = s.sampleSize
    this[SettingsKeys.showHex] = s.showHex
    this[SettingsKeys.haptics] = s.haptics
    this[SettingsKeys.showDebug] = s.showDebug
    this[SettingsKeys.tabVisible] = s.tabVisible
    this[SettingsKeys.tabPosition] = s.tabPosition
    this[SettingsKeys.onboardingDone] = s.onboardingDone
}
