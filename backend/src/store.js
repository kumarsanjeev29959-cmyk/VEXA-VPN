const fs = require('node:fs');
const path = require('node:path');

const STORE_PATH = process.env.VEXA_STORE_PATH || path.join(process.cwd(), 'data', 'state.json');

function loadState() {
  try {
    const parsed = JSON.parse(fs.readFileSync(STORE_PATH, 'utf8'));
    return {
      servers: new Map(parsed.servers || []),
      devices: new Map(parsed.devices || []),
      allocations: new Map(parsed.allocations || []),
    };
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
    return { servers: new Map(), devices: new Map(), allocations: new Map() };
  }
}

function saveState(state) {
  const dir = path.dirname(STORE_PATH);
  fs.mkdirSync(dir, { recursive: true });
  const tmp = `${STORE_PATH}.tmp`;
  const payload = JSON.stringify({
    servers: [...state.servers.entries()],
    devices: [...state.devices.entries()],
    allocations: [...state.allocations.entries()],
  });
  fs.writeFileSync(tmp, payload, { encoding: 'utf8', mode: 0o600 });
  fs.renameSync(tmp, STORE_PATH);
}

module.exports = { loadState, saveState, STORE_PATH };
