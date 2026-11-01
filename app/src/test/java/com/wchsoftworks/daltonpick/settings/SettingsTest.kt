package com.wchsoftworks.daltonpick.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsTest {
    @Test
    fun `empty preferences give the defaults`() {
        assertEquals(Settings(), preferencesOf().toSettings())
    }

    @Test
    fun `write then read round-trips`() {
        val s = Settings(
            lensSizeDp = 180, zoom = 5, edge = Edge.LEFT, autoDim = false, sampleSize = 5,
            showHex = true, haptics = false, showDebug = true, tabVisible = false, tabPosition = 0.6f,
        )
        val prefs = mutablePreferencesOf().apply { write(s) }
        assertEquals(s, prefs.toSettings())
    }

    @Test
    fun `out-of-range values are clamped`() {
        val prefs = mutablePreferencesOf().apply {
            this[SettingsKeys.lensSize] = 900
            this[SettingsKeys.zoom] = 1
            this[SettingsKeys.sampleSize] = 4
            this[SettingsKeys.edge] = "TOP"
            this[SettingsKeys.tabPosition] = 3f
        }
        val s = prefs.toSettings()
        assertEquals(200, s.lensSizeDp)
        assertEquals(2, s.zoom)
        assertEquals(3, s.sampleSize)
        assertEquals(Edge.RIGHT, s.edge)
        assertEquals(1f, s.tabPosition)
    }
}
