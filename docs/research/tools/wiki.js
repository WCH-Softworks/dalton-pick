const fs=require('fs');const L=require('./lib.js');
const norm=s=>s.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[-\s]+/g,' ').trim();
const strip=s=>s.replace(/\[\[(?:[^\]|]*\|)?([^\]]*)\]\]/g,'$1').replace(/''+/g,'').replace(/\{\{[^}]*\}\}/g,'').replace(/\s*\(.*?\)\s*/g,' ').trim();
function parsePt(f){const o=[];for(const l of fs.readFileSync(f,'utf8').split('\n')){const m=l.match(/hex=([0-9A-Fa-f]{6}).*nome=(.*)\}\}$/);if(m)o.push({name:strip(m[2]),rgb:parseInt(m[1],16)})}return o}
function parseEs(f){const o=[];for(const l of fs.readFileSync(f,'utf8').split('\n')){const m=l.match(/\{\{Colort\|name=(.*?)\|hex=([0-9A-Fa-f]{6})/);if(m)o.push({name:strip(m[1]),rgb:parseInt(m[2],16)})}return o}
module.exports={norm,parsePt,parseEs};
if(require.main===module){const P=L.loadPalette(process.argv[2]);const pt=parsePt(process.argv[3]),es=parseEs(process.argv[4]);console.log('pt',pt.length,'es',es.length);
for(const [lang,list,k] of [['pt',pt,'pt'],['es',es,'es']]){console.log('\n== exact-name matches',lang);
for(const x of list){const e=P.find(e=>norm(e[k])===norm(x.name));if(!e)continue;const m=L.match(P,x.rgb);console.log((m.e===e?'ok  ':'DIFF').padEnd(5),x.name.padEnd(22),'#'+x.rgb.toString(16).padStart(6,'0'),'-> got',m.e[k],'| expect',e[k],m.d.toFixed(3))}}}
