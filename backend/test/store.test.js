const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const { loadState, saveState } = require('../src/store');

test('store round-trips servers, devices and allocations', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'vexa-store-'));
  const previous = process.env.VEXA_STORE_PATH;
  process.env.VEXA_STORE_PATH = path.join(dir, 'state.json');

  try {
    const state = {
      servers: new Map([['server-1', { id: 'server-1', healthy: true }]]),
      devices: new Map([['token-1', { deviceId: 'device-1' }]]),
      allocations: new Map([['server-1:device-1', { serverId: 'server-1', deviceId: 'device-1', address: '10.64.0.2' }]]),
    };
    saveState(state);
    const restored = loadState();

    assert.deepEqual(restored.servers.get('server-1'), state.servers.get('server-1'));
    assert.deepEqual(restored.devices.get('token-1'), state.devices.get('token-1'));
    assert.deepEqual(restored.allocations.get('server-1:device-1'), state.allocations.get('server-1:device-1'));
  } finally {
    if (previous === undefined) delete process.env.VEXA_STORE_PATH;
    else process.env.VEXA_STORE_PATH = previous;
    fs.rmSync(dir, { recursive: true, force: true });
  }
});

test('store creates a fresh empty state when file is absent', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'vexa-store-empty-'));
  const previous = process.env.VEXA_STORE_PATH;
  process.env.VEXA_STORE_PATH = path.join(dir, 'missing.json');

  try {
    const state = loadState();
    assert.equal(state.servers.size, 0);
    assert.equal(state.devices.size, 0);
    assert.equal(state.allocations.size, 0);
  } finally {
    if (previous === undefined) delete process.env.VEXA_STORE_PATH;
    else process.env.VEXA_STORE_PATH = previous;
    fs.rmSync(dir, { recursive: true, force: true });
  }
});
