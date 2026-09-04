const http = require('node:http');
const crypto = require('node:crypto');

const PORT = Number(process.env.PORT || 8080);
const ADMIN_KEY = process.env.VEXA_ADMIN_KEY || '';
const DEVICE_TOKEN_TTL_MS = 24 * 60 * 60 * 1000;
const CONFIG_TTL_MS = 60 * 60 * 1000;
const servers = new Map();
const devices = new Map();
const allocations = new Map();

function json(res, status, body) {
  const payload = JSON.stringify(body);
  res.writeHead(status, {'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','Content-Length':Buffer.byteLength(payload)});
  res.end(payload);
}
function token() { return crypto.randomBytes(32).toString('base64url'); }
function readJson(req) {
  return new Promise((resolve,reject)=>{
    let data='';
    req.on('data', chunk=>{ data+=chunk; if(data.length>64*1024) reject(new Error('Request body too large')); });
    req.on('end',()=>{ if(!data) return resolve({}); try{resolve(JSON.parse(data));}catch{reject(new Error('Invalid JSON'));} });
    req.on('error',reject);
  });
}
function bearer(req){ const value=req.headers.authorization||''; return value.startsWith('Bearer ')?value.slice(7):''; }
function requireDevice(req,res){
  const value=bearer(req); const device=devices.get(value);
  if(!device || Date.parse(device.expiresAt)<=Date.now()){ json(res,401,{message:'Invalid or expired device token.'}); return null; }
  return device;
}
function validatePublicKey(value){ return typeof value==='string' && /^[A-Za-z0-9+/]{42}[AEIMQUYcgkosw48012468]=?$/.test(value); }
function healthyServers(){ return [...servers.values()].filter(s=>s.healthy); }
function fastestServer(){ return healthyServers().sort((a,b)=>(a.latencyMs??Number.MAX_SAFE_INTEGER)-(b.latencyMs??Number.MAX_SAFE_INTEGER)||a.loadPercent-b.loadPercent||a.id.localeCompare(b.id))[0]; }
function bootstrapServerFromEnv(){
  if(!process.env.VEXA_SERVER_ID || !process.env.VEXA_SERVER_HOST) return;
  servers.set(process.env.VEXA_SERVER_ID,{id:process.env.VEXA_SERVER_ID,name:process.env.VEXA_SERVER_NAME||'VEXA Node 1',countryCode:process.env.VEXA_SERVER_COUNTRY||'IN',city:process.env.VEXA_SERVER_CITY||'Mumbai',hostname:process.env.VEXA_SERVER_HOST,port:Number(process.env.VEXA_SERVER_PORT||51820),protocol:'wireguard',premium:false,healthy:true,loadPercent:0,latencyMs:Number(process.env.VEXA_SERVER_LATENCY_MS||50),publicKey:process.env.VEXA_SERVER_PUBLIC_KEY||'',dns:process.env.VEXA_SERVER_DNS||'1.1.1.1',clientNetwork:process.env.VEXA_CLIENT_NETWORK||'10.64.0.0/16'});
}
function allocateAddress(serverId,deviceId){
  const key=`${serverId}:${deviceId}`; if(allocations.has(key)) return allocations.get(key);
  const used=new Set([...allocations.entries()].filter(([k])=>k.startsWith(`${serverId}:`)).map(([,v])=>v));
  for(let i=2;i<65535;i++){ const address=`10.64.${Math.floor(i/254)}.${i%254}`; if(!used.has(address)){ allocations.set(key,address); return address; } }
  throw new Error('No client tunnel address is available.');
}
async function handler(req,res){
  const url=new URL(req.url,`http://${req.headers.host||'localhost'}`);
  if(req.method==='GET'&&url.pathname==='/v1/health') return json(res,200,{status:'ok',service:'vexa-control-plane'});
  if(req.method==='POST'&&url.pathname==='/v1/devices'){
    try{ const body=await readJson(req); if(!body.deviceId||!validatePublicKey(body.publicKey)) return json(res,400,{message:'deviceId and a valid WireGuard public key are required.'});
      const existing=[...devices.values()].find(d=>d.deviceId===body.deviceId); if(existing)return json(res,200,existing.response);
      const deviceToken=token(); const expiresAt=new Date(Date.now()+DEVICE_TOKEN_TTL_MS).toISOString(); const response={deviceToken,deviceId:body.deviceId,expiresAt};
      devices.set(deviceToken,{deviceId:body.deviceId,publicKey:body.publicKey,expiresAt,response}); return json(res,201,response);
    }catch(error){return json(res,400,{message:error.message});}
  }
  if(req.method==='GET'&&url.pathname==='/v1/servers'){if(!requireDevice(req,res))return; return json(res,200,{servers:[...servers.values()].map(({publicKey,dns,clientNetwork,...publicServer})=>publicServer)});}
  if(req.method==='POST'&&url.pathname==='/v1/vpn/config'){
    const device=requireDevice(req,res); if(!device)return;
    try{ const body=await readJson(req); if(body.deviceId!==device.deviceId||body.publicKey!==device.publicKey||!validatePublicKey(body.publicKey))return json(res,403,{message:'Device identity does not match.'});
      const server=body.serverId?servers.get(body.serverId):fastestServer();
      if(!server||!server.healthy)return json(res,503,{message:'No healthy VPN server is available.'});
      if(!server.publicKey)return json(res,503,{message:'VPN node public key is not configured.'});
      const address=allocateAddress(server.id,device.deviceId);
      return json(res,200,{server:{id:server.id,name:server.name,countryCode:server.countryCode,city:server.city,hostname:server.hostname,port:server.port,protocol:server.protocol,premium:server.premium,healthy:server.healthy,loadPercent:server.loadPercent,latencyMs:server.latencyMs},peer:{serverPublicKey:server.publicKey,address:`${address}/32`,dns:server.dns,allowedIPs:'0.0.0.0/0, ::/0',persistentKeepalive:25},expiresAt:new Date(Date.now()+CONFIG_TTL_MS).toISOString()});
    }catch(error){return json(res,400,{message:error.message});}
  }
  if(req.method==='POST'&&url.pathname==='/v1/admin/servers'){
    if(!ADMIN_KEY||req.headers['x-vexa-admin-key']!==ADMIN_KEY)return json(res,401,{message:'Unauthorized.'});
    try{const body=await readJson(req); if(!body.id||!body.hostname)return json(res,400,{message:'id and hostname are required.'}); servers.set(body.id,{...body,protocol:'wireguard',healthy:body.healthy!==false}); return json(res,201,{ok:true});}
    catch(error){return json(res,400,{message:error.message});}
  }
  return json(res,404,{message:'Not found.'});
}
bootstrapServerFromEnv();
http.createServer(handler).listen(PORT,()=>console.log(`VEXA control plane listening on ${PORT}`));
