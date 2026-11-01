# Play Console: AccessibilityService declaration

Basis: Google Play's [Use of the AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491) policy, read 2026-10-31. The article lists the questions for apps that set `isAccessibilityTool="true"`:

1. Describe one core feature in your app that requires a permitted use of the AccessibilityService API.
2. Which types of disability does your app serve? (Motor, **Vision**, Hearing, Cognitive or learning, Other)
3. Who are the target users of your app?
4. A link to a short video that shows the app being opened, the accessibility service being enabled, and the service being used (with voice-over or captions if it isn't obvious).

Submit the **English** text in Play Console (reviewers work in English). The Spanish and Portuguese versions are for the team and for any localized review correspondence. The wording of the console form can change; re-read the live form before pasting.

## Facts the answers rely on

All of these are true of the M0 build and must stay true for the release build. Re-check before submitting.

| Fact | Where |
|---|---|
| Service sets `android:isAccessibilityTool="true"` | `app/src/main/res/xml/accessibility_service.xml` |
| `android:canTakeScreenshot="true"` (needed for `AccessibilityService.takeScreenshot`, API 30+) | same |
| `android:canRetrieveWindowContent="false"`: the service cannot read what is on screen as text | same |
| Event types: `typeWindowStateChanged` and `typeViewScrolled` only, no text or click events. Only the event type and, for scrolls, the distance scrolled (`scrollDeltaX/Y`) are used: the cue that the content under an open lens moved, and how far, so the lens can follow it until its next screenshot. No event text, source node or window content is read | same |
| No `INTERNET` permission anywhere in the manifest | `app/src/main/AndroidManifest.xml` |
| One screenshot per pull, per refresh, and after the content under an open lens scrolls or changes screen; kept in memory, released when the lens closes | SPEC §5.2, §8 |
| No ads, analytics, or third-party SDKs | SPEC §5.1 |

If you change the service's events, capabilities or what it does with the screenshot, **resubmit the form**: the policy says changing how the API is used requires a new declaration.

## Answers (English: submit this)

### 1. Core feature

> DaltonPick is a color-naming magnifier for people with color vision deficiency. The user pulls a small edge tab and a round lens appears over any app; it magnifies the screen and shows the name of the color under its crosshair (for example "Navy blue", with the family "Blue"). To read the pixels of other apps, the service calls AccessibilityService.takeScreenshot() when the lens is pulled out, and again only when the user refreshes or the app under the open lens scrolls or changes screen (it listens for scroll and window-change events for that, and nothing else). The screenshot stays in memory and is discarded when the lens closes. The service does not read window content, text or input, does not perform actions or gestures, and has no network access.

(About 600 characters. Shorter version, about 240 characters, if the field is limited: *"A pull-out magnifying lens that names the color under its crosshair, over any app. The service calls takeScreenshot() when the lens opens or the content under it moves, to read screen pixels, in memory only. It does not read window content, perform actions, or access the network."*)

### 2. Types of disability served

- **Vision** (color vision deficiency: protanopia, deuteranopia, tritanopia and their milder forms, and achromatopsia).

Select only Vision. Do not select the others.

### 3. Target users

> People with color vision deficiency (color blindness), about 8% of men and 0.5% of women, who need to tell what color something is on their phone screen: lines in charts and maps, status indicators, clothing in shopping apps, game and design interfaces. The store listing, onboarding and in-app text are written for them, in English, Spanish and Brazilian Portuguese.

### 4. Video link

Public or unlisted YouTube link made following [declaration-video-script.md](declaration-video-script.md). Must open without signing in. [PASTE LINK HERE]

## Answers (español, para el equipo)

1. **Función principal:** DaltonPick es una lupa que dice el nombre de los colores para personas con deficiencia en la visión de los colores. El usuario desliza una pestaña del borde y aparece una lupa redonda sobre cualquier aplicación; amplía la pantalla y muestra el nombre del color bajo su mira (por ejemplo "Azul marino", con la familia "Azul"). Para leer los píxeles de otras aplicaciones, el servicio llama a AccessibilityService.takeScreenshot() al sacar la lupa, y otra vez solo cuando el usuario la actualiza o la aplicación bajo la lupa abierta se desplaza o cambia de pantalla (para eso escucha los eventos de desplazamiento y de cambio de ventana, nada más). La captura se queda en memoria y se descarta al cerrar la lupa. El servicio no lee el contenido de las ventanas, ni el texto, ni lo que se escribe, no realiza acciones ni gestos y no tiene acceso a la red.
2. **Tipo de discapacidad:** Visión.
3. **Usuarios objetivo:** personas con daltonismo (aproximadamente el 8 % de los hombres y el 0,5 % de las mujeres) que necesitan saber de qué color es algo en la pantalla de su teléfono: líneas de gráficos y mapas, indicadores de estado, ropa en tiendas en línea, interfaces de juegos y de diseño.

## Respostas (português do Brasil, para a equipe)

1. **Recurso principal:** o DaltonPick é uma lupa que diz o nome das cores para pessoas com deficiência na visão de cores. O usuário puxa uma aba da borda e uma lupa redonda aparece sobre qualquer app; ela amplia a tela e mostra o nome da cor sob a mira (por exemplo "Azul-marinho", com a família "Azul"). Para ler os pixels de outros apps, o serviço chama AccessibilityService.takeScreenshot() quando a lupa é puxada, e de novo só quando o usuário atualiza ou o app sob a lupa aberta rola ou muda de tela (para isso escuta os eventos de rolagem e de mudança de janela, nada mais). A captura fica na memória e é descartada quando a lupa fecha. O serviço não lê o conteúdo das janelas, o texto nem o que é digitado, não executa ações nem gestos e não tem acesso à rede.
2. **Tipo de deficiência:** Visão.
3. **Público-alvo:** pessoas daltônicas (cerca de 8% dos homens e 0,5% das mulheres) que precisam saber de que cor é algo na tela do celular: linhas de gráficos e mapas, indicadores de status, roupas em apps de compras, interfaces de jogos e de design.

## Why this qualifies as an accessibility tool (for the appeal, if needed)

The policy says eligible services are those "designed to help people with disabilities access their device or otherwise overcome challenges stemming from their disabilities", that the tool must have that as its **primary purpose**, and that it should be obvious from the store description who the users are and how it helps them. It lists screen readers, switch and voice input and braille systems as examples and adds that "tools supporting other users with disabilities" also qualify.

Points to make:

- **Primary and only purpose:** the app does one thing, naming colors for people who cannot reliably tell them apart. It is not a general assistant, automation tool, launcher, cleaner or monitor (all listed as *not* eligible).
- **The disability is a vision disability.** Color vision deficiency is recognized as a vision impairment; the product is built around its everyday challenges (charts, legends, status lights), and the store listing says so in the first sentence.
- **Why an accessibility service is needed.** For an ordinary app, only an accessibility service or a media-projection session can read other apps' pixels. The accessibility route lets the user grant consent **once** in system settings. The alternative (MediaProjection) shows a system dialog on every session, defaults to sharing one app instead of the whole screen, and shows a screen-recording indicator (measured in M0, SPEC §5.2). For a tool someone may open many times a day, that is a real barrier, particularly for users who already struggle with small controls.
- **Least privilege:** `canRetrieveWindowContent="false"`, two event types (window change and scroll, used only as a "content moved" cue), no gestures, no network, screenshot in memory only.
- **Verifiable:** open source under MIT and no `INTERNET` permission.
- **Fallback offered:** if Google declines the accessibility route, the project can switch to the MediaProjection backend behind the same `ScreenCapturer` interface (SPEC §5.2, §12). Say this only if the review team asks; leading with it weakens the case.

## Anticipated reviewer questions

| Question | Answer |
|---|---|
| Does the service read screen text, notifications or input? | No. `canRetrieveWindowContent="false"`; it receives only `typeWindowStateChanged` and `typeViewScrolled`, and uses only the event type, as a cue to refresh the open lens. No key or text events. |
| What happens to the screenshot? | Held as one in-memory bitmap while the lens is open; released on close. Never written to disk, logged or transmitted. The app has no internet permission. |
| Is anything collected or shared? | No. Data safety: "no data collected, no data shared". |
| Does it act on the user's behalf? | No gestures, taps or global actions are performed. |
| Why not MediaProjection? | See above. The accessibility route gives one-time consent and no recording indicator. |
| Is the prominent-disclosure screen needed? | Accessibility tools that set `isAccessibilityTool="true"` are exempt, but DaltonPick still shows an in-app explanation before sending the user to Settings (M4 onboarding). Keep it. |
| Android 13+ "restricted settings" on sideloaded builds | Applies only to sideloaded APKs. Play-installed builds are not restricted. Mention it in the testing notes, not in the declaration. |

## Checklist before submitting

- [ ] Declaration video recorded and uploaded, link opens signed-out
- [ ] Video shows the real release candidate, not the M0 test screen
- [ ] Manifest and `accessibility_service.xml` match the table at the top
- [ ] Privacy policy URL live (see `privacy-policy.*.md`) and entered in the listing
- [ ] Data safety form: no data collected, no data shared
- [ ] Store listing says who the app is for in the first sentence (it does)
- [ ] App content → "Accessibility" declaration answered with the text above
- [ ] The service `android:description` string (shown in Android's Accessibility settings) states plainly what the service does, in all three languages
