const test = require('node:test');
const assert = require('node:assert/strict');

function allocate(serverId, deviceId, allocations) {
  const key = `${serverId}:${deviceId}`;
  if (allocations.has(key)) return allocations.get(key).address;
  const used = new Set([...allocations.values()].filter(a => a.serverId === serverId).map(a => a.address));
  for (let secondOctet = 0; secondOctet < 256; secondOctet++) {
    for (let host = 2; host <= 254; host++) {
      const address = `10.64.${secondOctet}.${host}`;
      if (!used.has(address)) {
        allocations.set(key, { serverId, deviceId, address });
        return address;
      }
    }
  }
  throw new Error('No client tunnel address is available.');
}

test('allocates the first usable address in the 10.64.0.0/16 pool', () => {
  const allocations = new Map();
  assert.equal(allocate('in-01', 'device-1', allocations), '10.64.0.2');
  assert.equal(allocate('in-01', 'device-1', allocations), '10.64.0.2');
});

test('skips an address already allocated on the same server', () => {
  const allocations = new Map([['in-01:device-1', { serverId: 'in-01', deviceId: 'device-1', address: '10.64.0.2' }]]);
  assert.equal(allocate('in-01', 'device-2', allocations), '10.64.0.3');
});

test('supports the full second octet range instead of stopping at one subnet', () => {
  const allocations = new Map([['in-01:device-1', { serverId: 'in-01', deviceId: 'device-1', address: '10.64.0.2' }]]);
  for (let host = 2; host <= 254; host++) {
    allocations.set(`in-01:fill-${host}`, { serverId: 'in-01', deviceId: `fill-${host}`, address: `10.64.0.${host}` });
  }
  assert.equal(allocate('in-01', 'device-next', allocations), '10.64.1.2');
});

test('keeps allocations isolated per server', () => {
  const allocations = new Map([['in-01:device-1', { serverId: 'in-01', deviceId: 'device-1', address: '10.64.0.2' }]]);
  assert.equal(allocate('us-01', 'device-1', allocations), '10.64.0.2');
});
