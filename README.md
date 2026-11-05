# DaltonPick

A free Android app that helps colorblind people identify colors anywhere on their screen. Pull a small tab from the screen edge, and a round magnifying lens shows the name of the color under its crosshair ("Navy blue", "Dusty rose"). It works in English, Spanish and Brazilian Portuguese.

No ads, no tracking, no internet permission. If it helps you, you can [support it on Ko-fi](https://ko-fi.com/willcaphir).

> **Status:** in testing on Google Play, not yet released. See [SPEC.md](SPEC.md) and [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

## Feedback and bug reports

Testers and users are welcome to [open an issue](https://github.com/WCH-Softworks/dalton-pick/issues/new/choose) for bugs, wrong color names, or feedback. Please don't post personal information in issues: they are public.

Found a security or privacy problem? Report it privately, see [SECURITY.md](SECURITY.md).

## Contributing

DaltonPick is open source so anyone can read and audit it, but it is **not open to code contributions**: pull requests will be closed without review. See [CONTRIBUTING.md](CONTRIBUTING.md). You're free to fork it under the MIT license.

## Project layout

| Path | What it is |
|---|---|
| `colorengine/` | Pure Kotlin color naming: OKLab, curated palette with en/es/pt-BR names, hysteresis. No Android dependencies. |
| `app/` | The Android app: edge tab, lens overlay, two screen-capture backends, camera mode, settings and onboarding. |
| `docs/` | Testing guides, release steps, store and policy documents, research notes. |

## Build

Requires JDK 17+ and the Android SDK (compileSdk 36).

```bash
./gradlew :colorengine:test
```

```bash
./gradlew :app:assembleDebug
```

To try a debug build on a phone, follow [docs/M0-TESTING.md](docs/M0-TESTING.md).

## License

[MIT](LICENSE) © 2026 WCH Softworks
