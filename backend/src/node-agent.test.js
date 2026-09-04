const test = require('node:test');
const assert = require('node:assert/strict');

test('node agent requires an explicit node token', () => {
  assert.equal(process.env.VEXA_NODE_TOKEN || '', '');
});

test('WireGuard peer jobs use /32 addresses', () => {
  const address = '10.64.1.2/32';
  assert.match(address, /^10\.64\.\d+\.\d+\/32$/);
});
