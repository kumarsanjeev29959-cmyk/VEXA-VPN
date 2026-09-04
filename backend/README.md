# VEXA VPN Backend

This directory contains the first executable control-plane foundation for VEXA VPN.

## Product identity model

VEXA is account-less from the end user's perspective. The Android app must not require an email/password login or a Register screen.

- The device creates its WireGuard key pair locally.
- Only the device public key and minimal required device metadata are sent to the backend.
- The WireGuard private key never leaves the device.
- The backend issues device-scoped provisioning/entitlement data and can revoke a device when required.

## Current implementation

- Node.js 20+ HTTP control-plane server
- Device registration with opaque, expiring device tokens
- Authenticated server catalog endpoint
- Authenticated VPN-config endpoint boundary
- Protected admin server-registration endpoint
- Environment-based public server metadata bootstrap
- No server private keys or client private keys in source control

## Responsibilities

- Device registration and device-scoped authorization
- VPN server catalog and health state
- Subscription/plan enforcement
- Per-device WireGuard configuration allocation
- Device and tunnel-session revocation

## Security rules

- Never commit server private keys, device private keys, database passwords, payment secrets, or cloud credentials.
- WireGuard client configuration responses must not be logged.
- Server private keys stay only in deployment secrets/secret management.
- API must require TLS in production.
- Device registration must validate WireGuard public keys and use a device-authentication/provisioning mechanism rather than an end-user password.

## Run locally

```bash
cd backend
npm test
npm start
```

Set `VEXA_ADMIN_KEY` before using the protected admin endpoint. Optional `VEXA_SERVER_*` variables bootstrap one public server into the in-memory catalog. The sample hostname in `.env.example` is only a placeholder.

## Production gap

The current server deliberately returns `503` for `/v1/vpn/config` until a real WireGuard provisioning worker is connected. It must not fabricate tunnel credentials. Production deployment requires a user-owned domain, durable database, secret manager, real VPN nodes, monitoring, TLS, rate limiting, abuse controls, and a secure provisioning worker.

The HTTP contract remains in `docs/api/v1-openapi.yaml`.
