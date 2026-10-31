package com.wchsoftworks.daltonpick.colorengine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HysteresisNamerTest {
    private fun lerp(from: Int, to: Int, t: Double): Int {
        fun ch(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * t + 0.5).toInt()
        }
        return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun namesAlong(sweep: List<Int>, match: (Int) -> ColorMatch) = sweep.map { match(it).entry.key }

    private fun changes(names: List<String>) = names.zipWithNext().count { (a, b) -> a != b }

    @Test
    fun `gradient sweeps never flip back to an earlier name`() {
        val pairs = listOf(0xFF0000 to 0x00FF00, 0x0000FF to 0xFFFF00, 0x000000 to 0xFFFFFF, 0xFF00FF to 0x00FFFF)
        for ((from, to) in pairs) {
            val sweep = (0..511).map { lerp(from, to, it / 511.0) }
            val hysteresis = HysteresisNamer()
            val names = namesAlong(sweep, hysteresis::match)
            val runs = names.fold(mutableListOf<String>()) { acc, n -> if (acc.lastOrNull() != n) acc += n; acc }
            assertEquals(runs.size, runs.toSet().size, "flip-back on ${Srgb.toHex(from)}→${Srgb.toHex(to)}: $runs")
        }
    }

    @Test
    fun `noise at a boundary does not make the label flicker`() {
        val namer = ColorNamer()
        val random = Random(1)
        // A color halfway between two anchors, jittered by ±3 per channel like anti-aliasing noise.
        val boundary = lerp(Palette.Default["red"].rgb, Palette.Default["crimson"].rgb, 0.5)
        val jittered = List(400) {
            fun ch(shift: Int) = (((boundary shr shift) and 0xFF) + random.nextInt(-3, 4)).coerceIn(0, 255)
            (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
        val raw = changes(namesAlong(jittered, namer::match))
        val held = changes(namesAlong(jittered, HysteresisNamer(namer)::match))
        println("Boundary noise: $raw raw label changes, $held with hysteresis")
        assertTrue(held <= 2 && held < raw, "raw=$raw held=$held")
    }

    @Test
    fun `a clearly different color still switches immediately`() {
        val hysteresis = HysteresisNamer()
        assertEquals("red", hysteresis.match(0xFF0000).entry.key)
        assertEquals("blue", hysteresis.match(0x0000FF).entry.key)
    }

    @Test
    fun `reset forgets the held name`() {
        val hysteresis = HysteresisNamer(margin = 1.0)
        hysteresis.match(0xFF0000)
        hysteresis.reset()
        assertEquals("blue", hysteresis.match(0x0000FF).entry.key)
    }
}
