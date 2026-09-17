const test = require('node:test');
const assert = require('node:assert/strict');
const { allocateAddress, parseClientNetwork } = require('./address-pool');

test('allocates the first usable address in the 10.64.0.0/16 pool', () => {
  const allocations = new Map();
  assert.equal(allocateAddress('in-01', 'device-1', allocations), '10.64.0.2');
  assert.equal(allocateAddress('in-01', 'device-1', allocations), '10.64.0.2');
});

test('skips an address already allocated on the same server', () => {
  const allocations = new Map([['in-01:device-1', { serverId: 'in-01', address: '10.64.0.2' }]]);
  assert.equal(allocateAddress('in-01', 'device-2', allocations), '10.64.0.3');
});

test('moves into the next /24 after the first /24 is exhausted', () => {
  const allocations = new Map();
  for (let host = 2; host <= 254; host += 1) {
    allocations.set(`in-01:fill-${host}`, { serverId: 'in-01', address: `10.64.0.${host}` });
  }
  assert.equal(allocateAddress('in-01', 'device-next', allocations), '10.64.1.2');
});

test('keeps allocations isolated per server', () => {
  const allocations = new Map([['in-01:device-1', { serverId: 'in-01', address: '10.64.0.2' }]]);
  assert.equal(allocateAddress('us-01', 'device-1', allocations), '10.64.0.2');
});

test('rejects an unsupported client network instead of silently ignoring it', () => {
  assert.throws(() => parseClientNetwork('10.65.0.0/16'), /Only the 10\.64\.0\.0\/16 client network is supported/);
});
