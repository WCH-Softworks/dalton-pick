const L=require('./lib.js');const P=L.loadPalette(process.argv[2]);const X=L.loadXkcd(process.argv[3]);
const hue=x=>(Math.atan2(x.b,x.a)*180/Math.PI+360)%360;
// cusp table: max-chroma lightness per 10-degree hue bin along the sRGB cube edges
const cusp=new Array(36).fill(null);
const edges=[];for(let t=0;t<=255;t++){for(const a of [0,255])for(const b of [0,255]){edges.push([t,a,b],[a,t,b],[a,b,t])}}
for(const [r,g,b] of edges){const x=L.lab((r<<16)|(g<<8)|b);const c=L.chroma(x);if(c<0.05)continue;const k=Math.floor(hue(x)/10)%36;if(!cusp[k]||c>cusp[k].c)cusp[k]={c,l:x.l}}
for(let k=0;k<36;k++)if(!cusp[k]){let i=k;while(!cusp[i%36])i++;cusp[k]=cusp[i%36]}
const lc=h=>{const f=h/10-0.5,i=Math.floor(f),w=f-i;const a=cusp[((i%36)+36)%36].l,b=cusp[(i+1+36)%36].l;return a*(1-w)+b*w};
const red=/^(light|dark|pale|deep|very|bright)\b/i;
function cur(m){return m.tone}
function prop(m){const x=m.x,ch=L.chroma(x);if(['GRAY','BLACK','WHITE'].includes(m.e.fam))return null;
 const dl=x.l-lc(hue(x));
 let t=null;if(dl>=0.15)t=ch<0.08?'pale':'light';else if(dl<=-0.15)t=ch>=0.15?'deep':'dark';
 if(m.e.fam==='BROWN'&&t==='dark')t=null; // brown is dark orange by definition
 if(t&&red.test(m.e.en))t=null; // the name already says it
 return t}
let c=0,p=0,redC=0,redP=0,n=0;
for(const x of X){const m=L.match(P,x.rgb);n++;const a=cur(m),b=prop(m);if(a)c++;if(b)p++;const r=red.test(m.e.en);if(a&&r)redC++;if(b&&r)redP++}
console.log({n,withToneCurrent:c,withToneProposed:p,redundantCurrent:redC,redundantProposed:redP});
console.log('cusp L by hue bin:',cusp.map((q,i)=>i*10+':'+q.l.toFixed(2)).join(' '));
for(const h of ['00FF00','01FF07','00FFFF','FFFF00','FF0000','0000FF','000080','FADADD','8B0000','ADD8E6','40E0D0','005000','4CAF50','FF9800','FFC0CB','FF69B4','E6E6FA','5C4033','B03A2E','2E8B57','87CEEB','800080','D8A1A4','191970','F5DEB3','FFD700','8B4513','FA8072']){
 const rgb=parseInt(h,16),m=L.match(P,rgb);console.log(h,m.e.en.padEnd(16),String(cur(m)).padEnd(6),'->',prop(m))}
