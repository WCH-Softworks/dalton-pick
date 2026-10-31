package com.wchsoftworks.daltonpick.colorengine

/** The result of naming one sampled color. */
data class ColorMatch(
    val rgb: Int,
    val sample: Oklab,
    val entry: PaletteEntry,
    /** OKLab distance from the sample to [entry]. */
    val distance: Double,
    val tone: Tone?,
) {
    val family: Family get() = entry.family
    val hex: String get() = Srgb.toHex(rgb)
}

/**
 * Names a color by nearest neighbour in OKLab over a curated [Palette].
 *
 * Near-neutral samples (chroma below [achromaticChroma]) only match grays, black and white,
 * so a slightly tinted gray never gets a misleading hue name.
 */
class ColorNamer(
    val palette: Palette = Palette.Default,
    private val achromaticChroma: Double = ACHROMATIC_CHROMA,
) {
    fun match(rgb: Int): ColorMatch {
        val lab = Oklab.fromRgb(rgb)
        val candidates = candidatesFor(lab)
        var best = candidates[0]
        var bestDistance = lab.distanceTo(best.lab)
        for (i in 1 until candidates.size) {
            val d = lab.distanceTo(candidates[i].lab)
            if (d < bestDistance) {
                best = candidates[i]
                bestDistance = d
            }
        }
        return ColorMatch(rgb and 0xFFFFFF, lab, best, bestDistance, toneOf(lab, best.family))
    }

    internal fun candidatesFor(lab: Oklab): List<PaletteEntry> =
        if (lab.chroma < achromaticChroma) palette.achromatic else palette.entries

    internal fun toneOf(lab: Oklab, family: Family): Tone? {
        if (family.isAchromatic) return null
        val delta = lab.l - family.typicalLightness
        return when {
            delta >= LIGHT_DELTA && lab.chroma < PALE_CHROMA -> Tone.PALE
            delta >= LIGHT_DELTA -> Tone.LIGHT
            delta <= -LIGHT_DELTA && lab.chroma >= DEEP_CHROMA -> Tone.DEEP
            delta <= -LIGHT_DELTA -> Tone.DARK
            else -> null
        }
    }

    companion object {
        const val ACHROMATIC_CHROMA = 0.02
        private const val LIGHT_DELTA = 0.15
        private const val PALE_CHROMA = 0.08
        private const val DEEP_CHROMA = 0.15
    }
}

/**
 * Wraps a [ColorNamer] and keeps the current name until another palette entry is clearly
 * better (by [margin] in OKLab), so the label doesn't flicker on gradients and edges.
 */
class HysteresisNamer(
    private val namer: ColorNamer = ColorNamer(),
    private val margin: Double = DEFAULT_MARGIN,
) {
    private var current: PaletteEntry? = null

    fun match(rgb: Int): ColorMatch {
        val best = namer.match(rgb)
        val held = current
        if (held != null && held !== best.entry && held in namer.candidatesFor(best.sample)) {
            val heldDistance = best.sample.distanceTo(held.lab)
            if (heldDistance <= best.distance + margin) {
                return best.copy(entry = held, distance = heldDistance, tone = namer.toneOf(best.sample, held.family))
            }
        }
        current = best.entry
        return best
    }

    fun reset() {
        current = null
    }

    companion object {
        const val DEFAULT_MARGIN = 0.012
    }
}
