# Declaration video: shot-by-shot script

Google asks for "a short video which shows your app being opened, the accessibility services being enabled, and the accessibility services being used", with a voice-over or captions if the use isn't obvious from the UI (see [accessibility-declaration.md](accessibility-declaration.md)). This script gives about **100 seconds**, one take, no cuts needed.

Reviewers read English. Burn in **English captions** (and optionally record the Spanish and Portuguese versions of the captions below as separate files for the localized store listings). Captions are easier to review than voice-over and need no recording setup.

## Before you record

| Item | Setting |
|---|---|
| Device | A real phone on Android 11–16, not an emulator. Use the release candidate that is on the Play internal track, not the M0 test screen. |
| Language | Phone set to **English** for the main version. |
| Recording | Android's built-in screen recorder, 1080p, **"Show touches" on** (Developer options → Show taps). Record system audio off. |
| Clean state | Fresh install, accessibility service **off**, overlay permission **already granted** (to keep the video short; mention it in the caption if you prefer to show it). Do Not Disturb on, so no personal notifications appear. Hide or avoid personal data on screen. |
| Demo content | A page of test swatches in the browser, or the app's own "Test chart" screen. Use a **non-personal** photo (a fruit stand, a paint chart). Do not use a banking or messaging app. |
| Upload | YouTube, **Unlisted**. Open the link in a signed-out window to confirm it plays. Add the link to the declaration form. |

## Shot list

Timing is approximate. "On screen" is what the camera sees; "Caption" is the burned-in English text; "Say (optional)" is for a voice-over if you prefer one.

| # | Time | On screen | Caption (EN) | Say (optional) |
|---|---|---|---|---|
| 1 | 0:00–0:05 | Title card on a plain background: **DaltonPick**, "Accessibility service demo". | DaltonPick helps people with color blindness read the name of any color on their screen. | "DaltonPick helps people with color blindness read the name of any color on their screen." |
| 2 | 0:05–0:10 | Home screen. Finger taps the **DaltonPick** icon. The app opens on its welcome screen. | 1. Opening the app | "I open the app." |
| 3 | 0:10–0:20 | Onboarding explains what the app does, in plain words, and why it needs the accessibility service ("to take a screenshot when you pull out the lens; it does not read your content"). Pause 2 seconds so the text is readable. Tap **Continue**. | The app explains why it needs the accessibility service before asking. | "Before asking, the app explains what the service is for." |
| 4 | 0:20–0:40 | Tap **Open Accessibility settings**. Android Settings opens. Scroll to **Downloaded apps** (or Installed apps), tap **DaltonPick**. Show the service description text. Turn the toggle **On**. The system consent dialog appears: let it stay on screen for 3 seconds, then tap **Allow**. Toggle shows On. | 2. Turning on the accessibility service in Android Settings | "I turn the service on in Android Settings and confirm Android's consent dialog." |
| 5 | 0:40–0:45 | Press Back to return to the app. The "Service is on" state shows. A small edge tab appears at the screen edge. | The service is on. A small tab waits at the screen edge. | "Now a small tab waits at the edge of the screen." |
| 6 | 0:45–0:55 | Open the browser (or any non-DaltonPick app) showing the test swatches. The edge tab is visible over it. Point at it with a tap. | 3. Using the lens over another app | "I open another app. The tab stays on top." |
| 7 | 0:55–1:05 | **Pull the tab inward.** The lens appears under the finger, magnified, with a crosshair. A name label shows, for example **"Dusty rose"** with the line **"Pink"**. Pause 2 seconds. | Pulling the tab out takes one screenshot. The lens shows the name of the color under the crosshair. | "When I pull out the tab, the service takes one screenshot. The lens shows the color's name." |
| 8 | 1:05–1:20 | Drag the lens across three or four swatches, ending on a red and then a green swatch. The label changes each time ("Red", "Olive green"). Include one small detail (a thin chart line) and use the fine-adjust mode to hit it. | The lens names each color. Fine-adjust mode moves it slowly to hit small details. | "I move the lens over different colors and it names each one." |
| 9 | 1:20–1:30 | Tap the refresh dot on the label after scrolling the page slightly. The label updates. | Refresh takes a new screenshot. Nothing is saved. | "Refresh takes a new screenshot. Screenshots stay in memory and are never saved." |
| 10 | 1:30–1:35 | Tap the ✕ on the label. The lens closes and only the small tab is left. | Dismiss: the lens closes and the screenshot is discarded. | "When I close the lens, the screenshot is thrown away." |
| 11 | 1:35–1:45 | Settings → Accessibility → DaltonPick: turn the toggle **Off**. Return to the app: it shows "Service is off" and the tab disappears. | The user can turn the service off at any time. | "I can turn the service off at any time." |
| 12 | 1:45–1:50 | End card: **No internet permission · No ads · No data collected · Open source** and the repository address. | DaltonPick: no internet permission, no ads, no data collected. Open source. | "DaltonPick has no internet permission, no ads and collects no data." |

## What reviewers should be able to see

- [ ] The app being opened from the launcher (shot 2)
- [ ] The accessibility service being enabled, including the system consent dialog (shot 4)
- [ ] The service being used for its stated purpose, over another app (shots 6–9)
- [ ] The user being able to turn it off (shot 11)
- [ ] The explanation text is readable (hold for 2 seconds each)

## Common reasons a video gets sent back

- The app shown is a test build with debug text instead of the real UI.
- The service is already enabled at the start, so the enabling step is missing.
- The captions go by too fast, or the text on screen is too small to read at 1080p.
- A private notification or personal photo shows up.
- The link needs a sign-in.

## Caption translations

Use these if you record localized versions. Numbers match the shot list.

| # | Español (Latinoamérica) | Português (Brasil) |
|---|---|---|
| 1 | DaltonPick ayuda a las personas con daltonismo a leer el nombre de cualquier color en su pantalla. | O DaltonPick ajuda pessoas daltônicas a ler o nome de qualquer cor na tela. |
| 2 | 1. Abrir la aplicación | 1. Abrindo o app |
| 3 | La aplicación explica para qué necesita el servicio de accesibilidad antes de pedirlo. | O app explica por que precisa do serviço de acessibilidade antes de pedir. |
| 4 | 2. Activar el servicio de accesibilidad en los ajustes de Android | 2. Ativando o serviço de acessibilidade nas configurações do Android |
| 5 | El servicio está activado. Una pestaña pequeña espera en el borde de la pantalla. | O serviço está ativado. Uma pequena aba espera na borda da tela. |
| 6 | 3. Usar la lupa sobre otra aplicación | 3. Usando a lupa sobre outro app |
| 7 | Al sacar la pestaña se toma una captura de pantalla. La lupa muestra el nombre del color bajo la mira. | Ao puxar a aba, o serviço faz uma captura de tela. A lupa mostra o nome da cor sob a mira. |
| 8 | La lupa nombra cada color. El ajuste fino la mueve despacio para llegar a detalles pequeños. | A lupa nomeia cada cor. O ajuste fino move a lupa devagar para acertar detalhes pequenos. |
| 9 | Actualizar toma una captura nueva. No se guarda nada. | Atualizar faz uma nova captura. Nada é salvo. |
| 10 | Cerrar: la lupa se cierra y la captura se descarta. | Fechar: a lupa fecha e a captura é descartada. |
| 11 | La persona puede desactivar el servicio cuando quiera. | A pessoa pode desativar o serviço quando quiser. |
| 12 | DaltonPick: sin permiso de Internet, sin anuncios, sin recopilar datos. Código abierto. | DaltonPick: sem permissão de internet, sem anúncios, sem coleta de dados. Código aberto. |

## Notes on what the UI must contain

This script assumes the M2–M4 screens described in SPEC §4.5 and IMPLEMENTATION_PLAN (onboarding with an explanation before the permission step, a service on/off state, a refresh dot on the label). If the final UI differs, re-record rather than edit the script to match the old video.
