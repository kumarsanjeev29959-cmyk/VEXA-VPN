const http = require('node:http');
const https = require('node:https');
const { execFileSync } = require('node:child_process');

const API_URL = (process.env.VEXA_API_URL || '').replace(/\/$/, '');
const NODE_TOKEN = process.env.VEXA_NODE_TOKEN || '';
const POLL_MS = Number(process.env.VEXA_NODE_POLL_MS || 5000);
const WG_INTERFACE = process.env.VEXA_WG_INTERFACE || 'wg0';
const ALLOW_INSECURE_HTTP = process.env.VEXA_ALLOW_INSECURE_HTTP === 'true';

if (!API_URL || !NODE_TOKEN) {
  console.error('VEXA_API_URL and VEXA_NODE_TOKEN are required.');
  process.exit(1);
}

const parsedApiUrl = new URL(API_URL);
if (parsedApiUrl.protocol !== 'https:' && !ALLOW_INSECURE_HTTP) {
  console.error('VEXA_API_URL must use HTTPS. Set VEXA_ALLOW_INSECURE_HTTP=true only for local development.');
  process.exit(1);
}

function request(path, method = 'GET', body) {
  return new Promise((resolve, reject) => {
    const target = new URL(`${API_URL}${path}`);
    const transport = target.protocol === 'https:' ? https : http;
    const req = transport.request({
      protocol: target.protocol,
      hostname: target.hostname,
      port: target.port || (target.protocol === 'https:' ? 443 : 80),
      path: `${target.pathname}${target.search}`,
      method,
      headers: {
        'Accept': 'application/json',
        'X-VEXA-Node-Token': NODE_TOKEN,
        ...(body ? {'Content-Type': 'application/json'} : {}),
      },
    }, res => {
      let data = '';
      res.setEncoding('utf8');
      res.on('data', chunk => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          return reject(new Error(`API HTTP ${res.statusCode}`));
        }
        try {
          resolve(JSON.parse(data));
        } catch {
          reject(new Error('Invalid API response'));
        }
      });
    });
    req.on('error', reject);
    req.setTimeout(15000, () => req.destroy(new Error('API timeout')));
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

function applyPeer(job) {
  if (!/^[A-Za-z0-9+/]{42}[AEIMQUYcgkosw48012468]=?$/.test(job.publicKey)) throw new Error('Invalid peer public key');
  if (!/^10\.64\./.test(job.address)) throw new Error('Unexpected peer address');
  execFileSync('wg', ['set', WG_INTERFACE, 'peer', job.publicKey, 'allowed-ips', job.address], { stdio: 'pipe' });
}

async function poll() {
  try {
    const { jobs = [] } = await request('/v1/node/jobs');
    for (const job of jobs) {
      try {
        applyPeer(job);
        await request('/v1/node/jobs/ack', 'POST', { serverId: job.serverId, deviceId: job.deviceId, applied: true });
        console.log(`Applied peer ${job.deviceId} on ${job.serverId}`);
      } catch (error) {
        console.error(`Peer ${job.deviceId} failed: ${error.message}`);
        await request('/v1/node/jobs/ack', 'POST', { serverId: job.serverId, deviceId: job.deviceId, applied: false }).catch(() => {});
      }
    }
  } catch (error) {
    console.error(`Node poll failed: ${error.message}`);
  } finally {
    setTimeout(poll, POLL_MS);
  }
}

poll();
