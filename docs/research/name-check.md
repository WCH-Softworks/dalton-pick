# Name check: "DaltonPick"

Check date: 2026-10-31. This is a **knockout search by an AI assistant, not legal advice and not a clearance opinion.** SPEC §12 and IMPLEMENTATION_PLAN M6 asked for USPTO, INPI (Brazil) and EUIPO searches before release. Only the USPTO could be searched properly. The other two are unverified.

## Verdict at a glance

| Office | Searched? | Result |
|---|---|---|
| **USPTO (US)** | **Yes**, against the same database that powers tmsearch.uspto.gov | No `DALTONPICK` or `DALTON PICK` mark. No live software mark with `DALTON` that is for color-vision tools. **Two worth a lawyer's glance:** `DALTON` (RMK Inc., Class 9, color-amblyopia lenses) and `DALTON` (Firstmate BV, pending, Class 9/42 software). |
| **EUIPO (EU)** | **No.** | The eSearch plus page loaded but did not actually run a search (it returned "no results" even for plain `dalton`, which is impossible). The TMview and EUIPO data endpoints rejected scripted requests. **Unverified.** |
| **INPI (Brazil)** | **No.** | The pePI search needs a session (anonymous "Continuar" click, then a form). Not run. **Unverified.** |

**Bottom line:** nothing found that blocks "DaltonPick", but the two offices that matter most for an es/pt-BR app were not searched. Do those two by hand before the store listing goes live (steps at the end; about 20 minutes).

## What was searched, and how

### USPTO
The new search site is a front end for a JSON endpoint (`https://tmsearch.uspto.gov/prod-stage-v1-0-0/tmsearch`). I queried that endpoint directly. I validated it first against a mark I knew existed (`DALTONICO`, found immediately, 976 fuzzy hits for the bare word) so a "0 hits" result for DaltonPick is meaningful.

Queries (wordmark field):

| Query | Hits |
|---|---|
| `DALTONPICK` | 0 |
| `"DALTON PICK"` | 0 |
| `DALTONLENS` | 0 |
| `DALTONISM*` | 0 |
| `DALTONIC*` | 3 (DALTONICO, two dead DALTONICS) |
| `DALTON*` | 211 (204 retrieved; 66 live, the rest dead) |

Limits: I searched words that **start with** DALTON. A mark ending in PICK ("COLORPICK"), or a different spelling ("DALTONPIK", "DALTON-PICK" as a design mark with a logo), or a phonetic neighbour was not searched except as listed. Also: logo (design) marks only show up if they have a word element indexed. State and common-law (unregistered) use is not covered at all.

### Other places
- Web searches for "DaltonPick" and "Dalton Pick": no product, app or mark with that name. Third-party trademark aggregators showed only unrelated DALTON owners.
- Google Play search for `daltonpick`: returned no usable app ids in the page I fetched, so I can't call that a clean negative.
- GitHub repository search for `daltonpick`: one unrelated repo (`daltonpickleball`).
- Not searched: domains, social handles, F-Droid, the App Store.

## Marks worth knowing about (USPTO)

All from the live USPTO database on 2026-10-31. "Live" means registered or pending.

| Mark | Serial | Status | Class | Owner | Goods | Why it matters |
|---|---|---|---|---|---|---|
| **DALTON** | 90709249 (Reg. 7094303) | Live, registered | 9 | RMK Inc. (Korea) | Spectacle and ophthalmic lenses, **"color amblyopia corrective contact lenses"**, eyeglass frames | **Closest thematic neighbour.** Same word, same field (color-vision correction), and Class 9. Different goods (hardware vs software), so confusion is not certain, but this is the one to show a trademark attorney. |
| **DALTON** | 79452873 | Live, pending (non-final action mailed) | 9, 42 | Firstmate BV (Belgium) | A/B-testing and website-optimisation software, analysis of customer behaviour | Same word on **software** in the same class. Unrelated purpose, but an examiner may cite it against any DALTON-formative software mark. Filed 2026-04-24. |
| **DALTONICO** | 79427701 | Live, registered | 14, 35, 9 | Daltonico srl (Italy) | Watches, jewellery, smartwatches (the Class 9 goods) | Spelled like the Italian/Spanish word for "colorblind". Not close to "DaltonPick" in sight or sound, and the goods are far away. Listed because it came up in the first search. |
| DALTON TECHNOLOGIES, INC. | 97911540 | **Dead** (abandoned) | 9 | Dalton Technologies, Inc. | Software for self-improvement and social networking | Dead, but shows others have tried DALTON for apps. Abandoned applications give no rights. |
| DALTON AI | 97607596 / 97977602 | One dead, one live (Class 42) | 9 / 42 | Climavision | Weather-modelling services | Unrelated. |
| DALTONICS | 75213849, 76641676 | Dead (cancelled) | 9 | Bruker | Mass-spectrometry products | Unrelated and dead. |
| DALTON, DALTON SMART LOCKS, DALTON (various) | various | Live | 9, 35, 36, 37, 42 | Various | Locks, financial and construction services, school services | Unrelated goods. |

Not US trademarks, but close on the same topic:
- **DaltonLens** (GitHub, BSD-2): an open-source desktop color tool. I found no registered mark for it, but it is real prior use of a similar *name in the same field*. See [competitors.md](competitors.md).
- **Daltonic Color Finder** (App Store, iOS, SMED SRL, Nov 2024) and **Daltonic Pointer – color name** (Google Play): apps with "Daltonic" plus a descriptive suffix in the same category. No registered marks found for either in the USPTO search (they are not US filings).

## Reading the results (my opinion, not legal advice)

1. **"Dalton" is descriptive-leaning where your users live.** In Spanish, Portuguese, French and Italian the word means color blindness, after the chemist John Dalton (es/pt/it *daltonismo*, fr *daltonisme*). In Brazil and Latin America "daltônico" is the ordinary word. So the DALTON half of the name is close to a generic word for the product's audience in two of your three languages, and the PICK half is generic for "pick a color". A mark made of two weak parts is easy to register or clear, but also **gives you little exclusive protection** against a competitor who picks "Dalton Lens" or "DaltonFinder". That already happens in the market (Daltonic Pointer, DaltonLens).
2. **Registrability.** The EUIPO and Spanish/Portuguese examiners may object to DALTON-formative marks for color-vision software as descriptive. You don't need to register to ship. If you ever do, expect to argue for the composite (and a logo), or to file in Class 9 for downloadable software with a distinctive logo.
3. **Conflict risk is mainly the RMK `DALTON` registration** (color-correction lenses) and the pending Firstmate `DALTON` application. A free, open-source app is a low-profile use, and goods differ. I would not rename on the strength of this search. But if you plan to **register** the mark, or want a clean takedown history, run it by a trademark attorney in the US and in Brazil.
4. **Using "DaltonPick" is not infringement just because a mark exists.** Likelihood of confusion depends on goods, channels and consumers. Writing "DaltonPick™" is fine; do not write ® unless you register.

## What I could not verify

- **EUIPO / EU trade marks.** `https://euipo.europa.eu/eSearch/` loads, but opening a deep link with a search term does not run the search. It displayed "no results" for `dalton`, `daltonic` and `dalton pick`, and `dalton` alone certainly has hits, so I discarded those as non-results. I did not click the Search button (submitting a form on a third-party site) or accept its cookie banner. TMview (`tmdn.org`) and the EUIPO data endpoints answered scripted requests with "Request Rejected"; I did not try to get around that.
- **INPI Brazil.** `busca.inpi.gov.br/pePI` is a session-based search that wants an anonymous login and form entries. Not run. Brazil also uses Nice classes, so check 9 (software), 42 (SaaS and software development), 44 (medical, if any claim is made) and 35.
- **Anything unregistered** (common-law use, app-store names, domains, social handles, Ko-fi/GitHub namespaces).
- **Design marks and phonetic neighbours** ("DALTONPIK", "DALTON PIC", "DALTONPICK" as a logo).
- **Whether the USPTO endpoint is complete.** It is the public search's own backend, but I can't prove coverage of every record; the 204 of 211 `DALTON*` retrieved is because of how I paged the results.

## How to finish the check yourself (20 minutes)

1. **EUIPO:** open <https://euipo.europa.eu/eSearch/>. Type `daltonpick`, press **Search**. Then `dalton pick`, `daltonic`, `dalton*`. For `dalton*`, use Advanced search → Nice class 9 or 42, status "Registered" or "Filed".
2. **TMview** <https://www.tmdn.org/tmview/>: same terms, with offices EU (EM), BR (if listed), ES, PT, MX. TMview aggregates many national offices in one search, which covers Spain, Portugal and others cheaply.
3. **INPI Brazil:** <https://busca.inpi.gov.br/pePI/> → "Continuar" for anonymous search → *Marca* → search by **Marca** (radical `DALTON*`, then `DALTONPICK`) and by **Classe** 9 and 42.
4. **WIPO Global Brand Database** <https://branddb.wipo.int/>: one search across many offices, including Madrid (which is how both `DALTON` Firstmate and `DALTONICO` arrived in the US).
5. Domains and handles: `daltonpick.com`, `.app`, `.org`; GitHub, Ko-fi, Play Console developer name.

Tell me what you find, or give me permission to run the EUIPO and TMview searches in the built-in browser by pressing the Search button, and I'll record the results here.

## Recommendations

- Keep the name for now. Nothing found blocks it.
- Before the Play listing goes live: finish EUIPO + INPI (steps above) and record the date and queries in this file.
- Don't use "Dalton" alone, "DaltonLens" or "Daltonic" in the listing's keywords or copy. It invites confusion with the neighbours above.
- If you want protection, plan a **logo** and file for the composite. Keep "free, no ads, open source" as the differentiator; the name alone is weak.
- If you want to remove the question entirely, a more distinctive name works better than any search. That is a product decision for you, not something this scan settles.

## Sources

- USPTO trademark search (backend of <https://tmsearch.uspto.gov/>), queried 2026-10-31.
- [Dalton Technologies, Inc. record (Justia)](https://trademarks.justia.com/979/11/dalton-technologies-97911540.html), cross-checked against the USPTO endpoint.
- [DALTONICO record (TrademarkDir)](https://trademarkdir.com/trademark/daltonico-79427701), cross-checked against the USPTO endpoint.
- [DaltonLens on GitHub](https://github.com/DaltonLens/DaltonLens)
- [Daltonic Color Finder, App Store](https://apps.apple.com/app/id6737409303)
