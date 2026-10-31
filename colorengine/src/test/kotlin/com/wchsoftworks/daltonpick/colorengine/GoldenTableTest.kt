package com.wchsoftworks.daltonpick.colorengine

import kotlin.test.Test
import kotlin.test.assertTrue

class GoldenTableTest {
    private val namer = ColorNamer()

    @Test
    fun `golden table names`() {
        val wrong = GoldenTable.names.mapNotNull { case ->
            val got = namer.match(case.rgb).entry.key
            if (got in case.keys.split('|')) null
            else "${Srgb.toHex(case.rgb)}: expected ${case.keys}, got $got (${case.source})"
        }
        assertTrue(wrong.isEmpty(), wrong.joinToString("\n"))
    }

    @Test
    fun `golden table families`() {
        val wrong = GoldenTable.families.mapNotNull { case ->
            val got = namer.match(case.rgb).family.name
            if (got in case.families.split('|')) null
            else "${Srgb.toHex(case.rgb)}: expected ${case.families}, got $got (${case.source})"
        }
        assertTrue(wrong.isEmpty(), wrong.joinToString("\n"))
    }

    @Test
    fun `golden table has no duplicate rows and every key exists`() {
        val keys = Palette.Default.entries.map { it.key }.toSet()
        for (case in GoldenTable.names) {
            for (k in case.keys.split('|')) assertTrue(k in keys, "unknown key $k (${case.source})")
        }
        assertTrue(GoldenTable.names.size + GoldenTable.families.size >= 200)
    }
}
