# Releasing DaltonPick on Google Play

Releases are built and uploaded by GitHub Actions only. Nothing secret lives in the repo: the upload key and the Play service account reach the build as GitHub secrets.

| Piece | Where |
|---|---|
| Release build (R8, resource shrinking, signing from env vars) | `app/build.gradle.kts`, `app/proguard-rules.pro` |
| Uploads | [Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher) 3.13 (build-time plugin; adds nothing to the app) |
| Store text uploaded by GPP | `app/src/main/play/` (generated from `docs/store/listing.*.md` by `node docs/store/sync-play.js`) |
| Tests + unsigned R8 build on every push | `.github/workflows/ci.yml` |
| Signed AAB, optional upload | `.github/workflows/release.yml` (run by hand) |

## Do I still need my own key with Play App Signing?

Yes, but only a small one. With Play App Signing, Google keeps the **app-signing key** that signs what users install. You still sign every AAB you upload with an **upload key**, so Google knows the upload came from you. If the upload key is ever lost or leaked, Play Console support can reset it (the app-signing key, which you never hold, is unaffected). So: make the upload key once, keep a backup in your password manager, and give it to GitHub as a secret.

## One-time setup

### 1. Create the upload key (on your PC)

Run in Git Bash. `keytool` comes with the JDK. Pick a strong password and save it in your password manager first.

```bash
keytool -genkeypair -v -keystore ~/daltonpick-upload.jks -storetype PKCS12 -alias upload -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=WCH Softworks"
```

PKCS12 uses one password for the store and the key, so `UPLOAD_KEYSTORE_PASSWORD` and `UPLOAD_KEY_PASSWORD` below are the same. Keep `daltonpick-upload.jks` out of the repo (`*.jks` is gitignored) and back it up next to the password.

Turn it into one line of text for GitHub:

```bash
base64 -w0 ~/daltonpick-upload.jks > ~/daltonpick-upload.b64
```

### 2. Create the app in Play Console

1. Play Console → **Create app**. Name "DaltonPick", default language **English (United States) – en-US**, App, Free. Accept the declarations.
2. **Play App Signing** is on by default for new apps: when asked for the app-signing key, keep "Use a Google-generated key".
3. **Testing → Internal testing → Testers**: make an email list (you plus anyone else) and copy the opt-in link.

### 3. GitHub secrets

Repo → Settings → **Environments** → create `play` (the release job runs in it; you can add yourself as a required reviewer so every release run waits for your click). Add these as environment secrets:

| Secret | Value |
|---|---|
| `UPLOAD_KEYSTORE_BASE64` | contents of `daltonpick-upload.b64` |
| `UPLOAD_KEYSTORE_PASSWORD` | the keystore password |
| `UPLOAD_KEY_ALIAS` | `upload` |
| `UPLOAD_KEY_PASSWORD` | the same password |
| `PLAY_SERVICE_ACCOUNT_JSON` | contents of the service-account JSON (step 5) |

Delete `daltonpick-upload.b64` afterwards.

### 4. First upload, by hand

The Play API can't upload an app's very first bundle, so the first one goes in through the browser:

1. GitHub → Actions → **Release** → Run workflow with **upload off**.
2. Download the `daltonpick-101` artifact (it holds `app-release.aab` and the R8 `mapping.txt`). The repo is public, so anyone signed in to GitHub can download run artifacts; that's fine for an open-source app signed with an upload key.
3. Play Console → Internal testing → **Create new release** → upload the AAB, add release notes, save, review and roll out to internal testers.

### 5. Service account for automated uploads

1. [Google Cloud Console](https://console.cloud.google.com/): create (or pick) a project, then **APIs & Services → Library → Google Play Android Developer API → Enable**.
2. **IAM & Admin → Service accounts → Create**, e.g. `daltonpick-release`. No Cloud roles needed.
3. On the new account: **Keys → Add key → JSON**. The file downloads once; put its contents in `PLAY_SERVICE_ACCOUNT_JSON` and then delete the file.
4. Play Console → **Users and permissions → Invite new users** → the service account's email → **App permissions** → DaltonPick → allow *Release to testing tracks* (add *Release to production* later if you want CI to do that too) and *Manage store presence* (for listing uploads). Permissions can take a while to work the first time.

## Before the first release can leave internal testing

All in Play Console. Draft answers are in `docs/store/`.

- **App content → Privacy policy**: host `docs/store/privacy-policy.*.md` at a stable URL (e.g. GitHub Pages) and enter it. Fill the `[DEVELOPER NAME]`, `[CONTACT EMAIL]` and `[DATE OF RELEASE]` placeholders first.
- **App content → Ads**: no ads.
- **App content → App access**: all features work without an account. Add instructions for the reviewer: turn on DaltonPick in Settings → Accessibility, then pull the edge tab.
- **App content → Content rating**: fill the questionnaire (utility app, no user content).
- **App content → Target audience**: 13+ or 18+; not designed for children.
- **App content → Data safety**: no data collected, no data shared. Screenshots and camera frames are processed on the phone, in memory only, and never leave it; on-device processing that never leaves the device does not count as collection. No encryption-in-transit question applies since nothing is sent.
- **App content → Accessibility API (permission declaration)**: DaltonPick sets `isAccessibilityTool="true"`. Answers are in `docs/store/accessibility-declaration.md`; record the video from `docs/store/declaration-video-script.md` and add its link.
- **App content → Foreground service permissions**: the app declares `FOREGROUND_SERVICE_MEDIA_PROJECTION` for the screen-recording fallback. Declare it (media projection, user-started capture of the screen to name colors) with a short video of the fallback.
- **Store presence → Main store listing**: text comes from `app/src/main/play` (run Release with **listing on**, or paste it). Add by hand for now: app icon 512×512, feature graphic 1024×500, at least 2 phone screenshots, category (Tools), contact email.
- **Countries/regions** for each track.

## Closed testing (new personal developer accounts)

Personal developer accounts created after November 2023 must run a **closed test with at least 12 testers who stay opted in for 14 days in a row** before they can apply for production access.

1. **Testing → Closed testing → Create track** (the default one is called *Alpha*; GPP calls it `alpha`). Add a tester email list or a Google Group with 12+ people.
2. Run Release with `track = alpha`. Share the opt-in link; testers must accept it and install from Play.
3. After 14 days with 12+ opted in, **Dashboard → Apply for production** and answer the questions about the test.

## Making a release

1. Bump `versionName` in `app/build.gradle.kts` when the release deserves it. The version code is set by the workflow (101, 102, …, one per run), so you never edit it.
2. Update `app/src/main/play/release-notes/<lang>/default.txt` (500 characters max). A `<track>.txt` file next to it (e.g. `alpha.txt`) wins for that track.
3. If the listing changed, edit `docs/store/listing.*.md` and run `node docs/store/sync-play.js`; CI fails if you forget.
4. Commit and push. GitHub → Actions → **Release** → Run workflow:
   - **upload**: on.
   - **track**: `internal` or `alpha`.
   - **status**: `draft` until the app's first review has passed (Play rejects anything else on a never-published app). A draft waits in Play Console for you to roll it out. Later, `completed` releases straight to the track's testers.
   - **listing**: on only when the store text changed.

## Building a signed AAB on your PC (optional)

The same environment variables the workflow sets work locally:

```bash
DALTONPICK_UPLOAD_KEYSTORE=~/daltonpick-upload.jks DALTONPICK_UPLOAD_KEY_ALIAS=upload DALTONPICK_UPLOAD_KEYSTORE_PASSWORD='…' DALTONPICK_UPLOAD_KEY_PASSWORD='…' ./gradlew bundleRelease
```

Without them, `./gradlew bundleRelease` still runs R8 and builds an unsigned AAB at `app/build/outputs/bundle/release/`. Local builds have version code 1 unless `DALTONPICK_VERSION_CODE` is set.
