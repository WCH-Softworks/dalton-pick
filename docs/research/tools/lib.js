const fs=require('fs');
const lin=c=>{c/=255;return c<=0.04045?c/12.92:Math.pow((c+0.055)/1.055,2.4)};
function lab(rgb){const r=lin(rgb>>16&255),g=lin(rgb>>8&255),b=lin(rgb&255);
const l=Math.cbrt(0.4122214708*r+0.5363325363*g+0.0514459929*b),m=Math.cbrt(0.2119034982*r+0.6806995451*g+0.1073969566*b),s=Math.cbrt(0.0883024619*r+0.2817188376*g+0.6299787005*b);
return{l:0.2104542553*l+0.793617785*m-0.0040720468*s,a:1.9779984951*l-2.428592205*m+0.4505937099*s,b:0.0259040371*l+0.7827717662*m-0.808675766*s}}
const dist=(x,y)=>Math.hypot(x.l-y.l,x.a-y.a,x.b-y.b);
const chroma=x=>Math.hypot(x.a,x.b);
const TL={RED:.6,ORANGE:.72,YELLOW:.9,GREEN:.62,TEAL:.65,BLUE:.5,PURPLE:.5,PINK:.75,BROWN:.5,GRAY:.6,BLACK:0,WHITE:1};
function loadPalette(path){const t=fs.readFileSync(path,'utf8');const re=/c\("(\w+)",\s*0x([0-9A-Fa-f]+),\s*Family\.(\w+),\s*"([^"]*)",\s*"([^"]*)",\s*"([^"]*)"\)/g;const out=[];let m;
while((m=re.exec(t)))out.push({key:m[1],rgb:parseInt(m[2],16),fam:m[3],en:m[4],es:m[5],pt:m[6],lab:lab(parseInt(m[2],16))});return out}
const ACH=['GRAY','BLACK','WHITE'];
function match(P,rgb){const x=lab(rgb);const c=chroma(x)<0.02?P.filter(e=>ACH.includes(e.fam)):P;let b=c[0],bd=dist(x,b.lab);for(const e of c){const d=dist(x,e.lab);if(d<bd){b=e;bd=d}}
let tone=null;if(!ACH.includes(b.fam)){const dl=x.l-TL[b.fam],ch=chroma(x);tone=dl>=.15&&ch<.08?'pale':dl>=.15?'light':dl<=-.15&&ch>=.15?'deep':dl<=-.15?'dark':null}
return{e:b,d:bd,tone,x}}
function loadXkcd(p){return fs.readFileSync(p,'utf8').split('\n').filter(l=>l&&l[0]!='#').map(l=>{const [n,h]=l.split('\t');return{name:n,rgb:parseInt(h.slice(1),16)}})}
module.exports={lab,dist,chroma,loadPalette,match,loadXkcd,TL};
