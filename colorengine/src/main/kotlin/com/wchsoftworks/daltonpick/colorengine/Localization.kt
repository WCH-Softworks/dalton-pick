package com.wchsoftworks.daltonpick.colorengine

/** Languages with hand-written color names. Anything else falls back to English. */
enum class Lang(val tag: String) {
    EN("en"), ES("es"), PT_BR("pt-BR");

    companion object {
        /** Maps a BCP 47 tag ("pt-BR", "es-MX", "en-US", "pt") to the closest supported language. */
        fun fromTag(tag: String): Lang {
            val primary = tag.substringBefore('-').substringBefore('_').lowercase()
            return when (primary) {
                "pt" -> PT_BR
                "es" -> ES
                else -> EN
            }
        }
    }
}

/** Light/dark modifier relative to the family's typical lightness. */
enum class Tone { PALE, LIGHT, DARK, DEEP }

/** Text for the lens label: [name] is the big line, [detail] the smaller "family · tone" line. */
data class ColorLabel(val name: String, val detail: String)

object ColorLocalization {
    private val familyNames: Map<Lang, Map<Family, String>> = mapOf(
        Lang.EN to mapOf(
            Family.RED to "Red", Family.ORANGE to "Orange", Family.YELLOW to "Yellow",
            Family.GREEN to "Green", Family.TEAL to "Teal", Family.BLUE to "Blue",
            Family.PURPLE to "Purple", Family.PINK to "Pink", Family.BROWN to "Brown",
            Family.GRAY to "Gray", Family.BLACK to "Black", Family.WHITE to "White",
        ),
        Lang.ES to mapOf(
            Family.RED to "Rojo", Family.ORANGE to "Naranja", Family.YELLOW to "Amarillo",
            Family.GREEN to "Verde", Family.TEAL to "Verde azulado", Family.BLUE to "Azul",
            Family.PURPLE to "Morado", Family.PINK to "Rosa", Family.BROWN to "Marrón",
            Family.GRAY to "Gris", Family.BLACK to "Negro", Family.WHITE to "Blanco",
        ),
        Lang.PT_BR to mapOf(
            Family.RED to "Vermelho", Family.ORANGE to "Laranja", Family.YELLOW to "Amarelo",
            Family.GREEN to "Verde", Family.TEAL to "Verde-azulado", Family.BLUE to "Azul",
            Family.PURPLE to "Roxo", Family.PINK to "Rosa", Family.BROWN to "Marrom",
            Family.GRAY to "Cinza", Family.BLACK to "Preto", Family.WHITE to "Branco",
        ),
    )

    // Color nouns are grammatically masculine in Spanish and Portuguese ("el rosa", "o rosa"),
    // so the masculine adjective is always correct after the family name.
    private val toneNames: Map<Lang, Map<Tone, String>> = mapOf(
        Lang.EN to mapOf(Tone.PALE to "pale", Tone.LIGHT to "light", Tone.DARK to "dark", Tone.DEEP to "deep"),
        Lang.ES to mapOf(Tone.PALE to "pálido", Tone.LIGHT to "claro", Tone.DARK to "oscuro", Tone.DEEP to "intenso"),
        Lang.PT_BR to mapOf(Tone.PALE to "pálido", Tone.LIGHT to "claro", Tone.DARK to "escuro", Tone.DEEP to "intenso"),
    )

    fun family(family: Family, lang: Lang): String = familyNames.getValue(lang).getValue(family)

    fun tone(tone: Tone, lang: Lang): String = toneNames.getValue(lang).getValue(tone)

    fun label(match: ColorMatch, lang: Lang): ColorLabel {
        val familyText = family(match.family, lang)
        val detail = match.tone?.let { "$familyText · ${tone(it, lang)}" } ?: familyText
        return ColorLabel(name = match.entry.name(lang), detail = detail)
    }
}
