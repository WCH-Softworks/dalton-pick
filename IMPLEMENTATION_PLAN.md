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
- [x] Pull gesture: tab → lens spring animation (skipped when system animations are off). Fling back to dismiss.
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
## M4 — Main app (≈ 4 days)

- [ ] Onboarding flow: explanation → overlay permission → accessibility (or projection) permission → notifications → practice screen → one-line Ko-fi mention.
- [ ] Permission status screen with deep links to fix each missing grant.
- [ ] Settings screen (the DataStore repository exists since M2): lens size, zoom, edge, auto-dim, sample size, hex/RGB, haptics, language (per-app language, en/es/pt-BR).
- [ ] Test chart screen (a swatch grid that includes common confusion pairs).
- [ ] About screen: version, privacy policy link, open-source licenses, **Support on Ko-fi** button (`ACTION_VIEW` intent, URL in one constant).
- [ ] Theming (light/dark), font-scale and TalkBack QA.

## M4b — Camera mode (≈ 4 days)

- [ ] CameraX preview (AndroidX, open source, no network) full screen with the same `LensView` drawing and label.
- [ ] Sample from the `ImageAnalysis` frame at the crosshair (YUV → sRGB, 5×5 average), reusing `HysteresisNamer`.
- [ ] Freeze button, tap the label to speak the name, torch toggle.
- [ ] `CAMERA` permission requested on first use only, with a rationale screen.
- [ ] Main-app button + launcher shortcut ("Camera lens").

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
- [ ] Privacy policy page. Data Safety form ("no data collected"). Accessibility API declaration: Vision category, target users, and a video of the service being enabled and used. If rejected, appeal first, then fall back to MediaProjection.
- [ ] Publish the source on GitHub (WCH-Softworks, MIT). Optional F-Droid release.
- [ ] Internal → closed testing (Play requires 12+ testers for 14 days on new personal developer accounts) → production.
- [ ] Signed release build with R8 and baseline profiles.

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
