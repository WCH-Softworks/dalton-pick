package com.wchsoftworks.daltonpick.colorengine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorNamerTest {
    private val namer = ColorNamer()

    private fun nameOf(rgb: Int) = namer.match(rgb).entry.name(Lang.EN)

    @Test
    fun `every palette anchor names itself`() {
        for (entry in Palette.Default.entries) {
            assertEquals(entry.key, namer.match(entry.rgb).entry.key, "anchor $entry")
        }
    }

    @Test
    fun `palette anchors are not near-duplicates`() {
        val entries = Palette.Default.entries
        val tooClose = mutableListOf<String>()
        for (i in entries.indices) for (j in i + 1 until entries.size) {
            val d = entries[i].lab.distanceTo(entries[j].lab)
            if (d < MIN_ANCHOR_DISTANCE) tooClose += "${entries[i]} ~ ${entries[j]} (%.3f)".format(d)
        }
        assertTrue(tooClose.isEmpty(), "Anchors too close:\n" + tooClose.joinToString("\n"))
    }

    @Test
    fun `golden names`() {
        val golden = mapOf(
            0xFF0000 to "Red", 0xFE0303 to "Red",
            0x00FF00 to "Bright green", 0x0000FF to "Blue",
            0xFFFF00 to "Yellow", 0x00FFFF to "Cyan", 0xFF00FF to "Magenta",
            0x000000 to "Black", 0x0A0A0A to "Black", 0xFFFFFF to "White",
            0x808080 to "Gray", 0x7F7F7F to "Gray", 0x000080 to "Navy blue",
            0x008080 to "Teal", 0x800080 to "Purple", 0xFFC0CB to "Pink",
            0x228B22 to "Forest green", 0xD8A1A4 to "Dusty rose",
            0xF5F5DC to "Beige", 0x87CEEB to "Sky blue",
        )
        val wrong = golden.filter { (rgb, name) -> nameOf(rgb) != name }
            .map { (rgb, name) -> "${Srgb.toHex(rgb)}: expected $name, got ${nameOf(rgb)}" }
        assertTrue(wrong.isEmpty(), wrong.joinToString("\n"))
    }

    @Test
    fun `material design colors land in the expected family`() {
        val families = mapOf(
            0xF44336 to Family.RED, 0xE91E63 to Family.PINK, 0x9C27B0 to Family.PURPLE,
            0x3F51B5 to Family.BLUE, 0x2196F3 to Family.BLUE, 0x00BCD4 to Family.TEAL,
            0x4CAF50 to Family.GREEN, 0x8BC34A to Family.GREEN, 0xFFEB3B to Family.YELLOW,
            0xFF9800 to Family.ORANGE, 0x795548 to Family.BROWN, 0x9E9E9E to Family.GRAY,
            0x212121 to Family.GRAY,
        )
        val wrong = families.filter { (rgb, f) -> namer.match(rgb).family != f }
            .map { (rgb, f) -> "${Srgb.toHex(rgb)}: expected $f, got ${namer.match(rgb).let { "${it.family} (${it.entry})" }}" }
        assertTrue(wrong.isEmpty(), wrong.joinToString("\n"))
    }

    @Test
    fun `near-neutral colors never get a hue name`() {
        val random = Random(42)
        repeat(2_000) {
            val v = random.nextInt(256)
            val rgb = (v shl 16) or ((v + random.nextInt(-2, 3)).coerceIn(0, 255) shl 8) or v
            val match = namer.match(rgb)
            if (match.sample.chroma < ColorNamer.ACHROMATIC_CHROMA) {
                assertTrue(match.family.isAchromatic, "${match.hex} named ${match.entry}")
            }
        }
    }

    @Test
    fun `tones follow lightness`() {
        assertEquals(Tone.DEEP, namer.match(0x000080).tone) // navy
        assertEquals(Tone.PALE, namer.match(0xFADADD).tone) // pale pink
        assertNull(namer.match(0xFF0000).tone)
        assertNull(namer.match(0x808080).tone) // grays carry lightness in the name
    }

    @Test
    fun `lookup is fast`() {
        val random = Random(7)
        val colors = IntArray(50_000) { random.nextInt(0x1000000) }
        repeat(5_000) { namer.match(colors[it]) } // warm up the JIT
        val start = System.nanoTime()
        for (c in colors) namer.match(c)
        val perLookupMs = (System.nanoTime() - start) / 1e6 / colors.size
        println("ColorNamer: %.4f ms per lookup".format(perLookupMs))
        assertTrue(perLookupMs < 0.2, "too slow: $perLookupMs ms")
    }

    companion object {
        private const val MIN_ANCHOR_DISTANCE = 0.025
    }
}
