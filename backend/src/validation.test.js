const test = require('node:test');
const assert = require('node:assert/strict');
const { validatePublicKey, validateServerInput } = require('./validation');

test('accepts a valid WireGuard public key', () => {
  assert.equal(validatePublicKey('AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='), true);
});

test('rejects malformed WireGuard public keys', () => {
  assert.equal(validatePublicKey('not-a-wireguard-key'), false);
});

test('accepts a valid server definition', () => {
  assert.equal(validateServerInput({ id: 'in-01', hostname: 'vpn.example.com', port: 51820 }), null);
});

test('rejects invalid server hostnames and ports', () => {
  assert.equal(validateServerInput({ id: 'in-01', hostname: 'vpn.example.com/path', port: 51820 }), 'Server hostname is invalid.');
  assert.equal(validateServerInput({ id: 'in-01', hostname: 'vpn.example.com', port: 70000 }), 'Server port is invalid.');
});
