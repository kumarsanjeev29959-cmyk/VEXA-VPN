const WIREGUARD_PUBLIC_KEY = /^[A-Za-z0-9+/]{43}=$/;

function validatePublicKey(value) {
  if (typeof value !== 'string' || !WIREGUARD_PUBLIC_KEY.test(value)) return false;
  try {
    const decoded = Buffer.from(value, 'base64');
    return decoded.length === 32 && decoded.toString('base64') === value;
  } catch {
    return false;
  }
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
  return null;
}

module.exports = { validatePublicKey, validateServerInput };
