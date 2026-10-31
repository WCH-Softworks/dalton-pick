# Palette review (Palette.VERSION 1 → 2)

Review date: 2026-10-31. Scope: `colorengine/.../Palette.kt`, the golden tests, and the tone rule (SPEC §13 open question 1).

**Result in one paragraph.** Checked against the XKCD color survey, v1 had real holes: very dark blues were named "Dark purple", very dark greens and browns were named "Very dark gray", the survey's plain "blue" read as "Sapphire", plain "red" as "Crimson", and CSS orange (`#FFA500`) as "Mustard yellow". I moved 13 anchors, removed 4 entries, added 7 and renamed 1, bumped `Palette.VERSION` to 2, and added a 259-row golden table. `./gradlew :colorengine:test` is green (21 original tests plus 3 new ones). Proposed but **not applied**: the tone rule fix (§4), which needs your decision, and the Spanish and Portuguese name changes (§3), which need native-speaker review.

## 1. What I could and couldn't check

| Source | Used for | License / status | Limits |
|---|---|---|---|
| [XKCD color survey](https://blog.xkcd.com/2010/05/03/color-survey-results/): `rgb.txt`, 949 names | English "what do people call this color": each name's hex is where respondents' answers for it peak | CC0 (public domain) | Respondents were English-speaking web users on uncalibrated monitors. The file is **not** frequency-ranked, so I could not measure how common each name is. The blog's popularity table is an image. |
| CSS Color 4 named colors | Names every web developer knows | W3C spec | Some CSS names are jokes (`darkgray` is lighter than `gray`). Used only where our name matches. |
| Material Design 500/700/100 swatches, grey scale | Family-level checks on real UI colors | Published design spec | Family only. |
| [pt.wikipedia "Lista de cores"](https://pt.wikipedia.org/wiki/Lista_de_cores) (259 rows) and [es.wikipedia "Anexo:Colores"](https://es.wikipedia.org/wiki/Anexo:Colores) (405 rows) | Portuguese and Spanish names with hex values | CC BY-SA; I used the name/hex facts only | **Encyclopedic, not usage data.** Many rows are pigment names or CSS translations. The same name sometimes appears with several different hexes. |
| Academic studies: [Frontiers in Psychology 2018](https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2018.00761/pdf) (Castilian/Mexican/Uruguayan Spanish basic terms), Lillo Jover chapter, USP paper on Portuguese color names | Which Spanish/Portuguese words are basic | Open access | Cover basic terms, not our 125 specific names. |

**What does not exist.** I found no Spanish or Brazilian Portuguese equivalent of the XKCD survey, so I could **not** verify "what ordinary people call this color" in es or pt-BR with data. The es/pt checks below are (a) Wikipedia name+hex agreement and (b) my own knowledge of regional usage. Treat all es/pt findings as hypotheses for the native-speaker review that SPEC §10 already requires.

**Count.** The task said 124 names. `Palette.kt` at `c7b731c` has **123** entries (12 families are separate). After this review there are **126**.

## 2. Changes applied (Palette.VERSION 2)

Method: for each of the 949 XKCD colors I computed the engine's answer, then looked at (a) entries that capture no survey colors or the wrong ones and (b) survey colors far from every entry (OKLab distance above 0.07: 119 of 949 before the changes). I prototyped each edit in a script, kept the near-duplicate minimum (0.025) intact, then applied it to `Palette.kt`.

### Before / after on named test colors

| Color (source) | v1 said | v2 says |
|---|---|---|
| `#0343DF` "blue" (XKCD) | Sapphire | Blue |
| `#E50000` "red" (XKCD) | Crimson | Red |
| `#7E1E9C` "purple" (XKCD) | Grape | Purple |
| `#15B01A` "green" (XKCD) | Grass green | Green |
| `#01153E` "navy" | Dark purple | Dark navy blue |
| `#020035` "midnight blue" | Dark purple | Dark navy blue |
| `#000435` "dark navy" | Dark purple | Dark navy blue |
| `#062E03` "very dark green" | Very dark gray | Very dark green |
| `#002D04` "dark forest green" | Very dark gray | Very dark green |
| `#341C02` "dark brown" | Very dark gray | Very dark brown |
| `#1D0200` "very dark brown" | Dark purple | Very dark brown |
| `#650021` "maroon" | Blood red | Burgundy |
| `#AAFF32` "lime" | Bright green | Lime |
| `#C79FEF` "lavender" | Lilac | Lavender |
| `#06C2AC` "turquoise" | Dark turquoise | Turquoise |
| `#AE7181` "mauve" | Rose gold | Mauve |
| `#FFA500` CSS `orange` | Mustard yellow | Orange |
| `#BE03FD` "bright purple" | Violet | Bright purple |
| `#5D06E9` "blue violet" | Violet | Blue-violet |
| `#960056` "dark magenta" | Purple | Dark magenta |
| `#8E82FE` "periwinkle" | Amethyst | Periwinkle |

### Anchors moved

| Key | Old | New | Why |
|---|---|---|---|
| `red` | `FF0000` | `EE0000` | Survey red `E50000` was nearer to Crimson. Pure `FF0000` still reads Red. |
| `green` | `008000` | `12A01C` | Survey green was nearer to Grass green. CSS `008000` now reads "Forest green" (still in the Green family). I could not move it further without a near-duplicate with Forest green (0.020 < 0.025). |
| `blue` | `0000FF` | `0A3CE0` | Survey blue read as Sapphire. |
| `brown` | `964B00` | `884700` | Survey brown `653700` is darker than the Wikipedia brown. A compromise. |
| `purple` | `800080` | `7E1E9C` | Survey purple. CSS purple `800080` still reads Purple. |
| `orange` | `FF8000` | `FF9000` | CSS orange `FFA500` and Material orange `FF9800` both need to read Orange. Survey orange `F97306` now reads Red-orange (trade-off, see §5). |
| `turquoise` | `40E0D0` | `20D0BE` | Survey turquoise `06C2AC` and CSS `40E0D0` both read Turquoise. |
| `mint_green` | `AAF0D1` | `9FF5B2` | Survey mint is greener. |
| `periwinkle` | `CCCCFF` | `9A93F5` | Survey periwinkle is a mid blue-violet. The pale `CCCCFF` now reads as Periwinkle · pale or Light purple. |
| `lavender` | `E6E6FA` | `C8A2F0` | Survey lavender `C79FEF` is a medium-light purple. CSS lavender `E6E6FA` now reads Off-white. |
| `mauve` | `E0B0FF` | `B0768A` | Survey mauve `AE7181` is a dusty pink-brown. See the conflict note in §3. |
| `burgundy` | `800020` | `6A0A2A` | Takes over "maroon" and "burgundy" from Blood red. |
| `pink` | `FFC0CB` | `FFB6D0` | A small nudge toward the survey's hotter pink. CSS pink `FFC0CB` still reads Pink. |

### Removed

| Key | Reason |
|---|---|
| `blood_red` | Stole the survey's "maroon" and "burgundy" and is an odd name for a screen color. Burgundy now covers it. |
| `grape` | Obscure as a color name, and it stole "purple" from the Purple anchor. |
| `dark_turquoise` | CSS name only; survey turquoise is a single color. |
| `chocolate` | Near-duplicate of the moved Brown (0.009). "Chocolate brown" is a plausible name; if you want it back, move `light_brown` or `dark_brown` first. |

`lime_green` was removed in an intermediate step and **restored** (so it is not in the removed list), because the new Lime anchor left CSS `limegreen` `32CD32` named "Emerald green".

### Added

| Key | Anchor | en / es / pt-BR | Reason |
|---|---|---|---|
| `lime` | `A8F50A` | Lime / Verde limón / Verde-lima | Survey "lime", "lime green", "chartreuse", "lemon green" cluster. |
| `dark_navy` | `020640` | Dark navy blue / Azul marino oscuro / Azul-marinho-escuro | The survey's "navy", "midnight blue", "dark navy" and "dark blue" are far darker than CSS navy `000080`. |
| `very_dark_green` | `022B0C` | Very dark green / Verde muy oscuro / Verde-muito-escuro | Survey "dark forest green", "very dark green" had no entry and read gray. |
| `very_dark_brown` | `2B1608` | Very dark brown / Marrón muy oscuro / Marrom-muito-escuro | Same for dark browns. |
| `dark_magenta` | `96005A` | Dark magenta / Magenta oscuro / Magenta-escuro | Survey "dark magenta", "violet red", "deep magenta" cluster. |
| `bright_purple` | `B400F5` | Bright purple / Morado brillante / Roxo-vivo | Survey "bright purple", "hot purple", "neon purple". |
| `blue_violet` | `5A12E8` | Blue-violet / Azul violeta / Azul-violeta | Survey "blue/purple", "blue violet", "purplish blue". |

### Renamed

`neon_orange` → `red_orange` ("Red-orange" / "Naranja rojizo" / "Laranja-avermelhado"). `FF5F1F` is an orange-red, and "Neon orange" was the answer for survey orange `F97306`, which is just orange.

### Not changed because the tests or a near-duplicate forbade it

- `Beige` stays at `F5F5DC` (existing golden test). The survey's beige is `E6DAA6`, which reads Wheat · pale. Both are plausible "beige". Needs a decision from the human reviewers.
- `Mauve` / `Rose gold` are 0.028 apart: legal, but the neighborhood is crowded.

## 3. Names: obscure, missing, and per-language notes

Everything in this section is **proposal only**.

### English
- **Obscure-ish:** *Bright rose*, *Mexican pink*, *Rose gold*, *Very dark gray* are the only English names in `Palette.kt` that do not appear in the 949 XKCD names (checked loosely, ignoring a trailing "green/blue"). Presence in the survey only means a handful of respondents typed it. It is weak evidence of familiarity.
- **My judgment calls:** *Hunter green*, *Bottle green*, *Prussian blue*, *Petrol blue*, *Cerulean*, *Mulberry*, *Taupe*, *Sapphire* are legitimate but rarely said aloud. Keep them if you want the "specific name" line to teach vocabulary; drop them if you want fewer surprises. Competitor reviews (see [competitors.md](competitors.md)) show users disliking *Peru*, *Gainsboro*, *saffron*, *brass*, and *rose red*.
- **Mexican pink** is the weakest English name. The color (`E4007C`) is what the survey calls "magenta" (`C20078`) or "hot pink" (`FF028D`). Proposal: rename to *Vivid pink* (es "Rosa fucsia", pt-BR "Rosa-vivo") or merge with `deep_pink`.
- **Missing, common in the survey:** *Maroon* (covered by Burgundy now; keep an eye on it), *Gold* (reads Mustard yellow or Golden yellow), *Khaki* (survey khaki `AAA662` reads Sage green), *Sand*, *Chartreuse* (reads Lime), *Aqua* (reads Turquoise or Seafoam), *Rose* (survey `CF6275` reads Rose gold). None is a bug; they are alias candidates if the data model ever supports synonyms.

### Spanish (neutral Latin American)
- Basic terms differ by dialect. Per the Frontiers 2018 study, Castilian and Mexican Spanish have 11 basic terms and Uruguayan has 12, adding **celeste** (light blue). *Brown* and *purple* are the unstable ones: *marrón / café* and *morado / violeta*. Language-teaching sites say *café* is the usual word in Mexico and Ecuador and *marrón* elsewhere.
- **Proposals:**
  - `coffee` is named "Café", which a Mexican reader will take as plain brown. Rename to "Café oscuro" or "Café tostado".
  - Magenta: **Fucsia** is far more familiar than *magenta* in everyday Spanish and is also the CSS name for `FF00FF`. Consider "Fucsia" for `magenta`.
  - `denim_blue` "Azul mezclilla" is Mexican/Central American. The Southern Cone says *azul jean*. Fine for "neutral" if you accept the regionalism.
  - `rose_gold` "Oro rosa" and `dusty_rose` "Rosa viejo" are understood but not universal ("rosa palo" in Spain).
  - `grass_green` "Verde césped": *pasto* is more common in Latin America ("Verde pasto").
  - `taupe` "Gris topo": Spain-leaning. Consider "Gris pardo" or "Marrón grisáceo".
  - `hunter_green` "Verde cazador" is literal; very few people say it. Candidate to drop.
  - Consider a `celeste` entry (`87CEEB` area) for Río de la Plata, where *celeste* is a basic color term. It could be the es name for `sky_blue`: "Celeste" is arguably more common than "Azul cielo" in Uruguay and Argentina.
- Family line: "Verde azulado" for Teal is accurate but long; *turquesa* would be wrong (it's a separate entry).

### Brazilian Portuguese
- No Brazilian naming survey exists that I could find; a USP paper on Portuguese color-name formation is the nearest. Dictionaries and guides agree on *roxo* as the basic word (with *lilás* and *violeta* as shades), *marrom*, *rosa*, *cinza*, *bege*, and note that *bordô* and *vinho* are both used for dark reds.
- **Proposals:**
  - `lime`/`lime_green` pt-BR: "Verde-lima" and "Verde-limão" are close. I set limão on the plain lime green and lima on the yellow-green one, but a native speaker should decide.
  - `hot_pink` is "Rosa-choque" and `deep_pink` is "Rosa-forte" in the file. On pt.wikipedia, "Rosa forte" is `FF69B4` (our hot pink) and "Rosa-choque" is `FC0FC0` (more magenta). Swap or rename after native review.
  - `taupe` pt-BR is "Taupe" (borrowed). Consider "Cinza-amarronzado" or "Marrom-acinzentado".
  - Brazilians commonly say **cinza-chumbo** for a dark gray (lead gray); `charcoal` is "Grafite", which is fine. Consider adding "Chumbo" as the pt name for `dark_gray`.
  - **Dourado / prateado / cobre** (gold / silver / copper) are used as colors on products. They are metallic, which a screen cannot show, so I'd leave them out.
  - `light_orange` etc. use hyphens ("Laranja-claro"). The current spelling rules want a hyphen only for compounds of two color words; "Laranja-claro" is common in practice. Fine, but the reviewer should check consistency.

### Conflicts between sources worth a human decision
- **Mauve.** XKCD respondents call `AE7181` (dusty pink-brown) *mauve*. Wikipedia/Crayola give `E0B0FF` (pale violet). v2 follows the survey, so `E0B0FF` now reads *Lavender · light*. pt/es Wikipedia list *Malva* as `E0B0FF`; Spanish and Portuguese speakers may expect the violet. The golden table keeps the survey meaning for English, and the two Wikipedia *Malva* rows are listed in [golden-disagreements.md](golden-disagreements.md).
- **Lavender.** The same split: survey `C79FEF` vs CSS `E6E6FA`.
- **Orange.** Survey `F97306` vs CSS/Material `FFA500`/`FF9800`. I chose CSS/Material because on-screen UIs lean that way.

## 4. Tone rule: "Green · light" for `#00FF00`

### What is wrong
`toneOf` compares the sample's OKLab lightness with `Family.typicalLightness` (GREEN = 0.62). Pure green has L = 0.866, so it gets *light*. The same happens for every saturated yellow-green-cyan color, because the sRGB gamut is naturally lightest there. The label then reads "Bright green / Green · light", which tells the user nothing true: you cannot have a darker or paler pure green than that.

Measured on the 949 survey colors (v2 palette):
- **370 of 949 (39%) receive a tone.**
- **145 of those are redundant** because the name already says it ("Light green · light", "Sky blue · light", "Dark green · dark", "Pale pink · pale").
- Pure `#00FF00` and `#01FF07` ("Bright green · light"), `#00FFFF` ("Cyan · light"), `#40E0D0` ("Turquoise · light").

### Proposed fix
Two small changes, both in `ColorNamer.toneOf`:

1. **Measure lightness against the gamut, not the family.** For the sample's hue, find the lightness of the most saturated sRGB color at that hue (the gamut "cusp"). Tone *light/pale* means "lighter than the cusp by 0.15 or more". Tone *dark/deep* means "darker by 0.15 or more". Pure green, cyan and yellow are all at their cusp, so they get no tone. Navy (L 0.27 vs cusp 0.45) still reads *deep*. The cusp is a 36-entry table (one lightness per 10° of hue), computed once from the cube edges, so it stays cheap.
2. **Suppress the tone when the name already carries it.** If the matched entry's English/Spanish/Portuguese name starts with light/dark/pale/deep/very/bright (or equivalents), drop the second-line tone. This needs a per-entry flag (for example `impliesTone: Boolean`) rather than string matching, because the engine matches on keys and not on names.

Also special-case **Brown**: brown is dark orange by definition, so a "dark" tone on it is noise.

### Prototype result (JS port, `docs/research/tools/tone2.js`)

| Metric (949 survey colors) | Current rule | Proposed |
|---|---|---|
| Colors that get a tone | 370 | 244 |
| Redundant tones ("Light green · light") | 145 | 0 |

| Color | Current | Proposed |
|---|---|---|
| `#00FF00` Bright green | Green · light | Green |
| `#00FFFF` Cyan | Teal · light | Teal |
| `#40E0D0` Turquoise | Teal · light | Teal |
| `#FADADD` Pale pink | Pink · pale | Pink (the name says it) |
| `#8B0000` Dark red | Red · deep | Red (the name says it) |
| `#000080` Navy blue | Blue · deep | Blue · deep |
| `#191970` Midnight blue | Blue · dark | Blue · dark |

### Known weakness
The green hues have a high cusp (about 0.87), so ordinary mid greens look "dark" or "deep" against it: Material green `#4CAF50` came out *Green · deep* in the prototype. A fix is a wider dark threshold for hues between 100° and 200° (about 0.22 instead of 0.15) or keeping the family-relative test for *dark* only and using the cusp test for *light* only. I tried the "light-only" hybrid and it was too strict (nearly everything lost its tone: 74 of 949 kept one), so it needs tuning with real testers, which is also what SPEC §13 asks for.

### Tests that would change
`ColorNamerTest.tones follow lightness` asserts `Tone.PALE` for `FADADD` and `Tone.DEEP` for `000080`. Under the proposal `FADADD` returns `null` because "Pale pink" already says so. Update that test, and add `00FF00`, `00FFFF`, `FFFF00` and `4CAF50` assertions.

### Decision needed
Do you want me to implement this in `ColorNamer.kt`? I did **not**, because it changes label behavior and you asked for a proposal. The cleanest alternative, if you want something smaller, is only change 2 (suppress redundant tones): it removes 145 redundant tones but leaves `Bright green · light`.

## 5. Golden test table

`colorengine/src/test/.../GoldenTable.kt` is generated by `docs/research/tools/gen.js` and checked by `GoldenTableTest`.

| Source | Rows |
|---|---|
| XKCD survey names with a clear equivalent | 84 |
| CSS Color 4 named colors with a matching name | 59 |
| pt.wikipedia exact name matches | 14 |
| es.wikipedia exact name matches | 16 |
| Material grey scale / dark-theme neutrals | 15 |
| **Name rows** | **188** |
| Material 500/700/100 swatches (family) | 27 |
| pt/es Wikipedia "basic color word [light/dark]" names (family) | 44 |
| **Family rows** | **71** |
| Original hand-written goldens (`ColorNamerTest`) | 20 |
| **Total assertions** | **279** |

That is about 280 of the ~300 target. Every row carries its source string. Rows list accepted key sets (for example "dark green" accepts `dark_green` and `very_dark_green`), because ordinary usage allows more than one answer.

**What was left out.** Rows where the engine disagrees with a source are kept out of the test and listed in [golden-disagreements.md](golden-disagreements.md): 78 name rows and 5 family rows. I reviewed the largest; most are cases where the source itself is ambiguous (Wikipedia's *Morado* spans `572364` to `8D0036`). The ones worth a second look:
- Survey orange `F97306` → Red-orange (traded for CSS/Material orange).
- CSS `lavender` `E6E6FA` → Off-white; pt/es *Malva* `E0B0FF`/`DF80FF` → Lavender/Orchid.
- es *Café* `591F0B` → Burgundy: very dark reddish browns are the weakest region. Consider a "Reddish brown" / *Marrón rojizo* entry.
- es *Verde esmeralda* `00554E` → Petrol blue (a very dark teal-green): the teal/emerald boundary.
- `#FF69B4` is *Rosa forte* on pt.wikipedia but our hot pink: see §3.

Tests only pin v2. If you move an anchor later, regenerate the table (`gen.js`) and review the diff.

## 6. Verification

- `./gradlew :colorengine:test` → green (21 original + 3 in `GoldenTableTest`).
- Near-duplicate test intact at 0.025. Closest pairs in v2: `tan`/`khaki` 0.027, `tomato_red`/`red_orange` 0.027, `beige`/`off_white` 0.028, `mauve`/`rose_gold` 0.028.
- `Palette.VERSION = 2`.
- Not done: OKLab vs CIEDE2000 comparison, testing with colorblind reviewers, native-speaker review, and anything in `app/`.

## 7. Open decisions for you

1. Tone rule: implement the cusp-based fix (§4), or the smaller redundant-tone suppression only, or leave it?
2. Orange: survey orange (`F97306`) or CSS/Material orange (`FFA500`/`FF9800`)? I chose the latter.
3. Do you want an es-MX override for brown ("Café") and a Río de la Plata *celeste*, or stay with one neutral Latin American set?
4. Keep or drop the rarely-said names (*Hunter green*, *Bottle green*, *Prussian blue*, *Taupe*, *Mexican pink*)?
