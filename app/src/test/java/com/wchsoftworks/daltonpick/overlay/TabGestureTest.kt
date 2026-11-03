package com.wchsoftworks.daltonpick.overlay

import com.wchsoftworks.daltonpick.overlay.TabGesture.Mode
import com.wchsoftworks.daltonpick.overlay.TabGesture.Release
import kotlin.test.Test
import kotlin.test.assertEquals

class TabGestureTest {
    // Pixels at density 3: touch slop 8 dp, pull threshold 24 dp.
    private val slop = 24f
    private val pull = 72f

    /** Feeds a finger path (inward, vertical) point by point, as ACTION_MOVE events would. */
    private fun track(vararg points: Pair<Float, Float>, start: Mode = Mode.NONE): Mode =
        points.fold(start) { mode, (inward, vertical) -> TabGesture.classify(mode, inward, vertical, slop, pull) }

    private fun release(mode: Mode, lensOpen: Boolean = false, touchExploration: Boolean = false, cancelled: Boolean = false) =
        TabGesture.release(mode, cancelled, lensOpen, touchExploration)

    @Test
    fun `a wobble inside the slop is still a tap`() {
        assertEquals(Mode.NONE, track(3f to 2f, -4f to 6f, 10f to -12f))
    }

    @Test
    fun `a tap with the lens closed only hints at the pull`() {
        assertEquals(Release.HINT_PULL, release(Mode.NONE))
    }

    @Test
    fun `a tap with the lens open puts it away`() {
        assertEquals(Release.CLOSE_LENS, release(Mode.NONE, lensOpen = true))
    }

    @Test
    fun `with TalkBack on a tap still opens the lens`() {
        // TalkBack's double-tap can reach the view as a touch at its centre instead of ACTION_CLICK.
        assertEquals(Release.OPEN_LENS, release(Mode.NONE, touchExploration = true))
        assertEquals(Release.CLOSE_LENS, release(Mode.NONE, lensOpen = true, touchExploration = true))
    }

    @Test
    fun `a cancelled touch does nothing`() {
        assertEquals(Release.NOTHING, release(Mode.NONE, cancelled = true))
        assertEquals(Release.NOTHING, release(Mode.NONE, lensOpen = true, cancelled = true))
    }

    @Test
    fun `a short inward swipe is neither a tap nor a pull`() {
        // Used to count as a tap and open the lens at screen centre.
        val mode = track(20f to 0f, 45f to 0f)
        assertEquals(Mode.UNDECIDED, mode)
        assertEquals(Release.NOTHING, release(mode))
        assertEquals(Release.NOTHING, release(mode, lensOpen = true))
    }

    @Test
    fun `pulling past the threshold opens the lens`() {
        val mode = track(30f to 0f, 60f to 4f, 80f to 6f)
        assertEquals(Mode.PULL, mode)
        assertEquals(Release.END_PULL, release(mode))
    }

    @Test
    fun `a short swipe can still become a pull`() {
        assertEquals(Mode.PULL, track(40f to 0f, 50f to 0f, 90f to 0f))
    }

    @Test
    fun `mostly vertical drags move the tab`() {
        val mode = track(2f to 10f, 4f to 30f, 6f to 120f)
        assertEquals(Mode.MOVE, mode)
        assertEquals(Release.SAVE_POSITION, release(mode))
    }

    @Test
    fun `dragging outward along the edge moves the tab`() {
        assertEquals(Mode.MOVE, track(-10f to 30f))
    }

    @Test
    fun `once moving, going inward does not start a pull`() {
        assertEquals(Mode.MOVE, track(0f to 40f, 200f to 40f))
    }

    @Test
    fun `once pulling, going vertical does not move the tab`() {
        assertEquals(Mode.PULL, track(80f to 0f, 80f to 400f))
    }

    @Test
    fun `a carry stays a carry and snaps on release`() {
        assertEquals(Mode.CARRY, track(300f to 10f, -50f to 500f, start = Mode.CARRY))
        assertEquals(Release.END_CARRY, release(Mode.CARRY))
    }
}
