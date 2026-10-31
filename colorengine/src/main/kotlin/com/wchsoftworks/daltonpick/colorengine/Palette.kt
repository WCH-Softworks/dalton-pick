package com.wchsoftworks.daltonpick.colorengine

/** Basic color families, shown on the label's second line ("Pink · light"). */
enum class Family(
    /** Typical OKLab lightness of the family, used to decide light/dark tones. */
    val typicalLightness: Double,
    val isAchromatic: Boolean = false,
) {
    RED(0.60), ORANGE(0.72), YELLOW(0.90), GREEN(0.62), TEAL(0.65),
    BLUE(0.50), PURPLE(0.50), PINK(0.75), BROWN(0.50),
    GRAY(0.60, isAchromatic = true),
    BLACK(0.00, isAchromatic = true),
    WHITE(1.00, isAchromatic = true),
}

/** One named color. [names] holds the friendly name in every supported language. */
class PaletteEntry(
    val key: String,
    val rgb: Int,
    val family: Family,
    val names: Map<Lang, String>,
) {
    val lab: Oklab = Oklab.fromRgb(rgb)

    fun name(lang: Lang): String = names[lang] ?: names.getValue(Lang.EN)

    override fun toString() = "$key(${Srgb.toHex(rgb)})"
}

class Palette(val entries: List<PaletteEntry>) {
    init {
        require(entries.isNotEmpty())
        require(entries.map { it.key }.toSet().size == entries.size) { "Duplicate palette keys" }
    }

    val achromatic: List<PaletteEntry> = entries.filter { it.family.isAchromatic }

    operator fun get(key: String): PaletteEntry = entries.first { it.key == key }

    companion object {
        /** Palette format version; bump when names or anchors change. */
        const val VERSION = 1

        val Default: Palette by lazy { Palette(DefaultPaletteData.entries) }
    }
}

/**
 * The curated v1 palette: common-language names only (no "Gamboge").
 * Columns: key, sRGB anchor, family, English, Spanish, Brazilian Portuguese.
 */
internal object DefaultPaletteData {
    private fun c(key: String, rgb: Int, family: Family, en: String, es: String, pt: String) =
        PaletteEntry(key, rgb, family, mapOf(Lang.EN to en, Lang.ES to es, Lang.PT_BR to pt))

    val entries: List<PaletteEntry> = listOf(
        // Reds
        c("red", 0xFF0000, Family.RED, "Red", "Rojo", "Vermelho"),
        c("dark_red", 0x8B0000, Family.RED, "Dark red", "Rojo oscuro", "Vermelho-escuro"),
        c("blood_red", 0x5E0000, Family.RED, "Blood red", "Rojo sangre", "Vermelho-sangue"),
        c("crimson", 0xDC143C, Family.RED, "Crimson", "Carmesí", "Carmesim"),
        c("brick_red", 0xB03A2E, Family.RED, "Brick red", "Rojo ladrillo", "Vermelho-tijolo"),
        c("burgundy", 0x800020, Family.RED, "Burgundy", "Burdeos", "Bordô"),
        c("wine", 0x722F37, Family.RED, "Wine", "Vino", "Vinho"),
        c("tomato_red", 0xFF6347, Family.RED, "Tomato red", "Rojo tomate", "Vermelho-tomate"),
        c("coral_red", 0xFF4040, Family.RED, "Coral red", "Rojo coral", "Vermelho-coral"),

        // Oranges
        c("orange", 0xFF8000, Family.ORANGE, "Orange", "Naranja", "Laranja"),
        c("burnt_orange", 0xCC5500, Family.ORANGE, "Burnt orange", "Naranja quemado", "Laranja-queimado"),
        c("neon_orange", 0xFF5F1F, Family.ORANGE, "Neon orange", "Naranja neón", "Laranja-neon"),
        c("amber", 0xFFBF00, Family.ORANGE, "Amber", "Ámbar", "Âmbar"),
        c("coral", 0xFF7F50, Family.ORANGE, "Coral", "Coral", "Coral"),
        c("peach", 0xFFCBA4, Family.ORANGE, "Peach", "Durazno", "Pêssego"),
        c("light_orange", 0xFFB366, Family.ORANGE, "Light orange", "Naranja claro", "Laranja-claro"),
        c("terracotta", 0xE2725B, Family.ORANGE, "Terracotta", "Terracota", "Terracota"),
        c("rust", 0xB7410E, Family.ORANGE, "Rust", "Óxido", "Ferrugem"),

        // Yellows
        c("yellow", 0xFFFF00, Family.YELLOW, "Yellow", "Amarillo", "Amarelo"),
        c("lemon_yellow", 0xFFF44F, Family.YELLOW, "Lemon yellow", "Amarillo limón", "Amarelo-limão"),
        c("golden_yellow", 0xFFD700, Family.YELLOW, "Golden yellow", "Amarillo dorado", "Amarelo-ouro"),
        c("mustard", 0xE1AD01, Family.YELLOW, "Mustard yellow", "Amarillo mostaza", "Amarelo-mostarda"),
        c("dark_yellow", 0xB8A200, Family.YELLOW, "Dark yellow", "Amarillo oscuro", "Amarelo-escuro"),
        c("pale_yellow", 0xFFFF99, Family.YELLOW, "Pale yellow", "Amarillo pálido", "Amarelo-pálido"),
        c("cream", 0xFFFDD0, Family.YELLOW, "Cream", "Crema", "Creme"),

        // Greens
        c("green", 0x008000, Family.GREEN, "Green", "Verde", "Verde"),
        c("bright_green", 0x00FF00, Family.GREEN, "Bright green", "Verde brillante", "Verde-vivo"),
        c("lime_green", 0x32CD32, Family.GREEN, "Lime green", "Verde lima", "Verde-limão"),
        c("dark_green", 0x005000, Family.GREEN, "Dark green", "Verde oscuro", "Verde-escuro"),
        c("forest_green", 0x228B22, Family.GREEN, "Forest green", "Verde bosque", "Verde-floresta"),
        c("grass_green", 0x5DA130, Family.GREEN, "Grass green", "Verde césped", "Verde-grama"),
        c("emerald", 0x50C878, Family.GREEN, "Emerald green", "Verde esmeralda", "Verde-esmeralda"),
        c("jade", 0x00A86B, Family.GREEN, "Jade", "Jade", "Jade"),
        c("sea_green", 0x2E8B57, Family.GREEN, "Sea green", "Verde mar", "Verde-mar"),
        c("bottle_green", 0x006A4E, Family.GREEN, "Bottle green", "Verde botella", "Verde-garrafa"),
        c("hunter_green", 0x355E3B, Family.GREEN, "Hunter green", "Verde cazador", "Verde-caçador"),
        c("olive_green", 0x808000, Family.GREEN, "Olive green", "Verde oliva", "Verde-oliva"),
        c("dark_olive", 0x556B2F, Family.GREEN, "Dark olive green", "Verde oliva oscuro", "Verde-oliva-escuro"),
        c("army_green", 0x4B5320, Family.GREEN, "Army green", "Verde militar", "Verde-militar"),
        c("moss_green", 0x8A9A5B, Family.GREEN, "Moss green", "Verde musgo", "Verde-musgo"),
        c("sage_green", 0x9CAF88, Family.GREEN, "Sage green", "Verde salvia", "Verde-sálvia"),
        c("pistachio", 0x93C572, Family.GREEN, "Pistachio", "Pistacho", "Pistache"),
        c("yellow_green", 0x9ACD32, Family.GREEN, "Yellow-green", "Verde amarillento", "Verde-amarelado"),
        c("light_green", 0x90EE90, Family.GREEN, "Light green", "Verde claro", "Verde-claro"),
        c("mint_green", 0xAAF0D1, Family.GREEN, "Mint green", "Verde menta", "Verde-menta"),
        c("pale_green", 0xC1E1C1, Family.GREEN, "Pale green", "Verde pálido", "Verde-pálido"),
        c("seafoam", 0x71D9A5, Family.GREEN, "Seafoam green", "Verde agua", "Verde-água"),

        // Teals and cyans
        c("teal", 0x008080, Family.TEAL, "Teal", "Verde azulado", "Verde-azulado"),
        c("petrol_blue", 0x005060, Family.TEAL, "Petrol blue", "Azul petróleo", "Azul-petróleo"),
        c("turquoise", 0x40E0D0, Family.TEAL, "Turquoise", "Turquesa", "Turquesa"),
        c("dark_turquoise", 0x00CED1, Family.TEAL, "Dark turquoise", "Turquesa oscuro", "Turquesa-escuro"),
        c("cyan", 0x00FFFF, Family.TEAL, "Cyan", "Cian", "Ciano"),
        c("aquamarine", 0x7FFFD4, Family.TEAL, "Aquamarine", "Aguamarina", "Água-marinha"),
        c("pale_turquoise", 0xAFEEEE, Family.TEAL, "Pale turquoise", "Turquesa pálido", "Turquesa-claro"),
        c("light_cyan", 0xE0FFFF, Family.TEAL, "Light cyan", "Cian claro", "Ciano-claro"),

        // Blues
        c("blue", 0x0000FF, Family.BLUE, "Blue", "Azul", "Azul"),
        c("bright_blue", 0x0096FF, Family.BLUE, "Bright blue", "Azul brillante", "Azul-vivo"),
        c("royal_blue", 0x4169E1, Family.BLUE, "Royal blue", "Azul real", "Azul-royal"),
        c("navy_blue", 0x000080, Family.BLUE, "Navy blue", "Azul marino", "Azul-marinho"),
        c("midnight_blue", 0x191970, Family.BLUE, "Midnight blue", "Azul medianoche", "Azul-meia-noite"),
        c("prussian_blue", 0x003153, Family.BLUE, "Prussian blue", "Azul de Prusia", "Azul-da-prússia"),
        c("cobalt_blue", 0x0047AB, Family.BLUE, "Cobalt blue", "Azul cobalto", "Azul-cobalto"),
        c("sapphire", 0x0F52BA, Family.BLUE, "Sapphire", "Zafiro", "Safira"),
        c("denim_blue", 0x1560BD, Family.BLUE, "Denim blue", "Azul mezclilla", "Azul-jeans"),
        c("cerulean", 0x007BA7, Family.BLUE, "Cerulean", "Cerúleo", "Azul-cerúleo"),
        c("steel_blue", 0x4682B4, Family.BLUE, "Steel blue", "Azul acero", "Azul-aço"),
        c("blue_gray", 0x6699CC, Family.BLUE, "Blue-gray", "Gris azulado", "Cinza-azulado"),
        c("slate_blue", 0x6A5ACD, Family.BLUE, "Slate blue", "Azul pizarra", "Azul-ardósia"),
        c("sky_blue", 0x87CEEB, Family.BLUE, "Sky blue", "Azul cielo", "Azul-celeste"),
        c("light_blue", 0xADD8E6, Family.BLUE, "Light blue", "Azul claro", "Azul-claro"),
        c("periwinkle", 0xCCCCFF, Family.BLUE, "Periwinkle", "Azul pervinca", "Azul-pervinca"),

        // Purples
        c("purple", 0x800080, Family.PURPLE, "Purple", "Morado", "Roxo"),
        c("violet", 0x8F00FF, Family.PURPLE, "Violet", "Violeta", "Violeta"),
        c("indigo", 0x4B0082, Family.PURPLE, "Indigo", "Índigo", "Índigo"),
        c("dark_purple", 0x301934, Family.PURPLE, "Dark purple", "Morado oscuro", "Roxo-escuro"),
        c("eggplant", 0x614051, Family.PURPLE, "Eggplant", "Berenjena", "Berinjela"),
        c("plum", 0x8E4585, Family.PURPLE, "Plum", "Ciruela", "Ameixa"),
        c("grape", 0x6F2DA8, Family.PURPLE, "Grape", "Uva", "Uva"),
        c("royal_purple", 0x7851A9, Family.PURPLE, "Royal purple", "Púrpura real", "Roxo-real"),
        c("amethyst", 0x9966CC, Family.PURPLE, "Amethyst", "Amatista", "Ametista"),
        c("orchid", 0xDA70D6, Family.PURPLE, "Orchid", "Orquídea", "Orquídea"),
        c("mauve", 0xE0B0FF, Family.PURPLE, "Mauve", "Malva", "Malva"),
        c("lilac", 0xC8A2C8, Family.PURPLE, "Lilac", "Lila", "Lilás"),
        c("light_purple", 0xCBC3E3, Family.PURPLE, "Light purple", "Morado claro", "Roxo-claro"),
        c("lavender", 0xE6E6FA, Family.PURPLE, "Lavender", "Lavanda", "Lavanda"),

        // Pinks
        c("pink", 0xFFC0CB, Family.PINK, "Pink", "Rosa", "Rosa"),
        c("pale_pink", 0xFADADD, Family.PINK, "Pale pink", "Rosa pálido", "Rosa-claro"),
        c("hot_pink", 0xFF69B4, Family.PINK, "Hot pink", "Rosa intenso", "Rosa-choque"),
        c("deep_pink", 0xFF1493, Family.PINK, "Deep pink", "Rosa fuerte", "Rosa-forte"),
        c("bright_rose", 0xFF007F, Family.PINK, "Bright rose", "Rosa brillante", "Rosa-vivo"),
        c("mexican_pink", 0xE4007C, Family.PINK, "Mexican pink", "Rosa mexicano", "Rosa-mexicano"),
        c("raspberry", 0xD0104C, Family.PINK, "Raspberry", "Frambuesa", "Framboesa"),
        c("magenta", 0xFF00FF, Family.PINK, "Magenta", "Magenta", "Magenta"),
        c("mulberry", 0xC54B8C, Family.PINK, "Mulberry", "Mora", "Amora"),
        c("watermelon", 0xFC6C85, Family.PINK, "Watermelon pink", "Rosa sandía", "Rosa-melancia"),
        c("salmon", 0xFA8072, Family.PINK, "Salmon", "Salmón", "Salmão"),
        c("dusty_rose", 0xD8A1A4, Family.PINK, "Dusty rose", "Rosa viejo", "Rosa-seco"),
        c("rose_gold", 0xB76E79, Family.PINK, "Rose gold", "Oro rosa", "Ouro-rosé"),

        // Browns and neutrals with a hue
        c("brown", 0x964B00, Family.BROWN, "Brown", "Marrón", "Marrom"),
        c("dark_brown", 0x5C4033, Family.BROWN, "Dark brown", "Marrón oscuro", "Marrom-escuro"),
        c("chocolate", 0x7B3F00, Family.BROWN, "Chocolate brown", "Marrón chocolate", "Marrom-chocolate"),
        c("coffee", 0x6F4E37, Family.BROWN, "Coffee", "Café", "Café"),
        c("chestnut", 0x954535, Family.BROWN, "Chestnut", "Castaño", "Castanho"),
        c("light_brown", 0xB5651D, Family.BROWN, "Light brown", "Marrón claro", "Marrom-claro"),
        c("cinnamon", 0xD2691E, Family.BROWN, "Cinnamon", "Canela", "Canela"),
        c("caramel", 0xC68E17, Family.BROWN, "Caramel", "Caramelo", "Caramelo"),
        c("olive_brown", 0x645403, Family.BROWN, "Olive brown", "Marrón oliva", "Marrom-oliva"),
        c("camel", 0xC19A6B, Family.BROWN, "Camel", "Camello", "Camelo"),
        c("tan", 0xD2B48C, Family.BROWN, "Tan", "Tostado", "Bege-escuro"),
        c("khaki", 0xC3B091, Family.BROWN, "Khaki", "Caqui", "Cáqui"),
        c("taupe", 0x8B7765, Family.BROWN, "Taupe", "Gris topo", "Taupe"),
        c("wheat", 0xF5DEB3, Family.BROWN, "Wheat", "Trigo", "Trigo"),
        c("beige", 0xF5F5DC, Family.BROWN, "Beige", "Beige", "Bege"),

        // Grays, black and white
        c("black", 0x000000, Family.BLACK, "Black", "Negro", "Preto"),
        c("very_dark_gray", 0x2E2E2E, Family.GRAY, "Very dark gray", "Gris muy oscuro", "Cinza muito escuro"),
        c("charcoal", 0x36454F, Family.GRAY, "Charcoal", "Gris carbón", "Grafite"),
        c("dark_gray", 0x555555, Family.GRAY, "Dark gray", "Gris oscuro", "Cinza-escuro"),
        c("slate_gray", 0x708090, Family.GRAY, "Slate gray", "Gris pizarra", "Cinza-ardósia"),
        c("gray", 0x808080, Family.GRAY, "Gray", "Gris", "Cinza"),
        c("silver", 0xB4B4B4, Family.GRAY, "Silver", "Plateado", "Prata"),
        c("light_gray", 0xD9D9D9, Family.GRAY, "Light gray", "Gris claro", "Cinza-claro"),
        c("off_white", 0xF2F0EB, Family.WHITE, "Off-white", "Blanco hueso", "Branco-gelo"),
        c("white", 0xFFFFFF, Family.WHITE, "White", "Blanco", "Branco"),
    )
}
