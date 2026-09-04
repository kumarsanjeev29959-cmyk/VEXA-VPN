const fs = require('node:fs');
const path = require('node:path');

function storePath() {
  return process.env.VEXA_STORE_PATH || path.join(process.cwd(), 'data', 'state.json');
}

function loadState() {
  const filePath = storePath();
  try {
    const parsed = JSON.parse(fs.readFileSync(filePath, 'utf8'));
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
  const filePath = storePath();
  const dir = path.dirname(filePath);
  fs.mkdirSync(dir, { recursive: true });
  const tmp = `${filePath}.tmp`;
  const payload = JSON.stringify({
    servers: [...state.servers.entries()],
    devices: [...state.devices.entries()],
    allocations: [...state.allocations.entries()],
  });
  fs.writeFileSync(tmp, payload, { encoding: 'utf8', mode: 0o600 });
  fs.renameSync(tmp, filePath);
}

module.exports = { loadState, saveState, storePath };
