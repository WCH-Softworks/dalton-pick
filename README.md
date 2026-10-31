# DaltonPick

A free Android app that helps colorblind people identify colors anywhere on their screen. Pull a small tab from the screen edge, and a round magnifying lens shows the name of the color under its crosshair ("Navy blue", "Dusty rose"). It works in English, Spanish and Brazilian Portuguese.

No ads, no tracking, no internet permission. If it helps you, you can [support it on Ko-fi](https://ko-fi.com/willcaphir).

> **Status:** early development (milestone M0/M1). See [SPEC.md](SPEC.md) and [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

## Project layout

| Path | What it is |
|---|---|
| `colorengine/` | Pure Kotlin color naming: OKLab, curated palette with en/es/pt-BR names, hysteresis. No Android dependencies. |
| `app/` | Android app. Currently the M0 test build: edge tab, lens overlay, two screen-capture backends. |
| `docs/` | Testing guides and, later, store and policy documents. |

## Build

Requires JDK 17+ and the Android SDK (compileSdk 36).

```bash
./gradlew :colorengine:test
```

```bash
./gradlew :app:assembleDebug
```

To try the test build on a phone, follow [docs/M0-TESTING.md](docs/M0-TESTING.md).

## License

[MIT](LICENSE) © 2026 Guilherme Corneto
