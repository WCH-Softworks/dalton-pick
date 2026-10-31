# Competitor scan

Scan date: 2026-10-31. Scope: apps that help colorblind people name or tell apart colors, with a focus on Android and on anything that reads colors from the screen.

## How this was gathered, and what to trust

- **Google Play pages** (rating, rating count, install band, "Contains ads", "In-app purchases", and the handful of reviews Play embeds in each listing) were fetched directly from `play.google.com` on 2026-10-31, US storefront. Numbers are as shown that day. A `?` means my parser did not find the value, not that it is zero.
- Play only embeds about 10–20 reviews per listing, picked by Play's own "most relevant" ordering. The complaint themes below come from those, not from a full review export. Treat them as **representative, not exhaustive**.
- Review text is paraphrased, not copied.
- App Store / website facts come from web search results and the apps' own pages. Where sources disagreed I say so.
- Nothing here was tested on a device. I did not install any of these apps.

## Summary table

| App | Platform | How it gets colors | Price / ads | Rating (Play) |
|---|---|---|---|---|
| **Color Blind Pal** | Android + iOS | Live camera, photo from gallery, freeze frame. Also a "shift" filter and simulations. | Free. Play shows in-app purchases, no ads flag. | 3.40 (839) · 100K+ |
| **Daltonic Color Finder** | iOS only (no Android listing found) | Camera ("colors around you"), saved palette | $0.99 on the App Store (SMED SRL, v1.0 Nov 2024) | n/a (iOS) |
| **DaltonLens** | Desktop: macOS, Windows (Linux unconfirmed) | Screen under the pointer, global hotkey | Free, open source (BSD-2) | n/a |
| **Color Grab (color detection)** | Android | Live camera; image from gallery | Free with ads; ad-free upgrade (IAP) | 4.46 (16,368) · 1M+ |
| **Google Color correction** | Android system feature | Re-colors the whole display; does not read or name colors | Free, built in, no ads | n/a |
| **Pixolor – Live Color Picker** | Android | **Screen**: floating zoom circle, one screenshot per touch (MediaProjection) | Free with ads + IAP | 4.14 (1,779) · 100K+ |
| **ColorBlindClick Picker** | Android | Live camera, tap to name | Free, no ads, no IAP | 3.26 (118) · 10K+ |
| **Daltonic Pointer – color name** | Android | Pointer over an image/camera, matches ~5,000 names | Free (listing says 5K+ installs; ad data not parsed) | not parsed |
| **ColorADD** | Android + iOS | Camera; shows name + its own symbol code | Free, no ads shown | 3.13 (86) · 10K+ |
| **Color Identification** (`colorrecognizer.com`) | Android | Camera / photo | Ads + IAP | 3.91 (10,519) · 1M+ |
| **Color Identifier Color Finder** | Android | Camera / photo | Ads + IAP | 4.66 (1,840) · 50K+ |
| **Chromatic Vision Simulator** | Android | Camera filter (simulates CVD for people with normal vision) | IAP | 4.32 (1,307) · 500K+ |

### Notes per app

**Color Blind Pal.** The most direct "colorblind aid" competitor on Android. It names colors from the live camera, supports freeze and photos, and adds an optional "daltonize"-style shift. Last visible update info is inconsistent across aggregators (one lists 2020 for iOS, another October 2024), so treat "abandoned" as unproven. It reads the *camera*, not the screen.

**Daltonic Color Finder.** An iOS-only paid app from late 2024 (Italian developer). I found no Android listing. Included because the name is close to ours, which matters for the name check (see [name-check.md](name-check.md)).

**DaltonLens.** The nearest relative in spirit: a tray utility with a hotkey that shows the color under the pointer, with simulations and a "highlight similar colors" tool for charts. Desktop only. Source: [github.com/DaltonLens/DaltonLens](https://github.com/DaltonLens/DaltonLens). Open source under BSD-2, so its ideas are free to learn from; nothing is shared with us at code level.

**Color Grab.** The big player for general color picking (1M+ installs, 4.46★). It targets designers and painters (Munsell, paint-brand matching, palettes), not colorblind users. It is ad-supported and sells an ad-free version. It reads the camera and gallery images, not other apps' screens.

**Google Color correction.** Settings → Accessibility → Color and motion → *Color correction*, with deuteranomaly, protanomaly and tritanomaly modes and a grayscale toggle; Android 15 may add an intensity slider (reported on a beta, unverified). It shifts the colors of everything on the display. It does **not** tell you what a color is called. SPEC §11 already scopes this out and suggests linking to it. Sources: [AbilityNet Android 16 guide](https://mcmw.abilitynet.org.uk/how-to-use-colour-correction-to-adapt-for-colour-blindness-in-android-16), [Android Authority on the slider](https://www.androidauthority.com/android-15-color-correction-slider-3458931).

**Pixolor.** The only Android app found that matches DaltonPick's *mechanism* (a floating loupe over other apps, sampling the screen). It shows hex/RGB values and coordinates, **not friendly names**, and it is not aimed at colorblind users. It needs the screen-capture consent dialog and shows ads.

**ColorBlindClick Picker.** Small indie app by one developer, free, no ads, and the most colorblind-focused of the camera apps. Reviews are polarized: some users call it life-changing (charging LED red or green), others say it reports gray for most things.

## What users complain about

These themes recur in the reviews Play embeds for the listings above.

### 1. Names are too fancy or too many
Every naming app gets this. Users with CVD asked for "just red, pink, dark red" and said they had never heard of names like "Peru", "Gainsboro", "burly wood", "saffron", "brass", or "rose red". One ColorBlindClick reviewer wanted fewer color options; a Color Blind Pal reviewer asked for an option to show simple descriptions only. One reviewer also said a few names would be less ambiguous with a familiar color word appended ("heavenly" → "heavenly blue").
→ **DaltonPick answer:** the two-line label (specific name + basic family) is exactly this, and the palette review (see [palette-review.md](palette-review.md)) removes names ordinary people do not use.

### 2. The finger hides the crosshair
Several Color Blind Pal reviewers asked for the crosshair to be offset from the finger, a handle on the cursor, a zoom for small things, and a visible sample-size box. They said the app is "awkward to use" for small targets such as wires and resistors.
→ **DaltonPick answer:** round lens with the sample at its center and a precision/fine-adjust drag mode (SPEC §4.2).

### 3. Camera accuracy
Reviews of camera-based apps say names are wrong under indoor light: everything "dull orange", black as blue, white as "gray 30%", purple fabric as brown. A user buying for a child with achromatopsia returned to the store disappointed. Color Grab users complain that its white-reference calibration says "too dark" even in sunlight.
→ **DaltonPick answer:** the screen path has no lighting error. Pixels are exact. The camera mode (SPEC §4.4) inherits these problems, so the "lighting affects colors" hint and a later white-balance option matter.

### 4. Trust and permissions (the one that bites overlay apps)
Pixolor's reviews show how users react to a screen-reading permission. One long-time user accused it of spyware-like behavior for asking for screen capture. Another objected to the app asking to turn off battery optimization. The developer answered on the listing that capture is local and that Android now requires it. A third complained about pop-up ads appearing outside the app.
→ **DaltonPick answer:** state "no internet permission, nothing leaves your phone, open source" prominently in the listing, the onboarding and the permission rationale screens. This is a real differentiator, because the claim is checkable (manifest, source).

### 5. Ads and monetization friction
Color Grab: the paid ad-free version still shows rate/share pop-ups and adds a watermark and a pre-typed "sent from" line to exports, according to one reviewer. Pixolor: ads at the end of a session and on the settings screen; one user reported ads on the home screen. Color Identification and Color Identifier are ad-supported with IAP.
→ **DaltonPick answer:** no ads, no IAP, a single Ko-fi link.

### 6. Stability and polish
Pixolor: the overlay turns transparent after screen rotation; the notification buttons are unstable; the clipboard "color saved" toast is unreliable. Color Blind Pal: crashes and stale updates come up in aggregator summaries (AI-generated, low reliability).
→ **DaltonPick answer:** rotation and multi-window handling are explicit M2 tasks; the device matrix is M5.

### 7. Missing "pick from my gallery / screenshots"
Color Blind Pal and ColorBlindClick reviewers asked to check colors in saved pictures and in screenshots people send them.
→ **DaltonPick answer:** the screen lens covers this for any image shown on screen. Worth saying in the listing: "works on photos in your gallery, chats and websites".

## Who actually serves "name any color on my screen, for a colorblind person"?

Nobody that I found on Android, as of this scan:

| Need | Camera naming apps | Pixolor-style pickers | Color correction | Desktop DaltonLens | **DaltonPick** |
|---|---|---|---|---|---|
| Names colors in friendly language | yes (too fancy) | no (hex only) | no | partly | **yes, two-tier** |
| Works on *other apps'* screens | no | **yes** | n/a | n/a (desktop) | **yes** |
| Aimed at CVD users | partly | no | yes | yes | **yes** |
| No ads | mixed | no | yes | yes | **yes** |
| Needs no internet | mixed (one reviewer says ColorBlindClick uses the internet) | yes | yes | yes | **yes, no permission** |
| Spanish / Brazilian Portuguese names | not seen | n/a | n/a | n/a | **yes, hand-written** |

I did not verify language coverage of competitor name lists. "Not seen" means I saw no evidence in listings or reviews.

## Gaps DaltonPick fills

1. **Naming for screens, not only cameras.** Charts, maps, status badges, game UIs, shop apps and chat images are screen content. Camera apps cannot read them well (moiré, reflections, a second device in the way), and pickers only give hex.
2. **Friendly, two-tier names written by hand in en, es and pt-BR.** The main complaint across the category is names people do not use. Local-language names are a real gap, particularly for Latin America and Brazil, where CVD users are numerous (about 8% of men).
3. **A trust story that can be checked.** No `INTERNET` permission, no ads, no analytics, open source. The category has visible permission anxiety and ad annoyance.
4. **A lens that fits a thumb.** Fine-adjust mode and an offset label answer the "finger hides the crosshair" review theme.
5. **A free, no-strings tipping model** rather than ad-free upgrades with pop-ups.

## Store keywords to target

**Caveat:** I cannot see Play Console search-volume data, so these are hypotheses. The Play search pages for "color blind", "colorblind color name", "daltonismo", "daltónico" and "identify color name" were fetched on 2026-10-31. They returned dozens of tests and simulators, which shows the "color blind" space is crowded with *test* apps (Ishihara). The naming/aid space is much thinner. Validate with the Play Console search-terms report after the first closed test.

Play title is capped at 30 characters; short description at 80; the full description is indexed, with no separate keyword field.

| Language | Primary phrases | Secondary phrases |
|---|---|---|
| en | color blind, colorblind, color name, color identifier, what color is this, screen color picker | color finder, name that color, CVD, deuteranopia, protanopia, tritanopia, color blindness aid, accessibility |
| es (LatAm) | daltonismo, daltónico, nombre del color, identificar colores, qué color es | ciego a los colores, ayuda para daltónicos, color en pantalla, accesibilidad |
| pt-BR | daltonismo, daltônico, nome da cor, identificar cores, que cor é essa | cegueira para cores, ajuda para daltônicos, cor na tela, acessibilidade |

Notes:
- Spelling differs by market. Brazil uses **daltônico**, Latin America **daltónico**; write both in the respective locale.
- Avoid "test" and "Ishihara" in the listing: they attract the wrong audience and a wrong-intent install lowers conversion and rating.
- Suggested title shapes: `DaltonPick: Color Name Lens` (27 chars, en), `DaltonPick: nombre del color`, `DaltonPick: nome da cor`. See `docs/store/`.
- Do not use competitor names as keywords. Play rejects keyword stuffing and misleading metadata.

## Caveats and follow-ups

- Run a proper review export (for example the Play Console "Reviews" API on our own app later) rather than relying on embedded samples for competitors.
- Re-check Color Blind Pal's current Android update date and whether its camera naming uses the network. One ColorBlindClick reviewer claims ColorBlindClick needs internet; I did not verify it.
- Pixolor's developer (Hanping) is the best case study for handling the screen-capture permission copy. Their FAQ is linked from their replies.

## Sources

- Google Play listings fetched 2026-10-31: [Color Blind Pal](https://play.google.com/store/apps/details?id=com.colorblindpal.colorblindpal), [Color Grab](https://play.google.com/store/apps/details?id=com.loomatix.colorgrab), [Pixolor](https://play.google.com/store/apps/details?id=com.embermitre.pixolor.app), [ColorBlindClick Picker](https://play.google.com/store/apps/details?id=com.lukelorusso.colorblindclick), and the others in the table by package id.
- [Daltonic Color Finder, App Store](https://apps.apple.com/app/id6737409303)
- [DaltonLens on GitHub](https://github.com/DaltonLens/DaltonLens) and its [Mac App Store page](https://apps.apple.com/au/app/daltonlens/id1222737651?mt=12)
- [Pixolor feature coverage (electronic.partners)](https://electronic.partners/news/new-app-pixolor-is-the-live-eyedropper-tool-ive-always-wanted-for-android/)
- [AbilityNet: color correction on Android 16](https://mcmw.abilitynet.org.uk/how-to-use-colour-correction-to-adapt-for-colour-blindness-in-android-16)
- [ColorADD app](https://coloradd.net/en/coloradd-app)
