package com.wchsoftworks.daltonpick

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppStatusTest {
    private val pkg = "com.wchsoftworks.daltonpick"
    private val cls = "com.wchsoftworks.daltonpick.DaltonAccessibilityService"

    @Test
    fun `short and full component names both count`() {
        assertTrue(AppStatus.isServiceEnabled("$pkg/.DaltonAccessibilityService", pkg, cls))
        assertTrue(AppStatus.isServiceEnabled("$pkg/$cls", pkg, cls))
    }

    @Test
    fun `found among other services`() {
        val setting = "com.google.android.marvin.talkback/.TalkBackService:$pkg/.DaltonAccessibilityService"
        assertTrue(AppStatus.isServiceEnabled(setting, pkg, cls))
    }

    @Test
    fun `missing, empty or similar names do not count`() {
        assertFalse(AppStatus.isServiceEnabled(null, pkg, cls))
        assertFalse(AppStatus.isServiceEnabled("", pkg, cls))
        assertFalse(AppStatus.isServiceEnabled("com.google.android.marvin.talkback/.TalkBackService", pkg, cls))
        assertFalse(AppStatus.isServiceEnabled("$pkg/.OtherService", pkg, cls))
        assertFalse(AppStatus.isServiceEnabled("$pkg.evil/.DaltonAccessibilityService", pkg, cls))
    }
}
