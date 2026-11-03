package com.wchsoftworks.daltonpick.overlay

import kotlin.math.abs
import kotlin.math.hypot

/**
 * How a touch on the edge tab is read (spec §4.1). Pure Kotlin, so [EdgeTabView] only feeds it
 * distances and acts on the answer.
 *
 * Only a pull opens the lens: a plain tap on the tab was too easy to make by accident (found in
 * testing on a phone). A tap still puts an open lens away, and TalkBack's double-tap still opens it.
 */
object TabGesture {
    enum class Mode {
        /** Finger down, still within the touch slop: could be a tap, or a hold that starts a carry. */
        NONE,

        /** Moved past the slop without picking a direction yet. No longer a tap. */
        UNDECIDED,

        /** Sliding the tab along its edge. */
        MOVE,

        /** Pulling the lens out. */
        PULL,

        /** Held, then carried toward the other edge. */
        CARRY,
    }

    enum class Release {
        /** Nothing to do (a touch that wandered off without becoming a move or a pull). */
        NOTHING,

        /** A tap with the lens open: put it away. */
        CLOSE_LENS,

        /** A tap with the lens closed: show that the tab is pulled, not tapped. */
        HINT_PULL,

        /** A tap while TalkBack is on (its double-tap can arrive as a touch): open the lens. */
        OPEN_LENS,
        END_PULL,
        SAVE_POSITION,
        END_CARRY,
    }

    /**
     * The mode after the finger has moved [inward] (toward the middle of the screen; negative is
     * toward the edge) and [vertical] since it went down. Once a move, pull or carry starts, it stays.
     */
    fun classify(mode: Mode, inward: Float, vertical: Float, slop: Float, pullThreshold: Float): Mode {
        if (mode != Mode.NONE && mode != Mode.UNDECIDED) return mode
        return when {
            inward > pullThreshold -> Mode.PULL
            abs(vertical) > slop && abs(vertical) > inward -> Mode.MOVE
            hypot(inward, vertical) > slop -> Mode.UNDECIDED
            else -> mode
        }
    }

    /** What lifting the finger does. [cancelled] is ACTION_CANCEL: the touch was taken away. */
    fun release(mode: Mode, cancelled: Boolean, lensOpen: Boolean, touchExploration: Boolean): Release = when (mode) {
        Mode.PULL -> Release.END_PULL
        Mode.MOVE -> Release.SAVE_POSITION
        Mode.CARRY -> Release.END_CARRY
        Mode.UNDECIDED -> Release.NOTHING
        Mode.NONE -> when {
            cancelled -> Release.NOTHING
            lensOpen -> Release.CLOSE_LENS
            touchExploration -> Release.OPEN_LENS
            else -> Release.HINT_PULL
        }
    }
}
