package com.wchsoftworks.daltonpick.overlay

/**
 * Where each row of the lens's picture of the screen comes from while the app underneath scrolls.
 * Pure Kotlin bookkeeping; [ScreenContent] holds the actual bitmaps.
 *
 * Rows are snapshot rows. The scrolling band is [bandTop, bandBottom); everything else is fixed
 * (toolbars, bottom bars) and always comes from the latest snapshot. Inside the band, screen row
 * `y` of the latest snapshot shows content row `y + scrollPos`. Content already seen is kept in a
 * ring buffer of [capacity] rows, content row `c` at buffer row `c mod capacity`.
 */
class ScrollMap(val height: Int) {
    var bandTop = 0
        private set
    var bandBottom = 0
        private set

    /** True while the band is only a guess (no two snapshots lined up yet). */
    var provisional = false
        private set

    val hasBand get() = bandBottom > bandTop

    /** Content row shown at screen row 0 of the band in the latest snapshot. */
    var scrollPos = 0

    /** Rows scrolled since the latest snapshot; applied to band rows only. */
    var shift = 0

    var capacity = 0
        private set
    private var validTop = 0
    private var validBottom = 0

    val stored get() = validBottom - validTop

    fun inBand(y: Int) = y in bandTop until bandBottom

    fun setBand(top: Int, bottom: Int, provisional: Boolean) {
        bandTop = top.coerceIn(0, height)
        bandBottom = bottom.coerceIn(bandTop, height)
        this.provisional = provisional
    }

    /** Starts a buffer of [rows] rows, forgetting anything stored. */
    fun startBuffer(rows: Int) {
        capacity = rows
        validTop = 0
        validBottom = 0
    }

    /** Forgets the band, the scroll position and the buffer. */
    fun reset() {
        bandTop = 0
        bandBottom = 0
        provisional = false
        scrollPos = 0
        shift = 0
        capacity = 0
        validTop = 0
        validBottom = 0
    }

    fun bufferRow(content: Int) = Math.floorMod(content, capacity)

    /**
     * Content rows [from, to) were just written to the buffer. Keeps at most [capacity] rows,
     * dropping the end farther from the new rows.
     */
    fun stored(from: Int, to: Int) {
        if (capacity == 0) return
        if (validBottom <= validTop || to < validTop || from > validBottom) {
            validTop = from
            validBottom = to
        } else {
            validTop = minOf(validTop, from)
            validBottom = maxOf(validBottom, to)
        }
        if (validBottom - validTop > capacity) {
            val newNearBottom = from - validTop > validBottom - to
            if (newNearBottom) validTop = validBottom - capacity else validBottom = validTop + capacity
        }
    }

    fun isStored(content: Int) = capacity > 0 && content in validTop until validBottom

    /** Row in the source returned by the last [locate]. */
    var row = 0
        private set

    /** Which bitmap screen row [y] comes from ([SNAPSHOT], [BUFFER] or [EMPTY]); the row is in [row]. */
    fun locate(y: Int): Int {
        if (shift == 0 || !inBand(y)) {
            row = y
            return SNAPSHOT
        }
        val shifted = y + shift
        if (inBand(shifted)) {
            row = shifted
            return SNAPSHOT
        }
        val content = shifted + scrollPos
        if (isStored(content)) {
            row = bufferRow(content)
            return BUFFER
        }
        row = 0
        return EMPTY
    }

    fun interface RunSink {
        /** Screen rows [top, top + count) come from rows [srcTop, srcTop + count) of [kind]. */
        fun run(kind: Int, srcTop: Int, top: Int, count: Int)
    }

    /** Splits screen rows [top, bottom) into runs that each come from one contiguous source. */
    fun runs(top: Int, bottom: Int, sink: RunSink) {
        if (top >= bottom) return
        var start = top
        var kind = locate(top)
        var src = row
        for (y in top + 1..bottom) {
            val k = if (y < bottom) locate(y) else -1
            val r = row
            if (y == bottom || k != kind || (kind != EMPTY && r != src + (y - start))) {
                sink.run(kind, src, start, y - start)
                start = y
                kind = k
                src = r
            }
        }
    }

    companion object {
        const val SNAPSHOT = 0
        const val BUFFER = 1
        const val EMPTY = 2
    }
}
