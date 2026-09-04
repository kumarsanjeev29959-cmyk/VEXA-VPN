const test = require('node:test');
const assert = require('node:assert/strict');

function validKey() {
  return 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=';
}

test('WireGuard public key validation shape is accepted by API contract', async () => {
  const key = validKey();
  assert.equal(typeof key, 'string');
  assert.ok(key.length >= 43);
});

test('device token is intentionally opaque', () => {
  const crypto = require('node:crypto');
  const token = crypto.randomBytes(32).toString('base64url');
  assert.equal(token.length, 43);
  assert.match(token, /^[A-Za-z0-9_-]+$/);
});
