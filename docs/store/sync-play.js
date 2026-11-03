// Copies the store text from docs/store/listing.*.md into app/src/main/play/listings, the folder
// Gradle Play Publisher uploads with `./gradlew publishListing`. The .md drafts stay the source.
// Usage: node docs/store/sync-play.js           (write the files)
//        node docs/store/sync-play.js --check   (fail if they are out of date; used in CI)
const fs = require('fs');
const path = require('path');

// Draft file → Play language code. Spanish is Latin American (es-419).
const languages = { 'listing.en.md': 'en-US', 'listing.es.md': 'es-419', 'listing.pt-BR.md': 'pt-BR' };
const sections = {
  'title.txt': ['Title', 'Título'],
  'short-description.txt': ['Short description', 'Descripción corta', 'Descrição curta'],
  'full-description.txt': ['Full description', 'Descripción completa', 'Descrição completa'],
};

const check = process.argv.includes('--check');
const out = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'play', 'listings');
let stale = 0;
for (const [file, lang] of Object.entries(languages)) {
  const text = fs.readFileSync(path.join(__dirname, file), 'utf8').replace(/\r\n/g, '\n');
  const parts = {};
  for (const p of text.split(/^## /m).slice(1)) {
    const [head, ...rest] = p.split('\n');
    // No trailing newline: Play counts every character against the limits.
    parts[head.trim()] = rest.join('\n').trim();
  }
  for (const [name, headings] of Object.entries(sections)) {
    const body = headings.map(h => parts[h]).find(Boolean);
    if (!body) throw new Error(`${file}: no "${headings[0]}" section`);
    const target = path.join(out, lang, name);
    const current = fs.existsSync(target) ? fs.readFileSync(target, 'utf8') : null;
    if (current === body) continue;
    stale++;
    if (check) {
      console.log(`out of date: ${path.relative(process.cwd(), target)}`);
    } else {
      fs.mkdirSync(path.dirname(target), { recursive: true });
      fs.writeFileSync(target, body);
      console.log(`wrote ${path.relative(process.cwd(), target)}`);
    }
  }
}
if (check && stale) {
  console.log('Run: node docs/store/sync-play.js');
  process.exit(1);
}
