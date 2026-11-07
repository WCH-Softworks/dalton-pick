# DaltonPick Privacy Policy

- **Effective date:** November 7, 2026
- **Developer:** WCH Softworks
- **Contact:** bugs and suggestions on [GitHub Issues](https://github.com/WCH-Softworks/dalton-pick/issues); privacy questions to <admin@willcaphir.com>
- **Source code:** <https://github.com/WCH-Softworks/dalton-pick>


## The short version

DaltonPick does not collect, store, share or sell any personal data. It has no internet permission, so it cannot send anything off your phone. It has no ads, no analytics and no accounts.

## What DaltonPick does with your screen

DaltonPick shows a magnifying lens over other apps and tells you the name of the color under its crosshair. To do that it needs to see the pixels on your screen.

- When you pull out the lens, the app takes a screenshot of your screen using Android's accessibility service. While the lens stays open, it takes a fresh one when the screen scrolls or changes, so the lens keeps showing what is really there.
- The screenshot is kept **in memory only**, on your phone. It is used to magnify the area under the lens and to read one small group of pixels to work out the color name.
- The screenshot is **never saved to storage, never logged, never shared and never uploaded**. It is thrown away when you close the lens.
- DaltonPick does not read the text on your screen, your messages, your passwords or what you type. The accessibility service is set up so that it cannot read window contents.
- Some apps (banking apps, some video apps) block screenshots. In those apps the capture comes back blank and DaltonPick tells you it cannot see the content.

## The accessibility service

DaltonPick uses Android's AccessibilityService API as an accessibility tool for people with color vision deficiency. The service is used only to take the screenshots described above. To know when to take a fresh one, it is told *that* a screen scrolled or changed, and by how much it scrolled; it is not told what is on the screen. It does not perform actions for you, click anything, or record what you do in other apps. You turn it on yourself in Android settings (Settings → Accessibility) and you can turn it off at any time. When it is off, the lens stops working.

## Screen-recording mode (fallback)

If you don't want to turn on the accessibility service, DaltonPick can use Android's screen recording instead. Android asks for your permission each time and shows its own screen-recording indicator while it runs. The recorded frames are used in memory, exactly like the screenshots above, and are **never saved or sent anywhere**. You can stop it at any time with the Stop button in its notification, in the DaltonPick app, or from Android's own screen-recording controls.

## Camera mode

If you choose camera mode, DaltonPick asks for the camera permission at that moment. Camera frames are processed in memory to name colors and are **never saved or sent anywhere**. You can use the rest of the app without ever granting camera access.

## Other permissions

- **Display over other apps:** to draw the edge tab and the lens on top of other apps.
- **Notifications:** to show a small ongoing notification that lets you show or hide the tab.
- **Foreground service (screen recording):** Android requires this while the screen-recording mode runs. It is not used with the accessibility service.

None of these gives DaltonPick any way to collect data about you.

## What is stored on your phone

Your settings (lens size, zoom, which edge, tab position and similar choices, plus small counters such as whether you finished the introduction) are stored locally on your phone. They contain no personal information. If you have Android's backup turned on, your phone may include these settings in its normal backup to your Google account. DaltonPick itself does not send them anywhere.

## Third parties

- DaltonPick contains **no third-party SDKs**: no ads, no analytics, no crash reporters, no billing.
- The About screen has a "Support on Ko-fi" link. Tapping it opens the Ko-fi page in your web browser. From that moment you are on Ko-fi's website, which has its own privacy policy. DaltonPick sends nothing to Ko-fi. Supporting the project does not unlock anything in the app.

## Children

DaltonPick collects no personal data from anyone, including children. It is not directed at children as a specific audience.

## Your rights

Because DaltonPick does not collect or hold personal data, there is nothing to access, correct, export or delete. Data protection laws such as the GDPR (EU), the LGPD (Brazil) and similar laws in Latin America give you rights over personal data; if you have a question about how those apply, write to <admin@willcaphir.com>.

## Changes to this policy

If a future version of DaltonPick changes how it handles data, this page will be updated before that version is released and the effective date will change. Past versions of this policy are in the project's public repository history.

## Contact

- **Bugs, wrong color names and suggestions:** open an issue at <https://github.com/WCH-Softworks/dalton-pick/issues> (issues are public, so don't include personal information).
- **Privacy questions or anything you'd rather not post publicly:** <admin@willcaphir.com>
