// Checks Play Store length limits for docs/store/listing.*.md. Usage: node docs/store/check-limits.js
const fs = require('fs');
const path = require('path');
const limits = { Title: 30, 'Short description': 80, 'Full description': 4000, Título: 30, 'Descripción corta': 80, 'Descripción completa': 4000, 'Descrição curta': 80, 'Descrição completa': 4000 };
let bad = 0;
for (const f of fs.readdirSync(__dirname).filter(f => /^listing\..*\.md$/.test(f))) {
  const text = fs.readFileSync(path.join(__dirname, f), 'utf8');
  const parts = text.split(/^## /m).slice(1);
  for (const p of parts) {
    const [head, ...rest] = p.split('\n');
    const name = head.trim();
    if (!(name in limits)) continue;
    const body = rest.join('\n').trim();
    const n = [...body].length;
    const ok = n <= limits[name];
    if (!ok) bad++;
    console.log(`${ok ? 'ok ' : 'OVER'} ${f.padEnd(18)} ${name.padEnd(22)} ${String(n).padStart(4)}/${limits[name]}`);
  }
}
process.exit(bad ? 1 : 0);
