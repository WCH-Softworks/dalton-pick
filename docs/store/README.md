# Store and policy drafts

All drafts are in three languages: English (`en`), neutral Latin American Spanish (`es`) and Brazilian Portuguese (`pt-BR`). Nothing here is published. Native speakers should read the es and pt-BR text before release (SPEC §10).

| File | What it is |
|---|---|
| `listing.en.md`, `listing.es.md`, `listing.pt-BR.md` | Play title, short description (80 chars), full description (4,000 chars). Check limits with `node docs/store/check-limits.js`. |
| `sync-play.js` | Copies the listing text into `app/src/main/play/listings/` (en-US, es-419, pt-BR), which Gradle Play Publisher uploads. Run it after editing a listing; CI checks it. See [../RELEASE.md](../RELEASE.md). |
| `privacy-policy.en.md`, `.es.md`, `.pt-BR.md` | Privacy policy page. Host it at a stable public URL (for example GitHub Pages from this repo) and enter that URL in Play Console. |
| `accessibility-declaration.md` | Answers for the Play Console AccessibilityService declaration (Vision, target users, core feature), the reasoning, and reviewer Q&A. Answers in en (submit), es and pt-BR. |
| `declaration-video-script.md` | Shot-by-shot script for the required demo video, with English captions and es / pt-BR translations. |

## Placeholders you must fill

- `[DEVELOPER NAME]`, `[CONTACT EMAIL]`, `[DATE OF RELEASE]` in the privacy policies.
- The video link in `accessibility-declaration.md`.
- Final screenshots and the feature graphic (not drafted here).

## Things I assumed or want you to confirm

1. **Feature set.** The listings describe SPEC v0.4: lens, fine-adjust, haptics, TalkBack, camera mode. If camera mode slips out of v1, remove the camera lines from the listings and the policy. Do not publish claims for features that are not in the release.
2. **"No internet permission."** True for the M0 manifest. Re-check the release manifest (M5) before using the claim.
3. **Backups.** The M0 manifest has `android:allowBackup="true"`. The policy says Android may back up settings to the user's Google account. Setting `allowBackup="false"` in release makes that sentence unnecessary. (I did not change `app/`.)
4. **Boot receiver.** The policy lists "start at boot (optional)" per SPEC §5.5. Remove it if the receiver is cut.
5. **Medical wording.** Each listing ends with "not a medical device, does not test, diagnose or correct color vision". Keep that.
6. **Title.** "DaltonPick" alone is the brand; the suffixes ("Color Name Lens", "nombre del color", "nome da cor") are for search. Trademark status of the name is in [../research/name-check.md](../research/name-check.md): EUIPO and INPI are still unverified.
7. **Play policy on tips.** The Ko-fi link in the description is allowed under the creator-tip rule in SPEC §7 as long as nothing is unlocked. If the listing is flagged, remove the link from the listing and keep it on the website.
8. **Developer name.** The org is `WCH-Softworks` on GitHub and the package is `com.wchsoftworks`. The Play developer name and contact are your call.
9. **Keywords.** Search-term ideas are in [../research/competitors.md](../research/competitors.md). They are hypotheses, not measured volumes.
