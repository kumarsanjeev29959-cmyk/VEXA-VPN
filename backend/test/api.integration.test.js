const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawn } = require('node:child_process');

const VALID_KEY = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=';

async function waitForHealth(baseUrl, child, timeoutMs = 8000) {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    if (child.exitCode !== null) throw new Error(`server exited with ${child.exitCode}`);
    try {
      const response = await fetch(`${baseUrl}/v1/health`);
      if (response.ok) return;
    } catch {}
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error('server health check timed out');
}

test('control-plane provisions a device and persists its allocation', async () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'vexa-api-'));
  const port = 18000 + Math.floor(Math.random() * 1000);
  const env = {
    ...process.env,
    PORT: String(port),
    VEXA_STORE_PATH: path.join(dir, 'state.json'),
    VEXA_ADMIN_KEY: 'test-admin-key',
    VEXA_NODE_TOKEN: 'test-node-token',
    VEXA_SERVER_ID: 'test-node-1',
    VEXA_SERVER_HOST: 'vpn.example.test',
    VEXA_SERVER_PUBLIC_KEY: VALID_KEY,
  };
  const child = spawn(process.execPath, ['src/server.js'], {
    cwd: path.resolve(__dirname, '..'), env, stdio: ['ignore', 'pipe', 'pipe'],
  });

  try {
    const baseUrl = `http://127.0.0.1:${port}`;
    await waitForHealth(baseUrl, child);

    const deviceResponse = await fetch(`${baseUrl}/v1/devices`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceId: 'integration-device', publicKey: VALID_KEY }),
    });
    assert.equal(deviceResponse.status, 201);
    const device = await deviceResponse.json();
    assert.ok(device.deviceToken);

    const unauthorized = await fetch(`${baseUrl}/v1/servers`);
    assert.equal(unauthorized.status, 401);

    const serversResponse = await fetch(`${baseUrl}/v1/servers`, {
      headers: { Authorization: `Bearer ${device.deviceToken}` },
    });
    assert.equal(serversResponse.status, 200);
    const servers = await serversResponse.json();
    assert.equal(servers.servers[0].id, 'test-node-1');
    assert.equal('publicKey' in servers.servers[0], false);

    const configResponse = await fetch(`${baseUrl}/v1/vpn/config`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${device.deviceToken}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceId: 'integration-device', publicKey: VALID_KEY, fastest: true }),
    });
    assert.equal(configResponse.status, 200);
    const config = await configResponse.json();
    assert.equal(config.peer.serverPublicKey, VALID_KEY);
    assert.equal(config.peer.address, '10.64.0.2/32');

    const jobsResponse = await fetch(`${baseUrl}/v1/node/jobs`, {
      headers: { 'X-VEXA-Node-Token': 'test-node-token' },
    });
    assert.equal(jobsResponse.status, 200);
    const jobs = await jobsResponse.json();
    assert.equal(jobs.jobs.length, 1);
    assert.equal(jobs.jobs[0].publicKey, VALID_KEY);

    const ackResponse = await fetch(`${baseUrl}/v1/node/jobs/ack`, {
      method: 'POST',
      headers: { 'X-VEXA-Node-Token': 'test-node-token', 'Content-Type': 'application/json' },
      body: JSON.stringify({ serverId: 'test-node-1', deviceId: 'integration-device', applied: true }),
    });
    assert.equal(ackResponse.status, 200);

    const persisted = JSON.parse(fs.readFileSync(env.VEXA_STORE_PATH, 'utf8'));
    assert.equal(persisted.allocations.length, 1);
    assert.equal(persisted.allocations[0][1].applied, true);
  } finally {
    child.kill('SIGTERM');
    await new Promise((resolve) => child.once('exit', resolve));
    fs.rmSync(dir, { recursive: true, force: true });
  }
});
