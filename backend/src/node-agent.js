const {execFile} = require('node:child_process');
const {promisify} = require('node:util');

const execFileAsync = promisify(execFile);
const API_BASE_URL = (process.env.VEXA_API_BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/, '');
const NODE_TOKEN = process.env.VEXA_NODE_TOKEN || '';
const WG_INTERFACE = process.env.VEXA_WG_INTERFACE || 'wg0';
const POLL_MS = Number(process.env.VEXA_NODE_POLL_MS || 5000);

if (!NODE_TOKEN) {
  console.error('VEXA_NODE_TOKEN is required.');
  process.exit(1);
}

async function api(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {'Accept': 'application/json', 'X-VEXA-Node-Token': NODE_TOKEN, ...(options.headers || {})}
  });
  const text = await response.text();
  let body = {};
  try { body = text ? JSON.parse(text) : {}; } catch { body = {}; }
  if (!response.ok) throw new Error(body.message || `HTTP ${response.status}`);
  return body;
}

async function applyPeer(job) {
  if (!job.publicKey || !job.address) throw new Error('Invalid provisioning job.');
  await execFileAsync('wg', ['set', WG_INTERFACE, 'peer', job.publicKey, 'allowed-ips', job.address]);
}

async function poll() {
  const {jobs = []} = await api('/v1/node/jobs');
  for (const job of jobs) {
    try {
      await applyPeer(job);
      await api('/v1/node/jobs/ack', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({serverId: job.serverId, deviceId: job.deviceId, applied: true})});
      console.log(`Applied peer ${job.deviceId} on ${job.serverId}`);
    } catch (error) {
      console.error(`Peer ${job.deviceId} failed: ${error.message}`);
    }
  }
}

async function main() {
  console.log(`VEXA node agent for ${WG_INTERFACE}`);
  for (;;) {
    try { await poll(); } catch (error) { console.error(`Control-plane poll failed: ${error.message}`); }
    await new Promise(resolve => setTimeout(resolve, POLL_MS));
  }
}

main().catch(error => { console.error(error); process.exit(1); });
