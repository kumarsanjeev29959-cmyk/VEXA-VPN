const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');

const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'vexa-server-'));
process.env.VEXA_STORE_PATH = path.join(tempDir, 'state.json');
process.env.VEXA_ADMIN_KEY = 'test-admin-key';
process.env.VEXA_NODE_TOKEN = 'test-node-token';
const { handler } = require('./server');

let server;
let baseUrl;

test.before(async () => {
  server = http.createServer(handler);
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

test.after(async () => {
  await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
  fs.rmSync(tempDir, { recursive: true, force: true });
});

async function request(pathname, options = {}) {
  const response = await fetch(`${baseUrl}${pathname}`, options);
  const body = await response.json();
  return { response, body };
}

const publicKey = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=';

test('health endpoint is public', async () => {
  const { response, body } = await request('/v1/health');
  assert.equal(response.status, 200);
  assert.deepEqual(body, { status: 'ok', service: 'vexa-control-plane' });
});

test('device registration, provisioning and node acknowledgement work together', async () => {
  const registration = await request('/v1/devices', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceId: 'test-device', publicKey }),
  });
  assert.equal(registration.response.status, 201);
  assert.ok(registration.body.deviceToken);

  const unauthorized = await request('/v1/servers');
  assert.equal(unauthorized.response.status, 401);

  const admin = await request('/v1/admin/servers', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-VEXA-Admin-Key': 'test-admin-key' },
    body: JSON.stringify({
      id: 'in-01', hostname: 'vpn.example.com', port: 51820,
      publicKey, dns: '1.1.1.1', clientNetwork: '10.64.0.0/16',
      healthy: true, latencyMs: 20, loadPercent: 5,
    }),
  });
  assert.equal(admin.response.status, 201);

  const auth = { Authorization: `Bearer ${registration.body.deviceToken}` };
  const servers = await request('/v1/servers', { headers: auth });
  assert.equal(servers.response.status, 200);
  assert.equal(servers.body.servers.length, 1);
  assert.equal(servers.body.servers[0].publicKey, undefined);

  const config = await request('/v1/vpn/config', {
    method: 'POST',
    headers: { ...auth, 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceId: 'test-device', publicKey, serverId: 'in-01' }),
  });
  assert.equal(config.response.status, 200);
  assert.equal(config.body.peer.address, '10.64.0.2/32');
  assert.equal(config.body.peer.allowedIPs, '0.0.0.0/0, ::/0');

  const persisted = JSON.parse(fs.readFileSync(process.env.VEXA_STORE_PATH, 'utf8'));
  assert.equal(persisted.allocations.length, 1);
  assert.equal(persisted.allocations[0][1].address, '10.64.0.2');

  const jobs = await request('/v1/node/jobs', { headers: { 'X-VEXA-Node-Token': 'test-node-token' } });
  assert.equal(jobs.response.status, 200);
  assert.equal(jobs.body.jobs.length, 1);
  assert.equal(jobs.body.jobs[0].address, '10.64.0.2/32');

  const ack = await request('/v1/node/jobs/ack', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-VEXA-Node-Token': 'test-node-token' },
    body: JSON.stringify({ serverId: 'in-01', deviceId: 'test-device', applied: true }),
  });
  assert.equal(ack.response.status, 200);

  const jobsAfterAck = await request('/v1/node/jobs', { headers: { 'X-VEXA-Node-Token': 'test-node-token' } });
  assert.equal(jobsAfterAck.body.jobs.length, 0);
});

test('node and admin authentication reject incorrect credentials', async () => {
  const node = await request('/v1/node/jobs', { headers: { 'X-VEXA-Node-Token': 'wrong' } });
  assert.equal(node.response.status, 401);
  const admin = await request('/v1/admin/servers', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-VEXA-Admin-Key': 'wrong' },
    body: JSON.stringify({ id: 'x', hostname: 'vpn.example.com', port: 51820 }),
  });
  assert.equal(admin.response.status, 401);
});
