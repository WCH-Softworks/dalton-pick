package com.wchsoftworks.daltonpick.overlay

import kotlin.math.abs

/**
 * Lines up two snapshots of the same screen taken before and after a vertical scroll, using one
 * hash per pixel row. Pure Kotlin so it can be tested on the JVM.
 *
 * The offset `d` means: row `i` of the new snapshot shows what row `i + d` of the old one showed.
 * Scrolling down (content moving up) gives a positive `d`, the same sign as a scroll event's
 * `scrollDeltaY`.
 */
object ScrollRegistration {
    sealed interface Result

    /** The rows in [bandTop, bandBottom) of the new snapshot moved by [offset]; the rest stayed put. */
    data class Shifted(val offset: Int, val bandTop: Int, val bandBottom: Int, val confidence: Float) : Result

    /** Nothing moved (at most a few rows changed in place, like the clock). */
    data object Still : Result

    /** No trustworthy alignment: a different screen, or too ambiguous to tell. */
    data class Unrelated(val why: String) : Result

    /** Fewer matching rows than this is too little evidence to call it a scroll. */
    const val MIN_MATCHES = 8

    /** Share of comparable rows that must agree with the chosen offset. The rest is noise, or a
     * collapsing toolbar that changes size as the list moves (about 75% agree on Settings pages). */
    const val MIN_CONFIDENCE = 0.7f

    /** Another offset with this share of the best one's votes makes the match ambiguous. */
    const val AMBIGUOUS_SHARE = 0.7f

    /** If at most this share of rows changed and nothing lines up, call it still. */
    const val STILL_SHARE = 0.05f

    /** An unchanged row that appears more often than this is background, not a fixed bar. */
    const val MAX_REPEAT_FIXED = 4

    /** Hash of one row of ARGB pixels, columns [from, to). */
    fun rowHash(pixels: IntArray, from: Int, to: Int): Int {
        var h = -0x7ee3623b // FNV-1a offset basis
        for (x in from until to) {
            h = (h xor pixels[x]) * 0x01000193
        }
        return h
    }

    /**
     * [guess] is the scroll the app reported since the old snapshot, in snapshot rows, or null
     * when unknown. The images win when they agree clearly; the guess only breaks ties between
     * look-alike offsets (repeating list rows).
     */
    fun register(old: IntArray, new: IntArray, guess: Int?): Result {
        val h = old.size
        if (new.size != h || h == 0) return Unrelated("sizes differ")

        val changed = (0 until h).filter { old[it] != new[it] }
        if (changed.isEmpty()) return Still

        // Where each hash appears in the old snapshot. Very common hashes (blank rows) say nothing.
        val positions = HashMap<Int, MutableList<Int>>()
        for (i in 0 until h) positions.getOrPut(old[i]) { ArrayList(1) }.add(i)
        val tooCommon = h / 4

        // Each changed row votes for every offset that would explain it, split evenly between them,
        // so a block of identical rows (a solid button) counts as one row, not a hundred.
        val votes = FloatArray(2 * h - 1)
        for (i in changed) {
            val at = positions[new[i]] ?: continue
            if (at.size > tooCommon) continue
            val weight = 1f / at.size
            for (p in at) votes[p - i + h - 1] += weight
        }

        var bestIndex = 0
        for (k in votes.indices) if (votes[k] > votes[bestIndex]) bestIndex = k
        val best = votes[bestIndex]
        if (best < MIN_MATCHES) return if (changed.size <= h * STILL_SHARE) Still else Unrelated("no offset fits (${changed.size} rows changed)")

        // Look-alike offsets: local peaks with most of the best one's votes.
        val candidates = ArrayList<Int>()
        for (k in votes.indices) {
            val v = votes[k]
            if (v < best * AMBIGUOUS_SHARE) continue
            val left = if (k > 0) votes[k - 1] else 0f
            val right = if (k < votes.size - 1) votes[k + 1] else 0f
            if (v >= left && v >= right) candidates.add(k - (h - 1))
        }
        val offset = pick(candidates, bestIndex - (h - 1), guess) ?: return Unrelated("ambiguous: $candidates")

        // Verify: within the span of rows that moved, nearly every changed row must agree.
        var first = -1
        var last = -1
        for (i in changed) {
            val j = i + offset
            if (j in 0 until h && new[i] == old[j]) {
                if (first < 0) first = i
                last = i
            }
        }
        if (first < 0) return Unrelated("no rows moved")
        var compared = 0
        var matched = 0
        for (i in changed) {
            if (i < first || i > last) continue
            val j = i + offset
            if (j !in 0 until h) continue
            compared++
            if (new[i] == old[j]) matched++
        }
        if (matched < MIN_MATCHES) return Unrelated("only $matched rows match")
        val confidence = matched.toFloat() / compared
        if (confidence < MIN_CONFIDENCE) return Unrelated("offset $offset: $matched of $compared rows match")

        // Rows scrolled into view sit past the moved span, on the side the content came from.
        // Extend over them until a distinctive row that stayed put (a fixed bar), at most |offset|
        // rows. A blank row that merely matches the blank row it replaced doesn't stop it.
        fun fixedBar(i: Int) = old[i] == new[i] && (positions[old[i]]?.size ?: 0) <= MAX_REPEAT_FIXED
        var top = first
        var bottom = last + 1
        if (offset > 0) {
            val limit = minOf(h, bottom + offset)
            while (bottom < limit && !fixedBar(bottom)) bottom++
        } else if (offset < 0) {
            val limit = maxOf(0, top + offset)
            while (top > limit && !fixedBar(top - 1)) top--
        }
        return Shifted(offset, top, bottom, confidence)
    }

    /** The offset to trust among [candidates], or null when it can't be told apart. */
    private fun pick(candidates: List<Int>, best: Int, guess: Int?): Int? {
        if (candidates.size <= 1) return best
        if (guess == null) return null
        val sorted = candidates.sortedBy { abs(it - guess) }
        val nearest = abs(sorted[0] - guess)
        val next = abs(sorted[1] - guess)
        return if (nearest * 2 < next) sorted[0] else null
    }
}
