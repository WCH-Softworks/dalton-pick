# DaltonPick — Implementation Plan

See [SPEC.md](SPEC.md) for requirements. The plan is ordered so the **riskiest unknowns are tested first** (screen capture and Play policy) before any polish.

## Project layout

```
DaltonPick/
├─ app/                  Android app (Compose UI, overlay service, capture)
│  └─ src/main/java/.../
│     ├─ ui/             onboarding, settings, test chart, about (Compose)
│     ├─ overlay/        OverlayService, EdgeTabView, LensView, gesture handling
│     ├─ capture/        ScreenCapturer interface, A11yCapturer, ProjectionCapturer
│     └─ settings/       DataStore-backed SettingsRepository
├─ colorengine/          pure Kotlin/JVM: OKLab conversion, palette (en/es/pt-BR names), namer, hysteresis
└─ docs/                 store listing, privacy policy, policy declarations
```

Tooling: Android Studio (latest stable), Gradle Kotlin DSL + version catalog, JDK 21 (already installed), ktlint/detekt, GitHub Actions CI (build + unit tests).

---

## M0 — Spikes / de-risking (≈ 3–4 days) — *in progress*

Goal: prove the core mechanism works on real devices before building anything else. The test build is in `app/` (plain Android views, no libraries). How to run it on a phone is in [docs/M0-TESTING.md](docs/M0-TESTING.md).

- [x] **S1 Overlay**: a draggable edge tab and lens over other apps. Works on the Android 16 emulator. **Finding:** the system back gesture captured edge swipes, so the tab now excludes itself from system gestures. Re-checked on the emulator with gesture navigation: a pull no longer triggers Back (still to check on a phone).
- [x] **S2 Capture A** (emulator): `takeScreenshot()` in 55–58 ms, sRGB, and the overlay is hidden from the snapshot.
- [x] **S3 Capture B** (emulator): MediaProjection in 22 ms. **Finding:** Android 16's consent dialog defaults to "Share one app", and a red recording chip shows. Confirms A as preferred.
- [ ] **S1–S4 on real phones**: Android 11 / 13 / 14 / 15 / 16 across Pixel, Samsung, Xiaomi. Check colors on Display P3 and "vivid" screen modes.
- [x] **S5 Policy check**: the Ko-fi link is allowed (creator tip, 100% to the creator, nothing unlocked). `isAccessibilityTool` fits a vision aid, but Google decides at review, and a Play Console declaration plus video is required. Details in SPEC §5.2 and §7.

**Exit criteria:** a capture backend is chosen. The snapshot-on-pull flow is proven to work without the lens appearing in its own image. *(Met on the emulator; waiting on real phones.)*

## M1 — Color engine (`:colorengine`) (≈ 3 days) — *done, pending human review*

- [x] sRGB ↔ linear ↔ OKLab conversions, unit-tested against reference values.
- [x] Curated palette: 12 families + 124 friendly names, each with an OKLab anchor, family and stable key. Hand-written **en, es and pt-BR** names.
- [x] `ColorNamer.match(rgb)` → entry, family, tone (pale/light/dark/deep), hex. `ColorLocalization.label(match, lang)` → name + "Family · tone".
- [x] Achromatic threshold: grays, black and white never get hue names.
- [x] `HysteresisNamer`: on noisy boundaries, label changes drop from 89 to 1.
- [x] Tests: 21 tests, including reference values, every anchor naming itself, a no-near-duplicates check, golden names, Material-color families, gradient sweeps, localization completeness, and a benchmark (≈0.0007 ms per lookup on desktop).
- [ ] Expand the golden table toward about 300 entries during palette review.
- [ ] Palette review by colorblind testers, a color-normal reviewer, and a native speaker of each language.

## M2 — Overlay UI (≈ 1 week) — *done on the emulator*

Checked on the Android 16 emulator only (Pixel 9 Pro AVD, gesture navigation); nothing here has been seen on a real phone yet.

- [x] Hosts behind one `OverlayRuntime`: the accessibility service is the main path, MediaProjection (`ProjectionOverlayService`, foreground) is the fallback, both behind `ScreenCapturer`. Persistent notification with Hide/Show and Settings (plus Stop on the fallback). **Note:** the accessibility host posts a normal ongoing notification, not a foreground-service one, so on Android 14+ the user can swipe it away; the Quick Settings tile still works.
- [x] `EdgeTabView`: docking left or right, vertical drag, hold-and-carry to snap to the other edge, auto-dim to 30% after 3 s, saved position (as a fraction of screen height, so it survives rotation). TalkBack: double-tap opens the lens; a custom action switches the edge.
- [x] **Tap no longer opens the lens** (pre-v1 UX pass, 2026-11-03). On the Galaxy S22 (Android 16, 3-button navigation) tapping the tab opened the lens too easily by accident. Now only a pull opens it. A tap with the lens closed nudges the pill 10 dp inward and back, to show that it pulls. A tap with the lens open still puts it away. A touch that moves past the slop without becoming a move or pull is no longer a tap. Before, a 15 dp inward flick counted as a tap and opened the lens at screen centre. The decisions live in `TabGesture` (13 JVM tests). The view is now marked clickable, so TalkBack's double-tap arrives as ACTION_CLICK. With TalkBack on, a touch tap still opens the lens, in case the double-tap reaches the view as a touch.
  - Checked on the Android 16 emulator (gesture navigation) with adb taps, swipes and raw motion events. Tap on the pill and 20 dp inside: no lens, pill nudges. 15 dp inward flick: nothing. Pull: lens. Tap with the lens open: closes. Vertical drag: moves and saves. Hold-and-carry: left edge and back. 45° diagonal: still slides the tab (unchanged). Touches 8 dp above or below the tab still go to the app, and a swipe there still triggers Back (the exclusion covers the tab only, as before). With TalkBack on, a touch tap opened the lens and announced the name.
  - Diagnosis notes (sizes unchanged on purpose): the hit area is 28×56 dp around an 8×56 dp pill. The emulator's back-gesture strip is 30 dp, and its exclusion region matched the tab's window exactly. The tab doesn't swallow its first touch after dimming: touch-down restores full opacity and the touch carries on. View slop is 8 dp, pull threshold 24 dp, hold 400 ms (500 ms on the S22).
  - **Not verified:** the nudge and the "tap doesn't open" feel on a real phone. A real TalkBack double-tap on the tab (adb-injected taps bypass TalkBack's touch explorer, so only the touch fallback was exercised). Gesture navigation on a real phone.
- [x] Pull gesture: tab → lens spring animation (skipped when system animations are off). Putting the lens away: a ✕ on the label, dragging it back onto its edge (it fades over the drop zone), a fling toward that edge (1500 dp/s), or tapping the tab; TalkBack gets "Close lens" and "Look again" actions. *(✕ and drop zone added after testing showed fling-only wasn't discoverable; both checked on the emulator.)*
- [x] `LensView` (Canvas): circular clip, nearest-neighbour scaling from the snapshot, two-tone ring, gapped crosshair.
- [x] Name label pill: auto-placed above or below the lens, kept clear of the system bars and of the camera cutout itself (flips above or below to avoid it). Shows swatch, name, family and optional hex/RGB. The debug line is hidden unless the developer setting is on.
- [x] Fine-adjust precision drag (0.25× speed, extra inner ring): long press on the lens, or drag starting outside the circle.
- [x] Haptic tick when the name changes (setting).
- [x] TalkBack announcement, debounced 700 ms and only when the name changed. Checked with TalkBack 16 on: opening the lens announces "Light gray, Gray" once and TalkBack speaks it 700 ms later. Debug builds log each announcement (`announced: …` in logcat). `announceForAccessibility` is deprecated in API 36 but still spoken; revisit if a TalkBack update drops it.
- [x] Rotation: lens dismissed, tab re-docked, MediaProjection virtual display resized. Checked on both backends.
- [x] Multi-window and display cutouts. Split-screen: the lens reads the right pixel in both halves (overlays cover the whole display, so no change was needed). Landscape with the punch-hole on the tab's edge: the lens used to clip at the cutout band, 156 px in from the screen edge; now only the system bars limit it sideways. The tab slides clear of the hole itself. Freeform windows not tried (off by default on phones).
- [x] Quick Settings tile (show/hide tab; opens the app when no host is running).
- [x] Settings in DataStore (`SettingsRepository`) replace `SpikePrefs`: lens size, zoom, edge, auto-dim, sample size, hex, haptics, developer debug line, tab visibility and position. The settings UI on the main screen is still the plain-views test screen; the real one comes in M4.

## M3 — Capture integration (≈ 4 days) — *done on the emulator*

Most of this landed with M0 and M2; the rest is error handling. Checked on the Android 16 emulator only.

- [x] `ScreenCapturer` interface + both backends: accessibility screenshot (main) and MediaProjection (fallback). *(M0/M2)*
- [x] Snapshot-on-pull: hide overlay → wait two frames → capture → copy to software `ARGB_8888` → hand to `LensView`. *(M0)*
- [x] Refresh by tapping the label. The bitmap is recycled when the lens is dismissed or replaced. *(M0/M2)*
- [x] Screen → bitmap coordinates: scaled by the snapshot size, so it holds across rotation; checked in portrait, landscape and split-screen. *(M2)*
- [x] `FLAG_SECURE`: a fully black capture shows "Content hidden · The app blocks capture". Tested with a "Hide this screen from screenshots" switch on the test screen. **Limit:** in split-screen with one protected app, only half the screen is black, so the lens there just says "Black".
- [x] Error paths:
  - A capture that never answers times out after 3 s, so the overlay can't stay invisible.
  - A refresh inside Android's screenshot rate limit (about 3 per second) waits and retries once instead of failing.
  - Failures show a short, translated message instead of the raw error.
  - Accessibility service killed: Android restarts it and the tab comes back (tested by killing the process).
  - Screen-recording consent denied: a message explains the lens can't start. On Android 14+ the dialog only offers "Share entire screen" ("Share one app" is greyed out).
  - Recording stopped from the system chip: the fallback stops and says so. *(Written, not exercised: the emulator has no easy way to tap the chip from adb.)*
  - "Display over other apps" revoked: the fallback notices, stops, and asks for it again. A refused overlay window also stops the host instead of crashing.
  - Turning on the accessibility service while the fallback runs: the fallback stops and the accessibility host takes over, with one tab.
- [x] **Stale snapshot after scrolling** (found in testing): the lens stays open after the finger lifts, so the app underneath can scroll. The lens now re-snapshots 250 ms after the last scroll or window-change event (accessibility host) or 700 ms after a touch outside it (both hosts, and apps that send no scroll events). It blinks once per refresh. Checked on the emulator: scrolling the test chart under the lens updated it.
- [x] **Lens follows the scroll** (spec §5.2 step 4; the user wanted scrolling to feel like dragging the lens). Scroll events with a distance (`ScrollView`, `RecyclerView`) shift the band of the lens's picture right away, gliding between events; settle snapshots are lined up with the one before (`ScrollRegistration`, JVM-tested: fixed bars, pure shift, unrelated, repeating rows, noise, blank rows) to find the band, and stitched into a ring buffer of 3 band heights (~37 MB on the emulator) so scrolling back shows seen content at once. A lens on a fixed bar stays put; unseen content shows the empty fill and "…". No haptics or announcements mid-scroll. Checked on the Android 16 emulator only:
  - Settings home (`ScrollView`): offsets match the reported distance exactly (e.g. 1191/1191), band 444–2856 below the fixed search bar; mid-scroll screenshots show the lens text moving with the list, about one event (~115 px) behind; scrolling back drew from the stitched buffer (logged) before the refresh.
  - Settings > Accessibility and > Apps (`RecyclerView`): same, with the lens on the fixed toolbar holding still. The collapsing toolbar moves the list without scroll events (registered 924 vs reported 822); registration corrects it at the refresh. A scroll event arriving after the capture is taken back out using the registered offset (seen: 984 vs 933 reported).
  - Settings > Apps > See all apps (Compose): deltas are `-1`, so no live shift; `scrollY` differences were 2.1–2.2× the true offset (estimates, not usable). Registration finds the right offset but only 24–53% of rows match exactly (likely sub-pixel text), so it reports "unrelated" and only the refresh updates the lens.
  - Snapshots taken during stretch overscroll at a list's end don't line up; stitching starts over (fail-safe).
  - Not tested: WebView (needs a local test page; skipped), the screen-recording fallback with stitching (no scroll events there; registration runs on its refreshes but wasn't exercised), real phones.
## M4 — Main app (≈ 4 days) — *done; TalkBack walk-through still to do*

Jetpack Compose + Material 3 (AndroidX only), replacing the plain-views test screen. Checked on the Android 16 emulator (light theme, gesture navigation) and on a Galaxy S22 with Android 16 (dark theme, pt-BR, 3-button navigation), screens only; the lens itself was not used on the phone.

- [x] Onboarding, shown once: what the app does → turn on the accessibility service (with "Switch greyed out?" help for sideloaded installs) → notifications (Android 13+) → practice on the test chart → done, with the one Ko-fi line. Every step can be skipped. "Display over other apps" is not asked for here: only the screen-recording fallback needs it.
- [x] Home doubles as the permission status screen: lens state, accessibility service and notifications with a fix button each, and the screen-recording fallback (overlay permission, start/stop) folded away at the bottom. Re-checked whenever the app comes back to the front. "Open Accessibility settings" scrolls to and highlights DaltonPick's row.
- [x] Settings: edge tab (show, edge, auto-dim), lens (size, zoom, sample size), label (hex/RGB, haptics), language, developer (capture details, secure-screen test). The notification's Settings button opens this screen directly.
- [x] Language: Android 13+ per-app language (System / English / Español / Português (Brasil)); the overlay label follows it too. Android 11–12 follow the system language, so the section is hidden there.
- [x] Test chart: basic colors, six commonly confused pairs, and the red→green gradient, each with the name the lens should give.
- [x] About: version, "free, no ads, never goes online", **Support on Ko-fi ☕** (opens the browser; `Links.KOFI` is the one constant), privacy policy, source code, open-source licenses (MIT text plus the AndroidX/Kotlin libraries under Apache 2.0, linked), and a pointer to Android's own Color correction. The privacy link opens the policy on the GitHub Pages site, in the app's language.
- [x] Theming: light and dark, with wallpaper colors on Android 12+. States are spelled out ("✓ On" / "✕ Off"), never color alone. 200% font scale checked on Home, Settings and About.
- [x] Launcher icon: adaptive lens-and-crosshair over a color wheel, with a monochrome layer for themed icons.
- [ ] TalkBack walk-through of the main app. Screens use Compose's standard semantics (switches, radio groups, headings, state descriptions, swatch names), but a full pass with TalkBack running was not done: injected taps on the emulator don't drive TalkBack reliably. Do it by hand on a phone.
## M4b — Camera mode (≈ 4 days) — *done on the emulator; needs a real camera*

Checked on the Android 16 emulator's virtual back camera (the 3D room scene). Not yet tried with a real camera, so real-world color accuracy, the torch and pinch-zoom are untested.

- [x] CameraX (1.4, AndroidX, no network) full-screen preview with the overlay's look drawn in Compose: dark-and-light ring at the lens size from Settings, gapped crosshair, sample box, and the same name pill (swatch, name, family · tone, optional hex/RGB).
- [x] Live naming from `ImageAnalysis` frames in RGBA_8888 (CameraX does the YUV → RGB step): 5×5 linear-light average at the center of the frame's crop rect, which shares the preview's viewport, through `HysteresisNamer`. About 8 updates a second; frames are read and dropped, never stored. `FrameSampler` has unit tests (row padding, edges, flat colors).
- [x] "Hold picture" freezes the frame; drag to check any spot on it (checked: dragging onto the green square reads "Green"). "Back to live" resumes.
- [x] Tap the label to hear the name through the system text-to-speech engine, in the app's language. Checked that the engine synthesizes it (on-device voice); not heard, since the emulator has no speaker check.
- [x] Torch button, shown only when the camera has a flash (the emulator has none, so untested).
- [x] Pinch to zoom, with a zoom badge. Untested: adb can't send a two-finger pinch.
- [x] Haptic tick on name changes (Settings), and TalkBack reads the name once it has held still for 700 ms.
- [x] Lighting hint on the first three opens (spec §4.4); a "drag to check any spot" hint while the picture is held.
- [x] `CAMERA` requested on first use only, after an explanation screen; after a "don't ask again" denial it sends people to App info. Checked deny-then-allow.
- [x] Home entry ("Camera lens", hidden on devices without a camera) and a launcher shortcut (long-press the icon). Checked both.
## Pre-v1 UX review (2026-11-03)

From a pass over the lens, onboarding, Home, Settings and camera mode on the Android 16 emulator. Onboarding was reviewed from the code only, not walked through: resetting it means clearing app data, which also turns off the accessibility service. Fixed already: the camera's "Hold picture" button no longer looks active while the camera is still starting (before, tapping it did nothing).

- [x] **The edge tab looked like a scrollbar.** The pill is now 10 dp wide (was 8) with three grip dots.
- [x] **First tap gave no words.** For the first three taps a small bubble beside the tab says "Pull the tab toward the middle to open the lens" (en, es, pt-BR). It is drawn in the overlay, not as a toast: Android drops a background app's toasts when its notifications are off (seen on the emulator), and notifications are optional.
- [x] **The tab floated over DaltonPick's own camera mode.** It is now hidden while camera mode is on screen and comes back when it closes (checked on the emulator).
- [x] **The Developer section showed in release builds.** It now shows in debug builds, or after tapping the version in About seven times. Checked hidden in the release build on the S22 (2026-11-05).
- [x] **Notifications "✕ Off" read as an error on Home.** It now reads "Optional" with a neutral symbol.
- [ ] **Diagonal pulls.** A pull at 45° or steeper slides the tab instead (the split is at 45°). It wasn't a problem on the phone; revisit if testers report it.
- [x] **Main app look.** Home has the app mark and a single lens card; setup rows show only while something is missing, and the screen-recording section only while the accessibility service is off. Rows sit in rounded groups with icons and chevrons, status uses icons plus words, sliders show their value on the right, onboarding shows progress dots, and Android 11 gets a blue theme from the icon instead of the default purple. Onboarding was checked from the code only.

## M5 — Hardening & QA (≈ 1 week)

- [ ] Device matrix: Pixel (stock), Samsung One UI, Xiaomi/HyperOS (aggressive background killing, extra overlay permission), Motorola. Android 11–16.
- [ ] Battery check: idle drain ≈ 0 (no wakelocks, no capture loop).
- [ ] Memory: no leaked bitmaps after 100 pull/dismiss cycles (LeakCanary, debug builds only).
- [ ] Verify the manifest has no `INTERNET` permission and the release build contains no network-capable SDKs.
- [ ] Usability sessions with 3–5 colorblind users (protan/deutan/tritan if possible).
- [ ] CVD-simulated review of the app's own UI.

## M6 — Release (≈ 3–5 days + review time)

- [ ] Formal trademark search for "DaltonPick" (USPTO, INPI, EUIPO).
- [ ] Native-speaker review of all strings and color names (en, es, pt-BR).
- [ ] Store listing (en, es, pt-BR), screenshots, short demo video. Mention "free, no ads, no tracking" and the Ko-fi link.
- [x] Privacy policy page: <https://wch-softworks.github.io/dalton-pick/privacy/> (also `/es/`, `/pt-BR/`), built from `docs/store/` with a small landing page in `site/` (2026-11-07).
- [ ] Data Safety form ("no data collected"). Accessibility API declaration: Vision category, target users, and a video of the service being enabled and used. If rejected, appeal first, then fall back to MediaProjection.
- [ ] Publish the source on GitHub (WCH-Softworks, MIT). Optional F-Droid release.
- [ ] Internal → closed testing (Play requires 12+ testers for 14 days on new personal developer accounts) → production.
- [x] Release pipeline: R8 release build signed with an upload key from GitHub secrets, CI tests on every push, a hand-started Release workflow that uploads with Gradle Play Publisher (internal or closed track), store text in `app/src/main/play`. Play Console steps are in [docs/RELEASE.md](docs/RELEASE.md).
- [x] Signed release build checked on a phone: installed from the internal track on the Samsung S22 (Android 16), lens and camera mode work with R8 (2026-11-05).
- [ ] Baseline profiles.

**Rough total: about 5–6 weeks for one developer** (including camera mode), plus Play review and closed-testing time.

---

## Later (post-v1)
- Camera white-balance calibration ("tap something white").
- iOS app (Kotlin Multiplatform `:colorengine` + SwiftUI). Camera and photo/screenshot modes only, since iOS doesn't allow overlays.
- History / favorites, share a color.
- More languages.
- F-Droid release (easy if the app is open source and has no proprietary SDKs).

## Key risks

| Risk | Impact | Mitigation |
|---|---|---|
| Play rejects the Accessibility-service use | Recurring consent dialog UX | Accessibility-tool declaration with a clear justification. MediaProjection fallback behind the same interface. |
| Play objects to the in-app Ko-fi link | Listing rejected | The policy allows creator tips (checked in M0), but enforcement has been inconsistent. Fallback: link only on the store listing and website. |
| The edge tab conflicts with the back gesture | Pulling the tab triggers "back" | Gesture exclusion rect on the tab (added in M0). Verify on OEM launchers. |
| The lens appears in its own capture | Wrong colors / recursion | Snapshot-on-pull with the overlay hidden for one frame (validated in M0) |
| OEM background killing (Xiaomi, etc.) | Tab disappears | Foreground service, battery-optimization guidance screen, boot receiver |
| Wide-gamut or "vivid" display modes | Names don't match what the user sees | Convert to sRGB. Document that names describe the source color. |
