# M0 — Testing the spike build on a phone

The M0 build is a bare test app. It is not the real UI. It exists to answer one question per phone: **does the pull-out lens capture the screen quickly, without capturing itself, and read the right colors?**

## 1. Build and install

USB debugging must be on (Settings → About phone → tap *Build number* 7 times → Developer options → USB debugging).

```bash
./gradlew :app:assembleDebug
```

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## 2. Backend A — Accessibility capture (preferred)

1. Open **DaltonPick**, then tap **Open Accessibility settings**.
2. Find **DaltonPick** under *Downloaded apps* and turn it on. Android shows a warning; this is the one-time consent.
   - Android 13+ may say the setting is *restricted* for sideloaded apps. Fix: Settings → Apps → DaltonPick → ⋮ → *Allow restricted settings*, then try again.
3. Back in the app, tap **Start lens (Accessibility capture)**. A small pill appears on the right edge.
4. Go to any app and **pull the pill inward**. The lens should appear above your finger.

## 3. Backend B — Screen-recording capture (fallback)

1. Tap **Allow display over other apps** and grant it.
2. Tap **Start lens (screen-recording capture)**. In the dialog, switch *Share one app* to **Share entire screen**.
3. Pull the pill as above.

Use one backend at a time: **Stop lens**, or turn off the accessibility service, before testing the other.

## 4. What to check

The label's third line shows `backend · capture ms · size · color space · sampled hex`.

| # | Check | Pass if |
|---|---|---|
| S1 | Pull the pill out from the edge with **gesture navigation** on | The lens opens; the phone does **not** go "back" |
| S1 | Drag the pill up and down | It moves along the edge and stays there after restart |
| S1 | Drag the lens around; tap the pill; fling the lens toward the pill | Moves smoothly; tap and fling both close it |
| S2/S3 | Look inside the lens right after pulling | It shows the app underneath, **not** the lens or pill itself |
| S2/S3 | Turn off *Hide overlay before capture* and pull again | Tells us whether hiding is needed. Note if the pill or lens appears in the image |
| S2/S3 | Capture ms on the label | Under ~150 ms feels instant. Note the number |
| S4 | Put the lens on each test swatch in the app | The lens name matches the name printed under the swatch |
| S4 | Note the color space on the label | `sRGB` or `Display P3`. Repeat S4 with the screen mode set to *Vivid* / *Natural* if the phone has it |
| S4 | Open a banking app or Netflix and pull the lens | It says *"This app hides its content"* |
| — | Drag slowly across the red→green gradient | The name changes in a few steps without flickering back and forth |
| — | Tap the label | Takes a fresh snapshot (refresh) |
| — | Set the phone language to Español or Português (Brasil) | The app and color names switch language |

## 5. Report back

For each phone, copy and fill in:

```
Phone / Android version:
Navigation: gestures | 3-button
Backend A: works? capture ms: color space: lens shows itself? (hide on/off):
Backend B: works? capture ms:
Back-gesture conflict when pulling?
Swatch names that were wrong:
Anything odd:
```

Known so far (Android 16 emulator): A works in 55–58 ms; B works in 22 ms; both sRGB; hiding works on both. The back-gesture fix and the edge-clamped label are in this build but **were not verified on a device yet**.
