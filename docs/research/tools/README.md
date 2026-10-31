# Palette review tooling

Throwaway Node scripts (no dependencies) used for `docs/research/palette-review.md`. They re-implement the engine's OKLab nearest-neighbour naming in JS and parse `Palette.kt` with a regex, so they only work while palette rows keep the `c("key", 0xRRGGBB, Family.X, "en", "es", "pt")` shape.

Fetch the data first (not committed):

```bash
curl -L https://xkcd.com/color/rgb.txt -o xkcd.txt                       # CC0
curl -L -A "DaltonPick-research" "https://pt.wikipedia.org/w/index.php?title=Lista_de_cores&action=raw" -o pt.txt
curl -L -A "DaltonPick-research" "https://es.wikipedia.org/w/index.php?title=Anexo:Colores&action=raw" -o es.txt
```

Then, from the repo root:

```bash
node docs/research/tools/analyze.js colorengine/src/main/kotlin/com/wchsoftworks/daltonpick/colorengine/Palette.kt xkcd.txt
node docs/research/tools/gen.js <Palette.kt> xkcd.txt pt.txt es.txt <out GoldenTable.kt> <out disagreements.md>
node docs/research/tools/tone2.js <Palette.kt> xkcd.txt
```

`gen.js` regenerates `colorengine/src/test/.../GoldenTable.kt`. Review the diff before committing; the accepted-key sets inside `gen.js` are judgement calls.
