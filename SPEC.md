# DaltonPick — Specification

> Status: Draft v0.4 · 2026-10-31. Scope is the lens, plus a camera mode. No ads and no in-app purchases; support comes through a Ko-fi link. Open source (MIT). Languages: en, es, pt-BR.

## 1. Purpose

DaltonPick helps colorblind people identify colors **anywhere on their phone screen**. It stays out of the way until the user pulls it into view. Then it shows a round magnifying lens with a small crosshair and a friendly name for the color under the crosshair ("Olive green", "Salmon pink").

The app is **free, with no ads, no purchases and no account**. A Ko-fi link in the main app lets people support development if they want to.

## 2. Target users

- People with color vision deficiency (protan, deutan, tritan, achromat), about 8% of men and 0.5% of women.
- Typical uses: telling apart chart lines and map legends, checking clothing colors in a shop app, reading status indicators, game UIs, and color pickers.

## 3. Platform decision

| Platform | Feasibility | v1 scope |
|---|---|---|
| **Android** | Full. Apps can draw overlays on top of other apps (`TYPE_APPLICATION_OVERLAY`) and read screen pixels (AccessibilityService screenshot or MediaProjection). | **Yes, primary target** |
| **iOS** | Apps **cannot** draw over other apps or sample another app's pixels live. | Not in v1. A later in-app mode could cover it: pick a screenshot or photo, or use the live camera, with the same lens UI. |

**v1 is native Android (Kotlin).** The color engine is written as pure Kotlin with no Android dependencies so it can be reused later (Kotlin Multiplatform for iOS).

## 4. User experience

### 4.1 Idle state: "hidden away"
- A small, semi-transparent **edge tab** (about 8×48 dp) docked to the left or right edge of the screen, floating over all apps.
- The user can drag it along the edge to change its vertical position, and can snap it to the other edge. The position is remembered.
- Only a pull opens the lens (§4.2). A tap doesn't, because tapping was too easy to do by accident (found in testing on a phone). Instead it nudges the tab inward and back, to show that it pulls, and for the first three taps a small bubble beside the tab says "Pull the tab toward the middle to open the lens." The tab carries three grip dots so it reads as a handle, not a scrollbar, and it steps aside while camera mode is on screen. A tap with the lens open puts it away, and TalkBack's double-tap opens it.
- Optional auto-dim to about 30% opacity after 3 s without interaction.
- A Quick Settings tile and a persistent notification can show or hide the tab entirely.
- With gesture navigation, a swipe from the screen edge is the system **back gesture**. The tab therefore excludes its own area from system gestures with `setSystemGestureExclusionRects`. Android allows up to 200 dp per edge; the tab is 56 dp. This was found in M0.

### 4.2 Active state: the lens
- **Pull** the tab inward (a drag past about 24 dp) to bring the lens out under the finger. Releasing the drag leaves it in place.
- The lens is a **round lens with no handle**, 140 dp across by default (adjustable 100–200 dp), with a thin ring outline that stays visible on any background (dark and light ring).
- **Magnification** of 3× by default (2×–8×), with nearest-neighbour scaling so individual pixels stay crisp.
- **Crosshair**: a small, low-key cross with a gap in the center, so the sampled pixel itself is not covered. It is drawn in two contrasting tones (white with a dark outline).
- **Name label**: a pill below the lens (or above it when near the bottom edge) with:
  - The **friendly name**, large (≥18 sp, bold), for example "Dusty rose".
  - A **hue family** line, smaller, for example "Pink · light".
  - Optional (setting): hex `#D8A1A4` and RGB values.
  - A small swatch of the sampled color.
- **Move**: drag the lens anywhere. The sample point is the center of the lens.
- **Precision**: a fine-adjust mode that moves the lens at a fraction of finger speed, so the finger doesn't hide the target. It turns on after a long press, or after dragging from outside the lens.
- **Dismiss**: tap the ✕ at the end of the label, drag the lens back onto its edge and let go (it fades while over the drop zone), fling it toward that edge, or tap the tab. It returns to idle. TalkBack offers "Close lens" as an action. *(The ✕ and the drop zone were added after testing: fling-only dismissal wasn't discoverable.)*
- **Freeze/refresh**: see §5.2. A tap on the label (or a small refresh dot) takes a new snapshot of the screen.

### 4.3 Feedback
- A light haptic tick when the friendly name changes.
- With TalkBack on, the name is announced (with debounce) when the lens settles.
- Hysteresis on the name: the label changes only when the new color is clearly closer to another name, which prevents flicker on gradients and anti-aliased edges.

### 4.4 Camera mode
- A button in the main app (and an app shortcut) opens the camera full screen, with the same lens, crosshair and label, for real-world objects such as clothes, paint and fruit.
- The camera feed is live, so no snapshot is needed. A "freeze" button holds the frame, and a tap on the label speaks the name.
- Uses the `CAMERA` permission, requested only when camera mode is first opened. Frames are processed in memory and never saved.
- Lighting changes how colors look. The label adds a small "lighting affects colors" hint the first few times.
- White-balance caveat: a phone camera auto-corrects white balance, so names describe what the camera sees. A later version could add a "tap something white to calibrate" option.

### 4.5 Main app (launcher activity)
- Onboarding: what the app does → grant permissions (overlay, screen access) → a practice screen with color swatches.
- Settings: lens size, zoom, which edge, auto-dim, show hex/RGB, haptics, language. There is no Simple/Detailed name toggle, because the label always shows both: the specific name ("Navy blue") and the basic family ("Blue · deep").
- Language: follows the system language, with a per-app override (Android 13+ per-app language setting) for en, es and pt-BR.
- "Test chart" screen with sample swatches, including common confusion pairs (red/green, blue/purple). It is useful for practice and for QA.
- **About** screen: version, privacy policy, open-source licenses, and a **"Support on Ko-fi ☕"** button (§7).

## 5. Technical design (Android)

### 5.1 Stack
- Kotlin, minSdk **30** (Android 11), target the latest SDK.
- Main app UI: Jetpack Compose + Material 3.
- Overlay: a custom `View` with `Canvas` drawing for the lens (low latency, no Compose lifecycle work inside the overlay window), hosted by a foreground `Service` through `WindowManager`.
- Pure-Kotlin module `:colorengine` for color naming, with JVM unit tests.
- Settings in DataStore (Preferences).
- **No third-party SDKs** in the release build (no ads, billing or analytics).

### 5.2 Screen capture strategy

**Problem:** the lens is itself part of the screen. If the app magnified a live screen stream, the stream would include the lens and it would magnify itself.

**Solution: snapshot-on-pull.**
1. When the user starts pulling the tab, the overlay hides the lens for one frame. The edge tab is drawn away from the capture area.
2. Capture **one full-screen bitmap**.
3. Show the lens. It magnifies and samples from the **frozen snapshot**, not from the live screen.
4. The lens stays open after the finger lifts, so the app underneath **can** scroll or change screen while it is open (found in testing). The lens then takes a new snapshot once things settle: 250 ms after the last scroll or window-change event from the accessibility service, or 700 ms after a touch outside the lens (FLAG_WATCH_OUTSIDE_TOUCH), which also covers apps that send no scroll events and the screen-recording fallback. Each refresh hides the lens for a moment, so it blinks once. Refreshes wait while a finger is on the lens. Tapping the label still refreshes by hand.
   - **Following the scroll before the refresh.** Scroll events from View-based lists (`ScrollView`, `RecyclerView`) carry the distance scrolled (`scrollDeltaY`). The lens adds it up and redraws the scrolling part of the screen shifted by it, gliding between events (about 115 ms apart), so it moves with the list, about one event behind. Only the event's type and distance are used; the service still can't read window content.
   - **Which rows scroll.** Events don't say where the scrolling list is (no window content). When a refresh snapshot arrives, it is lined up with the previous one off the main thread (`ScrollRegistration`: one hash per pixel row, weighted voting for the vertical offset, then a check that at least 70% of the changed rows agree). That gives the offset and the **band** of rows that moved; rows that stayed put are fixed bars (toolbar, search bar) and always come from the latest snapshot. The reported distance only breaks ties between look-alike offsets (repeating rows); the images win otherwise. A weak or ambiguous match means "unrelated" and stitching starts over. A lens whose centre is on a fixed bar does not shift. Before the first band is known, the first scroll assumes everything between the system bars scrolls.
   - **Stitched content.** Each lined-up snapshot's band is copied into a ring buffer in content coordinates, up to **3 band heights** (about 37 MB at 1280 × 2412 rows; the far end is dropped first), so scrolling back over content already seen shows the right pixels at once. Content not seen yet draws as the lens's empty fill and the label shows "…" until the refresh. The buffer is cleared on a window change in another app, a sideways scroll, a failed registration, rotation, and when the lens closes. Snapshots and buffer stay in memory only.
   - No haptic tick or TalkBack announcement while the lens follows a scroll; the name is announced once after the refresh.
   - **Limits.** Compose lists report no distance (`-1`); their `scrollY` is an estimate (2.1–2.2× the real distance in Settings > Apps > See all apps), so the lens doesn't move during a Compose scroll and just refreshes after it. Compose snapshots also rarely line up exactly (sub-pixel text), so they don't stitch. Collapsing toolbars move the list without scroll events, so the lens shows less movement than the list until the refresh. Snapshots taken during the stretch overscroll at a list's end don't line up, so stitching starts over. Without the scrolling list's bounds, a scroll event from a second list on screen shifts the band of the first until the refresh. The screen-recording fallback gets no scroll events: its refresh snapshots are still lined up and stitched, but the lens only updates on refresh.

**Capture backends** (chosen by a spike, M0):

| | A. AccessibilityService `takeScreenshot()` (API 30+) | B. MediaProjection |
|---|---|---|
| Consent | Enabled once in Accessibility settings | System dialog **every session** (Android 14+ requires new consent for each capture session) |
| Indicators | None | Screen-cast status indicator |
| Rate limit | About 1 shot per 333 ms (fine for snapshots) | Continuous |
| Play policy | Must declare `isAccessibilityTool="true"` and pass Accessibility policy review. A colorblind aid is a legitimate accessibility tool. | Standard foreground-service declaration (`mediaProjection` type) |
| UX | Much better | Annoying for something used many times a day |

**Preferred: A, with B as fallback.** If Play review rejects A, we appeal first and switch to B only if the appeal fails. Both sit behind a `ScreenCapturer` interface.

**M0 emulator results (Pixel 9 Pro, Android 16):**

| | A. Accessibility | B. MediaProjection |
|---|---|---|
| Capture latency | 55–58 ms | 22 ms |
| Color space | sRGB | sRGB |
| Overlay hidden from the snapshot | Yes | Yes |
| Consent | One-time switch in settings | Dialog each session, which **defaults to "Share one app"**, so the user must switch to "entire screen" every time |
| Indicator | None | Red recording chip in the status bar |

These results confirm A as the better experience. Both still need testing on real phones; see [docs/M0-TESTING.md](docs/M0-TESTING.md).

- Protected content (`FLAG_SECURE` windows such as banking apps and DRM video) comes back black. Detect an all-black region and show "This app hides its content."
- Hardware bitmaps must be copied to `ARGB_8888` before reading pixels.
- Color space: convert the capture to sRGB if the screenshot carries a wide-gamut color space (Display P3).

### 5.3 Sampling
- Sample the center pixel, plus a **3×3 average** (setting: 1×1 / 3×3 / 5×5). The average reduces noise from dithering and subpixel anti-aliasing.
- Coordinates: lens center in screen pixels → snapshot pixels. Account for display cutouts, rotation and system-bar insets.

### 5.4 Color naming engine (`:colorengine`)
1. sRGB (8-bit) → linear RGB → **OKLab** (a perceptually uniform space that is cheap to compute).
2. **Nearest-neighbour** search over a curated palette, using ΔE in OKLab. CIEDE2000 is an alternative if tests show OKLab mislabels some colors.
3. Two tiers:
   - **Family** (about 12): Red, Orange, Yellow, Green, Teal, Blue, Purple, Pink, Brown, Gray, Black, White, plus a lightness modifier (light / dark / pale / deep).
   - **Friendly name** (about 150 curated, common-language names, avoiding obscure ones like "Gamboge"), for example "Navy blue", "Mint green", "Mustard yellow", "Burgundy".
4. Achromatic handling: when OKLab chroma falls below a threshold, return gray, black or white with a lightness level, so the result is never a misleading hue name.
5. Hysteresis: keep the current name unless the new best match is closer by margin `m`.
6. The palette lives in a versioned Kotlin table (`Palette.VERSION`). Each entry has a stable key and hand-written names in en, es and pt-BR. Names are kept in the engine, not in Android resources, so a future iOS app can reuse them.
7. Target: lookup in under 0.2 ms on mid-range devices. A linear scan over 150 entries is enough.

### 5.5 Permissions and services
- `SYSTEM_ALERT_WINDOW` (Display over other apps), granted through the settings intent.
- `BIND_ACCESSIBILITY_SERVICE` (backend A), or `FOREGROUND_SERVICE_MEDIA_PROJECTION` (backend B).
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` for the overlay service, with a persistent notification that has Hide / Settings actions.
- `POST_NOTIFICATIONS` (Android 13+).
- `RECEIVE_BOOT_COMPLETED` (optional: restore the tab after reboot).
- `CAMERA` (camera mode only, requested on first use).
- **No `INTERNET` permission.** The Ko-fi link opens in the user's browser through an `ACTION_VIEW` intent, so the app itself never goes online.

## 6. Accessibility requirements
- Every label has at least 4.5:1 contrast. The label background adapts and never uses color alone to carry meaning.
- Full TalkBack support in the main app. The lens announces the name.
- Respect system font scale on the label, up to 200%.
- Dark and light theme.
- The app's own UI must be usable by every CVD type. Run it through a CVD simulator during QA.

## 7. Support (Ko-fi)
- A "Support on Ko-fi ☕" button on the About screen, plus one low-key line at the end of onboarding ("DaltonPick is free and ad-free. If it helps you, you can buy me a coffee."). Popups, nag screens and reminders are not used.
- The button opens the Ko-fi page in the external browser. The URL is set in one constant.
- Nothing is unlocked by donating. Every feature is free for everyone.
- Ko-fi page: https://ko-fi.com/willcaphir
- **Policy check (M0, done):** Google's [Payments policy FAQ](https://support.google.com/googleplay/android-developer/answer/10281818) treats a direct tip from user to creator as a peer-to-peer payment, which doesn't need Play Billing. Two conditions apply: **100% of the tip goes to the creator**, and **the tip gives no access to digital content or services**. Ko-fi takes no platform fee on one-off donations (payment processor fees aside). So we must:
  - never unlock anything in the app for supporters, and
  - link to a plain donation page, not to Ko-fi memberships or shop items with perks.

  Google has been inconsistent in enforcing this in the past. If the listing is ever flagged, the fallback is to move the link to the store listing and website only.

## 8. Privacy
- Screenshots are processed **in memory only** and are never saved, logged or uploaded. The bitmap is released when the lens closes.
- No network access (no `INTERNET` permission), no account, no analytics, no ads.
- Play Data Safety form: **no data collected, no data shared.**
- A privacy policy URL is still required because the app uses an accessibility service and screen content. A short page states the above.

## 9. Performance and battery
- Idle: the tab is a tiny view. No capture and no wake locks happen while idle. The foreground service is required by the system but does no work.
- Active: one capture per pull or refresh. Lens redraws on drag at 60 fps (just a scaled bitmap blit plus the color lookup).
- Memory: one full-screen ARGB bitmap (about 10–15 MB at 1080×2400), released when the lens is dismissed.

## 10. Localization
- v1: English, Spanish (neutral Latin American: "Durazno", "Azul mezclilla") and Brazilian Portuguese. Color names are written by hand per language, not machine-translated word by word (for example "Verde-oliva", "Rosa-seco"). Any other system language falls back to English.
- App UI strings use Android resources (`values/`, `values-es/`, `values-pt-rBR/`). Color names, families and tones live in `:colorengine` (§5.4).
- A native speaker of each language should review the color names before release.

## 11. Out of scope for v1
- iOS. A later iOS app would be built around the camera mode, since iOS has no overlays.
- Screen recoloring and color filters. Android's built-in Accessibility → Color correction already covers live recoloring. The app could link to it from the About screen.
- A color vision test.
- Saving or sharing picked colors, history.

## 12. Decisions (resolved 2026-10-31)
1. **Name:** "DaltonPick". A web search found no existing app with this name. The closest is [DaltonLens](https://github.com/DaltonLens/DaltonLens), an open-source desktop colorblind tool. The names are similar but the products are clearly distinct. A formal trademark search (USPTO, INPI Brazil, EUIPO) has **not** been done yet; do it before release.
2. **Simple/Detailed names:** no toggle. The label shows both the specific name and the basic family (§4.5).
3. **Camera mode:** in v1 (§4.4).
4. **Play review fallback:** appeal first, then MediaProjection (§5.2).
5. **Ko-fi:** https://ko-fi.com/willcaphir (§7).
6. **Open source:** MIT, in the WCH-Softworks GitHub organization next to Incudo.

## 13. Open questions
1. Should the palette tone rules be tuned? For example, pure #00FF00 currently reads "Green · light", because saturated green is naturally very light. Decide during palette review with colorblind testers.
   - **Proposed fix (not implemented):** in `ColorNamer.toneOf`, (a) measure lightness against the sRGB gamut's most saturated color at the sample's hue, not against `Family.typicalLightness`, so pure green, cyan and yellow get no tone; (b) drop the tone when the matched name already says it ("Light green", "Dark red"), using a per-entry flag; (c) never show "dark" for Brown. On the 949 XKCD survey colors this cuts colors with a tone from 370 to 244 and redundant tones from 145 to 0.
   - **Known weakness:** green hues have a high gamut lightness, so ordinary mid greens (for example Material green `#4CAF50`) read "deep" until the dark threshold is tuned for hues 100°–200°. Tune with colorblind testers.
   - **Tests to update if adopted:** `ColorNamerTest.tones follow lightness` (`#FADADD` would have no tone) and add `#00FF00`, `#00FFFF`, `#FFFF00`, `#4CAF50`.
   - Details, prototype numbers and the smaller alternative (suppress redundant tones only) are in [docs/research/palette-review.md](docs/research/palette-review.md) §4.
