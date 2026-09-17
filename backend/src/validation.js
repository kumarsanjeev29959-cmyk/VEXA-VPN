const WIREGUARD_PUBLIC_KEY = /^[A-Za-z0-9+/]{43}=$/;
const DEVICE_ID = /^[A-Za-z0-9._:-]{1,128}$/;
const CLIENT_NETWORK = /^10\.64\.0\.0\/16$/;

function validatePublicKey(value) {
  if (typeof value !== 'string' || !WIREGUARD_PUBLIC_KEY.test(value)) return false;
  try {
    const decoded = Buffer.from(value, 'base64');
    return decoded.length === 32 && decoded.toString('base64') === value;
  } catch {
    return false;
  }
}

function validateDeviceId(value) {
  return typeof value === 'string' && DEVICE_ID.test(value);
}

function validateServerInput(body) {
  if (!body || typeof body !== 'object') return 'Server payload is required.';
  if (!/^[A-Za-z0-9._-]{1,64}$/.test(String(body.id || ''))) return 'Server id is invalid.';
  if (!/^[A-Za-z0-9.-]{1,253}$/.test(String(body.hostname || ''))) return 'Server hostname is invalid.';
  const port = Number(body.port ?? 51820);
  if (!Number.isInteger(port) || port < 1 || port > 65535) return 'Server port is invalid.';
  if (body.publicKey !== undefined && body.publicKey !== '' && !validatePublicKey(body.publicKey)) {
    return 'Server public key is invalid.';
  }
  if (body.dns !== undefined && (typeof body.dns !== 'string' || body.dns.length > 253 || /[\r\n]/.test(body.dns))) {
    return 'Server DNS is invalid.';
  }
  if (body.clientNetwork !== undefined && !CLIENT_NETWORK.test(String(body.clientNetwork))) {
    return 'Client network is invalid.';
  }
  if (body.healthy !== undefined && typeof body.healthy !== 'boolean') return 'Server health flag is invalid.';
  if (body.loadPercent !== undefined && (!Number.isFinite(Number(body.loadPercent)) || Number(body.loadPercent) < 0 || Number(body.loadPercent) > 100)) {
    return 'Server load is invalid.';
  }
  if (body.latencyMs !== undefined && (!Number.isFinite(Number(body.latencyMs)) || Number(body.latencyMs) < 0)) {
    return 'Server latency is invalid.';
  }
  return null;
}

module.exports = { validatePublicKey, validateDeviceId, validateServerInput };
