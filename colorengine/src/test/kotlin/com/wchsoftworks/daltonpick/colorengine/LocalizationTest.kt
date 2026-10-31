package com.wchsoftworks.daltonpick.colorengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalizationTest {
    @Test
    fun `every palette entry has a name in every language`() {
        for (entry in Palette.Default.entries) for (lang in Lang.entries) {
            assertTrue(!entry.names[lang].isNullOrBlank(), "${entry.key} missing $lang")
        }
    }

    @Test
    fun `names are unique within each language`() {
        for (lang in Lang.entries) {
            val dupes = Palette.Default.entries.groupBy { it.name(lang) }.filterValues { it.size > 1 }
            assertTrue(dupes.isEmpty(), "$lang duplicate names: ${dupes.keys}")
        }
    }

    @Test
    fun `families and tones are translated`() {
        for (lang in Lang.entries) {
            Family.entries.forEach { ColorLocalization.family(it, lang) }
            Tone.entries.forEach { ColorLocalization.tone(it, lang) }
        }
    }

    @Test
    fun `language tags map to the closest supported language`() {
        assertEquals(Lang.PT_BR, Lang.fromTag("pt-BR"))
        assertEquals(Lang.PT_BR, Lang.fromTag("pt_PT"))
        assertEquals(Lang.ES, Lang.fromTag("es-419"))
        assertEquals(Lang.ES, Lang.fromTag("es"))
        assertEquals(Lang.EN, Lang.fromTag("en-US"))
        assertEquals(Lang.EN, Lang.fromTag("fr-FR"))
    }

    @Test
    fun `label combines name family and tone`() {
        val match = ColorNamer().match(0x000080)
        assertEquals(ColorLabel("Navy blue", "Blue · deep"), ColorLocalization.label(match, Lang.EN))
        assertEquals(ColorLabel("Azul marino", "Azul · intenso"), ColorLocalization.label(match, Lang.ES))
        assertEquals(ColorLabel("Azul-marinho", "Azul · intenso"), ColorLocalization.label(match, Lang.PT_BR))
    }
}
