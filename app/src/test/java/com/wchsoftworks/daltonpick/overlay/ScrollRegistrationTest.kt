package com.wchsoftworks.daltonpick.overlay

import com.wchsoftworks.daltonpick.overlay.ScrollRegistration.Shifted
import com.wchsoftworks.daltonpick.overlay.ScrollRegistration.Still
import com.wchsoftworks.daltonpick.overlay.ScrollRegistration.Unrelated
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScrollRegistrationTest {
    private val random = Random(42)

    /** A long scrollable page of distinct rows; [at] is the window onto it. */
    private val page = IntArray(2000) { random.nextInt() }

    private fun window(at: Int, rows: Int) = IntArray(rows) { page[at + it] }

    /** A screen: [header] fixed rows, a band showing the page from [scroll], [footer] fixed rows. */
    private fun screen(scroll: Int, height: Int = 300, header: Int = 40, footer: Int = 30): IntArray {
        val fixedTop = IntArray(header) { 1_000_000 + it }
        val fixedBottom = IntArray(footer) { 2_000_000 + it }
        return fixedTop + window(scroll, height - header - footer) + fixedBottom
    }

    @Test
    fun `fixed header and footer with a shifted middle`() {
        val result = ScrollRegistration.register(screen(100), screen(137), guess = null)
        assertIs<Shifted>(result)
        assertEquals(37, result.offset)
        assertEquals(40, result.bandTop)
        assertEquals(270, result.bandBottom)
        assertTrue(result.confidence > 0.99f)
    }

    @Test
    fun `scrolling back up gives a negative offset and the same band`() {
        val result = ScrollRegistration.register(screen(137), screen(100), guess = -37)
        assertIs<Shifted>(result)
        assertEquals(-37, result.offset)
        assertEquals(40, result.bandTop)
        assertEquals(270, result.bandBottom)
    }

    @Test
    fun `pure shift covers the whole screen`() {
        val result = ScrollRegistration.register(window(500, 300), window(488, 300), guess = null)
        assertIs<Shifted>(result)
        assertEquals(-12, result.offset)
        assertEquals(0, result.bandTop)
        assertEquals(300, result.bandBottom)
    }

    @Test
    fun `identical snapshots are still`() {
        assertEquals(Still, ScrollRegistration.register(screen(100), screen(100), guess = null))
    }

    @Test
    fun `a few rows changing in place is still`() {
        val new = screen(100).copyOf()
        for (i in 5..8) new[i] = random.nextInt() // the status bar clock
        assertEquals(Still, ScrollRegistration.register(screen(100), new, guess = 0))
    }

    @Test
    fun `different screens are unrelated`() {
        val other = IntArray(300) { random.nextInt() }
        assertIs<Unrelated>(ScrollRegistration.register(screen(100), other, guess = 20))
    }

    @Test
    fun `the images win over a wrong guess when they are clear`() {
        val result = ScrollRegistration.register(screen(100), screen(110), guess = 80)
        assertIs<Shifted>(result)
        assertEquals(10, result.offset)
    }

    /** List items that look exactly alike: every 25 rows repeat. */
    private fun repeating(scroll: Int): IntArray {
        val item = IntArray(25) { 5_000 + it * 7 }
        val header = IntArray(40) { 1_000_000 + it }
        return header + IntArray(260) { item[(scroll + it) % 25] }
    }

    @Test
    fun `repeating rows without a guess are ambiguous`() {
        assertIs<Unrelated>(ScrollRegistration.register(repeating(0), repeating(60), guess = null))
    }

    @Test
    fun `repeating rows take the offset nearest the guess`() {
        val result = ScrollRegistration.register(repeating(0), repeating(60), guess = 58)
        assertIs<Shifted>(result)
        assertEquals(60, result.offset)
    }

    @Test
    fun `repeating rows with a guess halfway between are ambiguous`() {
        // 60 matches like 35 and 85 do; a guess of 48 is about as near 35 as 60.
        assertIs<Unrelated>(ScrollRegistration.register(repeating(0), repeating(60), guess = 48))
    }

    @Test
    fun `noise in a few rows still registers`() {
        val old = screen(300)
        val new = screen(345).copyOf()
        val noisy = (40 until 270).shuffled(random).take(12)
        for (i in noisy) new[i] = random.nextInt()
        val result = ScrollRegistration.register(old, new, guess = null)
        assertIs<Shifted>(result)
        assertEquals(45, result.offset)
        assertTrue(result.confidence >= ScrollRegistration.MIN_CONFIDENCE, "confidence ${result.confidence}")
    }

    @Test
    fun `blank rows and solid blocks do not outvote real content`() {
        val blank = 0xFFFFFF
        // Text rows separated by blank gaps, plus a tall solid button.
        val content = IntArray(1200) { i ->
            when {
                i % 40 >= 30 -> blank
                i in 600..700 -> 0x2196F3
                else -> random.nextInt()
            }
        }
        val old = IntArray(300) { content[400 + it] }
        val new = IntArray(300) { content[453 + it] }
        val result = ScrollRegistration.register(old, new, guess = null)
        assertIs<Shifted>(result)
        assertEquals(53, result.offset)
    }

    @Test
    fun `blank rows scrolled into view do not cut the band short`() {
        val blank = 0xFFFFFF
        val content = IntArray(1000) { i -> if (i % 50 >= 40) blank else random.nextInt() }
        val footer = IntArray(30) { 2_000_000 + it }
        val old = IntArray(270) { content[100 + it] } + footer
        val new = IntArray(270) { content[150 + it] } + footer
        // Row 245 is blank in both (content 345 and 395): the revealed rows go on past it.
        assertEquals(blank, old[245])
        assertEquals(blank, new[245])
        val result = ScrollRegistration.register(old, new, guess = 50)
        assertIs<Shifted>(result)
        assertEquals(50, result.offset)
        assertEquals(270, result.bandBottom)
    }

    @Test
    fun `sizes must match`() {
        assertIs<Unrelated>(ScrollRegistration.register(IntArray(10), IntArray(12), guess = null))
    }

    @Test
    fun `row hash ignores columns outside the range`() {
        val a = intArrayOf(1, 2, 3, 4, 5)
        val b = intArrayOf(9, 2, 3, 4, 9)
        assertEquals(ScrollRegistration.rowHash(a, 1, 4), ScrollRegistration.rowHash(b, 1, 4))
        assertTrue(ScrollRegistration.rowHash(a, 0, 5) != ScrollRegistration.rowHash(b, 0, 5))
    }
}
