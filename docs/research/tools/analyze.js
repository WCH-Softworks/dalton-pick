const L=require('./lib.js');const P=L.loadPalette(process.argv[2]);const X=L.loadXkcd(process.argv[3]);
console.log('palette',P.length,'xkcd',X.length);
const cnt={};P.forEach(e=>cnt[e.key]=[]);
const far=[];
for(const x of X){const m=L.match(P,x.rgb);cnt[m.e.key].push(x.name);if(m.d>0.07)far.push([x.name,x.rgb.toString(16).padStart(6,'0'),m.e.en,m.d.toFixed(3)])}
console.log('\n== entries by # xkcd colors (ascending) ==');
P.map(e=>[e.key,cnt[e.key].length]).sort((a,b)=>a[1]-b[1]).forEach(([k,n])=>{ if(n<=3) console.log(k,n,cnt[k].join(', '))});
console.log('\n== top 25 entries ==');P.map(e=>[e.key,cnt[e.key].length]).sort((a,b)=>b[1]-a[1]).slice(0,25).forEach(a=>console.log(a.join(' ')));
console.log('\n== xkcd colors far from any entry (>0.07):',far.length);far.sort((a,b)=>b[3]-a[3]).slice(0,60).forEach(f=>console.log(f.join(' ')));
// nearest xkcd names for each anchor
console.log('\n== anchor -> nearest xkcd names ==');
for(const e of P){const n=X.map(x=>[x.name,L.dist(e.lab,L.lab(x.rgb))]).sort((a,b)=>a[1]-b[1]).slice(0,3);console.log(e.key.padEnd(15),e.en.padEnd(18),n.map(a=>a[0]+' '+a[1].toFixed(3)).join(' | '))}
