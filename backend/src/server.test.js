const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

function freshServerModule() {
  const storePath = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'vexa-test-')), 'state.json');
  process.env.VEXA_STORE_PATH = storePath;
  process.env.VEXA_SERVER_ID = '';
  process.env.VEXA_SERVER_HOST = '';
  const serverPath = require.resolve('./server');
  delete require.cache[serverPath];
  return { serverPath, storePath };
}

test('backend source imports successfully with no server bootstrap configuration', () => {
  const { serverPath } = freshServerModule();
  const loaded = require(serverPath);
  assert.ok(loaded);
});

test('store state is created with restrictive permissions', () => {
  const storePath = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'vexa-store-')), 'state.json');
  const { saveState, loadState } = require('./store');
  saveState({ servers: new Map(), devices: new Map(), allocations: new Map() });
  assert.deepEqual(loadState().servers.size, 0);
  assert.equal(fs.statSync(storePath).mode & 0o777, 0o600);
});
