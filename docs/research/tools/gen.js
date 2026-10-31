const fs = require('fs');
const L = require('./lib.js');
const W = require('./wiki.js');
const [palettePath, xkcdPath, ptPath, esPath, outKt, outMd] = process.argv.slice(2);
const P = L.loadPalette(palettePath);
const X = L.loadXkcd(xkcdPath);
const byKey = Object.fromEntries(P.map(e => [e.key, e]));
const hex6 = n => n.toString(16).toUpperCase().padStart(6, '0');

const cases = []; // {rgb, keys:[], source}
const fails = []; // same + got
const seen = new Set();
function add(rgb, keys, source) {
  keys.forEach(k => { if (!byKey[k]) throw new Error('unknown key ' + k + ' for ' + source); });
  const id = rgb + '|' + keys.join('|');
  if (seen.has(id)) return;
  seen.add(id);
  const m = L.match(P, rgb);
  const ok = keys.includes(m.e.key);
  (ok ? cases : fails).push({ rgb, keys, source, got: m.e.key + (m.tone ? ' · ' + m.tone : ''), d: m.d });
}

// 1. XKCD survey (CC0): the hex is where respondents' answers for that name peak.
const xk = {
  purple: 'purple', green: 'green', blue: 'blue', red: 'red', orange: 'orange', yellow: 'yellow', teal: 'teal',
  cyan: 'cyan', violet: 'violet', turquoise: 'turquoise', lavender: 'lavender', tan: 'tan', mauve: 'mauve',
  maroon: 'burgundy', olive: 'olive_green', salmon: 'salmon', black: 'black', white: 'white', grey: 'gray',
  'sky blue': 'sky_blue', 'light green': 'light_green', 'dark green': 'dark_green|very_dark_green',
  'dark blue': 'navy_blue|dark_navy', 'dark purple': 'dark_purple', 'bright green': 'bright_green',
  periwinkle: 'periwinkle', indigo: 'indigo', mustard: 'mustard', burgundy: 'burgundy', 'light brown': 'light_brown',
  lime: 'lime', 'lime green': 'lime|bright_green', 'forest green': 'forest_green|dark_green', 'navy blue': 'navy_blue|dark_navy',
  navy: 'navy_blue|dark_navy', 'midnight blue': 'midnight_blue|dark_navy', 'dark navy': 'dark_navy', 'olive green': 'olive_green',
  'hot pink': 'hot_pink|deep_pink', 'very dark green': 'very_dark_green', 'dark red': 'dark_red', 'dark orange': 'burnt_orange',
  'dark yellow': 'mustard|dark_yellow', 'pale yellow': 'pale_yellow', 'light orange': 'light_orange', 'light grey': 'light_gray',
  'dark grey': 'dark_gray|very_dark_gray', 'dark brown': 'dark_brown|very_dark_brown', 'dark magenta': 'dark_magenta',
  'bright purple': 'bright_purple', 'blue violet': 'blue_violet', 'sea green': 'sea_green|seafoam|light_green',
  'army green': 'army_green|dark_olive', 'light blue': 'sky_blue|light_blue', 'light pink': 'pale_pink|pink',
  'baby blue': 'sky_blue|light_blue', 'baby pink': 'pink|pale_pink', peach: 'peach|light_orange', coral: 'coral_red|coral|tomato_red',
  cream: 'cream|pale_yellow', ivory: 'cream', eggshell: 'cream', 'neon green': 'bright_green', 'electric blue': 'royal_blue|bright_blue',
  'bright red': 'red', 'bright yellow': 'yellow', 'bright blue': 'royal_blue|bright_blue', azure: 'bright_blue', aqua: 'turquoise|seafoam|cyan',
  fuchsia: 'magenta|dark_magenta', 'dark teal': 'petrol_blue|teal', 'dusty rose': 'dusty_rose|rose_gold|mauve', moss: 'moss_green',
  sage: 'sage_green', 'steel blue': 'steel_blue|slate_gray', jade: 'jade', emerald: 'emerald|jade', amber: 'amber', tangerine: 'orange',
  rust: 'rust|brick_red|burnt_orange', 'brick red': 'brick_red|dark_red', crimson: 'crimson|dark_red', raspberry: 'raspberry', wine: 'wine|burgundy',
  taupe: 'taupe|camel|tan', charcoal: 'charcoal|very_dark_gray|dark_gray', silver: 'silver|light_gray', 'light turquoise': 'aquamarine|seafoam|pale_turquoise',
};
for (const [name, keys] of Object.entries(xk)) {
  const x = X.find(x => x.name === name);
  if (!x) { console.error('xkcd missing', name); continue; }
  add(x.rgb, keys.split('|'), `XKCD colour survey rgb.txt (CC0): "${name}" #${hex6(x.rgb)}`);
}

// 2. CSS named colours whose name matches one of ours (W3C CSS Color 4).
const css = {
  forestgreen: ['228B22', 'forest_green'], midnightblue: ['191970', 'midnight_blue'], navy: ['000080', 'navy_blue'],
  royalblue: ['4169E1', 'royal_blue'], steelblue: ['4682B4', 'steel_blue'], skyblue: ['87CEEB', 'sky_blue'],
  lightblue: ['ADD8E6', 'light_blue'], lightgreen: ['90EE90', 'light_green'], lightcyan: ['E0FFFF', 'light_cyan'],
  darkred: ['8B0000', 'dark_red'], hotpink: ['FF69B4', 'hot_pink'], deeppink: ['FF1493', 'deep_pink'],
  indigo: ['4B0082', 'indigo'], lavender: ['E6E6FA', 'lavender'], magenta: ['FF00FF', 'magenta'], fuchsia: ['FF00FF', 'magenta'],
  orchid: ['DA70D6', 'orchid'], salmon: ['FA8072', 'salmon'], tan: ['D2B48C', 'tan'], teal: ['008080', 'teal'],
  wheat: ['F5DEB3', 'wheat'], beige: ['F5F5DC', 'beige'], coral: ['FF7F50', 'coral'], crimson: ['DC143C', 'crimson'],
  cyan: ['00FFFF', 'cyan'], aqua: ['00FFFF', 'cyan'], red: ['FF0000', 'red'], blue: ['0000FF', 'blue'], yellow: ['FFFF00', 'yellow'],
  black: ['000000', 'black'], white: ['FFFFFF', 'white'], gray: ['808080', 'gray'], grey: ['808080', 'gray'],
  green: ['008000', 'green|dark_green'], purple: ['800080', 'purple'], lime: ['00FF00', 'bright_green'], darkgreen: ['006400', 'dark_green'],
  turquoise: ['40E0D0', 'turquoise'], pink: ['FFC0CB', 'pink'], seagreen: ['2E8B57', 'sea_green'], slategray: ['708090', 'slate_gray'],
  gainsboro: ['DCDCDC', 'light_gray'], olive: ['808000', 'olive_green'], maroon: ['800000', 'dark_red|burgundy'], orange: ['FFA500', 'orange'],
  gold: ['FFD700', 'golden_yellow'], aquamarine: ['7FFFD4', 'aquamarine'], slateblue: ['6A5ACD', 'slate_blue'], chocolate: ['D2691E', 'cinnamon'],
  mediumblue: ['0000CD', 'blue|royal_blue|navy_blue'], darkblue: ['00008B', 'navy_blue'], limegreen: ['32CD32', 'bright_green|lime|grass_green|green'],
  darkolivegreen: ['556B2F', 'dark_olive'], lightyellow: ['FFFFE0', 'cream|pale_yellow'], lightpink: ['FFB6C1', 'pink|pale_pink'],
  lightgray: ['D3D3D3', 'light_gray'], silver: ['C0C0C0', 'silver|light_gray'], dimgray: ['696969', 'dark_gray|gray'],
  violet: ['EE82EE', 'orchid|mauve|lilac'], plum: ['DDA0DD', 'lilac|mauve|orchid'], thistle: ['D8BFD8', 'light_purple|lilac|lavender'],
  khaki: ['F0E68C', 'pale_yellow|wheat|khaki'], tomato: ['FF6347', 'tomato_red'], firebrick: ['B22222', 'brick_red|dark_red'],
  sienna: ['A0522D', 'light_brown|chestnut|brown|brick_red'], saddlebrown: ['8B4513', 'light_brown|brown|chestnut|chocolate_brown'],
  mintcream: ['F5FFFA', 'white|off_white'], ivory: ['FFFFF0', 'white|off_white|cream'], snow: ['FFFAFA', 'white'],
};
for (const [name, [h, keys]] of Object.entries(css)) {
  const ks = keys.split('|').filter(k => byKey[k]);
  if (!ks.length) continue;
  add(parseInt(h, 16), ks, `CSS Color 4 named colour "${name}" #${h}`);
}

// 3. Spanish / Portuguese Wikipedia colour lists: exact name matches with our localised names.
const norm = W.norm;
for (const [lang, k, list, label] of [
  ['pt', 'pt', W.parsePt(ptPath), 'pt.wikipedia "Lista de cores"'],
  ['es', 'es', W.parseEs(esPath), 'es.wikipedia "Anexo:Colores"'],
]) {
  for (const x of list) {
    const e = P.find(e => norm(e[k]) === norm(x.name));
    if (!e) continue;
    add(x.rgb, [e.key], `${label}: "${x.name}" #${hex6(x.rgb)} (${lang})`);
  }
}

// 4. Neutrals used by real UIs (Material Design grey scale and dark-theme surfaces).
const neutrals = [
  ['121212', 'very_dark_gray|black', 'Material dark-theme surface'], ['1F1F1F', 'very_dark_gray', 'common dark-theme card'],
  ['212121', 'very_dark_gray', 'Material grey 900'], ['424242', 'dark_gray|very_dark_gray', 'Material grey 800'],
  ['616161', 'dark_gray|gray', 'Material grey 700'], ['757575', 'gray|dark_gray', 'Material grey 600'],
  ['9E9E9E', 'gray|silver', 'Material grey 500'], ['BDBDBD', 'silver|light_gray', 'Material grey 400'],
  ['E0E0E0', 'light_gray', 'Material grey 300'], ['EEEEEE', 'light_gray|off_white', 'Material grey 200'],
  ['F5F5F5', 'off_white|white', 'Material grey 100'], ['FAFAFA', 'white|off_white', 'Material grey 50'],
  ['FEFEFE', 'white', 'near-white'], ['010101', 'black', 'near-black'], ['2B2B2B', 'very_dark_gray', 'IDE dark theme grey'],
];
for (const [h, keys, src] of neutrals) add(parseInt(h, 16), keys.split('|'), `${src} #${h}`);

// 5. Existing hand-written goldens keep their own section in ColorNamerTest.

// family-level cases: Material Design 500 swatches
const fam = [
  ['F44336', 'RED', 'Material red 500'], ['E91E63', 'PINK|RED', 'Material pink 500'], ['9C27B0', 'PURPLE', 'Material purple 500'],
  ['673AB7', 'PURPLE', 'Material deep purple 500'], ['3F51B5', 'BLUE|PURPLE', 'Material indigo 500'], ['2196F3', 'BLUE', 'Material blue 500'],
  ['03A9F4', 'BLUE|TEAL', 'Material light blue 500'], ['00BCD4', 'TEAL|BLUE', 'Material cyan 500'], ['009688', 'TEAL|GREEN', 'Material teal 500'],
  ['4CAF50', 'GREEN', 'Material green 500'], ['8BC34A', 'GREEN', 'Material light green 500'], ['FFEB3B', 'YELLOW', 'Material yellow 500'],
  ['FFC107', 'YELLOW|ORANGE', 'Material amber 500'], ['FF9800', 'ORANGE', 'Material orange 500'], ['FF5722', 'ORANGE|RED', 'Material deep orange 500'],
  ['795548', 'BROWN', 'Material brown 500'], ['9E9E9E', 'GRAY', 'Material grey 500'], ['607D8B', 'GRAY|BLUE', 'Material blue grey 500'],
  ['D32F2F', 'RED', 'Material red 700'], ['1976D2', 'BLUE', 'Material blue 700'], ['388E3C', 'GREEN', 'Material green 700'],
  ['F57C00', 'ORANGE', 'Material orange 700'], ['7B1FA2', 'PURPLE', 'Material purple 700'], ['FFCDD2', 'PINK|RED', 'Material red 100'],
  ['BBDEFB', 'BLUE', 'Material blue 100'], ['C8E6C9', 'GREEN', 'Material green 100'], ['FFF9C4', 'YELLOW', 'Material yellow 100'],
];
const famCases = [], famFails = [];
for (const [h, fs_, src] of fam) {
  const m = L.match(P, parseInt(h, 16));
  const r = { rgb: parseInt(h, 16), fams: fs_.split('|'), source: `${src} #${h}`, got: m.e.fam + ' (' + m.e.key + ')' };
  (r.fams.includes(m.e.fam) ? famCases : famFails).push(r);
}

// 6. Wikipedia names that are "<basic colour> [light/dark/vivid]" must land in that family.
const head = {
  pt: { verde: 'GREEN|TEAL', azul: 'BLUE|TEAL', vermelho: 'RED', amarelo: 'YELLOW', roxo: 'PURPLE', rosa: 'PINK', marrom: 'BROWN', laranja: 'ORANGE', cinza: 'GRAY|BLACK|WHITE' },
  es: { verde: 'GREEN|TEAL', azul: 'BLUE|TEAL', rojo: 'RED', amarillo: 'YELLOW', morado: 'PURPLE', rosa: 'PINK', marron: 'BROWN', naranja: 'ORANGE', gris: 'GRAY|BLACK|WHITE' },
};
const modifier = /^(escuro|oscuro|claro|vivo|forte|fuerte|intenso|palido|brillante|medio)$/;
const seenFam = new Set();
for (const [lang, list, label] of [['pt', W.parsePt(ptPath), 'pt.wikipedia "Lista de cores"'], ['es', W.parseEs(esPath), 'es.wikipedia "Anexo:Colores"']]) {
  for (const x of list) {
    const words = W.norm(x.name).split(' ');
    const fams = head[lang][words[0]];
    if (!fams || words.length > 2 || (words.length === 2 && !modifier.test(words[1]))) continue;
    const id = lang + x.name + x.rgb;
    if (seenFam.has(id)) continue;
    seenFam.add(id);
    const m = L.match(P, x.rgb);
    const r = { rgb: x.rgb, fams: fams.split('|'), source: `${label}: "${x.name}" #${hex6(x.rgb)} (${lang}, family by head word)`, got: m.e.fam + ' (' + m.e.key + ')' };
    (r.fams.includes(m.e.fam) ? famCases : famFails).push(r);
  }
}

// emit Kotlin
const q = s => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$');
let kt = `package com.wchsoftworks.daltonpick.colorengine

/**
 * Golden RGB -> expected-name table (palette review, Palette.VERSION 2).
 * Generated from public sources; every row says where its expectation comes from.
 * A row lists the accepted palette keys, because ordinary usage allows more than one answer
 * ("dark green" and "very dark green" for #033500). See docs/research/palette-review.md.
 */
internal object GoldenTable {
    class Case(val rgb: Int, val keys: String, val source: String)
    class FamilyCase(val rgb: Int, val families: String, val source: String)

    val names: List<Case> = listOf(
`;
for (const c of cases) kt += `        Case(0x${hex6(c.rgb)}, "${c.keys.join('|')}", "${q(c.source)}"),\n`;
kt += `    )\n\n    val families: List<FamilyCase> = listOf(\n`;
for (const c of famCases) kt += `        FamilyCase(0x${hex6(c.rgb)}, "${c.fams.join('|')}", "${q(c.source)}"),\n`;
kt += `    )\n}\n`;
fs.writeFileSync(outKt, kt);

// emit Markdown (disagreements + counts)
let md = `Generated cases: ${cases.length} name cases + ${famCases.length} family cases passing; ${fails.length} name + ${famFails.length} family disagreements.\n\n`;
md += `### Disagreements (source says X, engine says Y)\n\n| Colour | Source expectation | Engine says | Distance | Source |\n|---|---|---|---|---|\n`;
for (const f of fails.sort((a, b) => b.d - a.d)) md += `| \`#${hex6(f.rgb)}\` | ${f.keys.map(k => byKey[k].en).join(' / ')} | ${f.got} | ${f.d.toFixed(3)} | ${f.source.replace(/\|/g, '/')} |\n`;
for (const f of famFails) md += `| \`#${hex6(f.rgb)}\` | family ${f.fams.join('/')} | ${f.got} | | ${f.source} |\n`;
fs.writeFileSync(outMd, md);
const bySrc = {};
for (const c of cases) { const k = c.source.split(/[:"(]/)[0].trim(); bySrc[k] = (bySrc[k] || 0) + 1; }
console.log(JSON.stringify({ pass: cases.length, fail: fails.length, fam: famCases.length, famFail: famFails.length, bySrc }, null, 1));
