const test = require('node:test');
const assert = require('node:assert/strict');
const { validatePublicKey, validateDeviceId, validateServerInput } = require('./validation');

test('accepts a valid WireGuard public key', () => {
  assert.equal(validatePublicKey('AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='), true);
});

test('rejects malformed WireGuard public keys', () => {
  assert.equal(validatePublicKey('not-a-wireguard-key'), false);
  assert.equal(validatePublicKey('AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='), false);
  assert.equal(validatePublicKey('AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'), false);
});

test('accepts safe device ids', () => {
  assert.equal(validateDeviceId('android-01:abc_123'), true);
  assert.equal(validateDeviceId('bad device id'), false);
  assert.equal(validateDeviceId(''), false);
});

test('accepts a valid server definition', () => {
  assert.equal(validateServerInput({ id: 'in-01', hostname: 'vpn.example.com', port: 51820 }), null);
});

test('rejects invalid server hostnames and ports', () => {
  assert.equal(validateServerInput({ id: 'in-01', hostname: 'vpn.example.com/path', port: 51820 }), 'Server hostname is invalid.');
  assert.equal(validateServerInput({ id: 'in-01', hostname: 'vpn.example.com', port: 70000 }), 'Server port is invalid.');
});

test('rejects unsafe server metadata', () => {
  assert.equal(validateServerInput({ id: 'in-01', hostname: 'vpn.example.com', dns: '1.1.1.1\nAllowedIPs=x' }), 'Server DNS is invalid.');
  assert.equal(validateServerInput({ id: 'in-01', hostname: 'vpn.example.com', clientNetwork: '10.65.0.0/16' }), 'Client network is invalid.');
  assert.equal(validateServerInput({ id: 'in-01', hostname: 'vpn.example.com', loadPercent: 101 }), 'Server load is invalid.');
});
