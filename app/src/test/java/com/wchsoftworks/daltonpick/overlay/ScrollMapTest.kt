package com.wchsoftworks.daltonpick.overlay

import com.wchsoftworks.daltonpick.overlay.ScrollMap.Companion.BUFFER
import com.wchsoftworks.daltonpick.overlay.ScrollMap.Companion.EMPTY
import com.wchsoftworks.daltonpick.overlay.ScrollMap.Companion.SNAPSHOT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScrollMapTest {
    /** 100-row screen, band 20..90, scrolled to content row 500 at the band's screen row 0. */
    private fun map() = ScrollMap(100).apply {
        setBand(20, 90, provisional = false)
        scrollPos = 500
        startBuffer(210)
        stored(500 + 20, 500 + 90) // the latest snapshot's band
    }

    private fun ScrollMap.at(y: Int) = locate(y) to row

    @Test
    fun `without a shift everything comes from the snapshot`() {
        val m = map()
        assertEquals(SNAPSHOT to 5, m.at(5))
        assertEquals(SNAPSHOT to 50, m.at(50))
    }

    @Test
    fun `fixed rows never shift`() {
        val m = map().apply { shift = 30 }
        assertEquals(SNAPSHOT to 10, m.at(10))
        assertEquals(SNAPSHOT to 95, m.at(95))
    }

    @Test
    fun `band rows shift within the snapshot, then fall back to the buffer or empty`() {
        val m = map().apply {
            stored(590, 650) // content seen earlier, below the current view
            shift = 30
        }
        assertEquals(SNAPSHOT to 60, m.at(30))
        // Screen row 70 + 30 = 100 is past the band: content row 600, in the buffer.
        assertEquals(BUFFER to 600 % 210, m.at(70))
        assertEquals(BUFFER to 619 % 210, m.at(89))
    }

    @Test
    fun `content never seen is empty`() {
        val m = map().apply { shift = -30 }
        // Row 25 - 30 = -5 is content 495, above anything stored.
        assertEquals(EMPTY, m.locate(25))
        assertEquals(SNAPSHOT to 20, m.at(50))
    }

    @Test
    fun `runs split at the band edge and at the buffer wrap`() {
        val m = ScrollMap(100).apply {
            setBand(20, 90, provisional = false)
            scrollPos = 0
            startBuffer(100)
            stored(80, 190)
            shift = 75
        }
        val runs = mutableListOf<List<Int>>()
        m.runs(10, 40) { kind, src, top, count -> runs += listOf(kind, src, top, count) }
        assertEquals(
            listOf(
                listOf(SNAPSHOT, 10, 10, 10), // fixed header
                listOf(BUFFER, 95, 20, 5), // content 95..99
                listOf(BUFFER, 0, 25, 15), // content 100.. wraps to buffer row 0
            ),
            runs,
        )
    }

    @Test
    fun `the buffer keeps the rows nearest the latest content`() {
        val m = ScrollMap(100).apply {
            setBand(0, 100, provisional = false)
            startBuffer(300)
        }
        m.stored(0, 100)
        m.stored(100, 200)
        m.stored(200, 300)
        m.stored(300, 400) // scrolled on: the top 100 rows go
        assertFalse(m.isStored(50))
        assertTrue(m.isStored(150))
        assertTrue(m.isStored(399))
        m.stored(50, 150) // scrolled back up: now the bottom goes
        assertTrue(m.isStored(60))
        assertFalse(m.isStored(380))
        assertEquals(300, m.stored)
    }

    @Test
    fun `a gap starts the buffer over`() {
        val m = ScrollMap(100).apply { startBuffer(300) }
        m.stored(0, 100)
        m.stored(500, 600)
        assertFalse(m.isStored(50))
        assertTrue(m.isStored(550))
    }
}
