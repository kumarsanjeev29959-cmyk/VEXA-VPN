const DEFAULT_CLIENT_NETWORK = '10.64.0.0/16';

function parseClientNetwork(value = DEFAULT_CLIENT_NETWORK) {
  const match = /^10\.64\.0\.0\/(16)$/.exec(String(value).trim());
  if (!match) throw new Error('Only the 10.64.0.0/16 client network is supported.');
  return { prefix: '10.64', first: 2, last: 254 };
}

function allocateAddress(serverId, deviceId, allocations, clientNetwork = DEFAULT_CLIENT_NETWORK) {
  const network = parseClientNetwork(clientNetwork);
  const key = `${serverId}:${deviceId}`;
  if (allocations.has(key)) return allocations.get(key).address;

  const used = new Set(
    [...allocations.values()]
      .filter(allocation => allocation.serverId === serverId)
      .map(allocation => allocation.address),
  );

  for (let thirdOctet = 0; thirdOctet < 256; thirdOctet += 1) {
    for (let fourthOctet = network.first; fourthOctet <= network.last; fourthOctet += 1) {
      const address = `${network.prefix}.${thirdOctet}.${fourthOctet}`;
      if (used.has(address)) continue;
      allocations.set(key, {
        serverId,
        deviceId,
        address,
        applied: false,
        updatedAt: new Date().toISOString(),
      });
      return address;
    }
  }

  throw new Error('No client tunnel address is available.');
}

module.exports = { DEFAULT_CLIENT_NETWORK, parseClientNetwork, allocateAddress };
