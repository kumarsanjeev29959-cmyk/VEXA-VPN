# VEXA VPN Backend

This directory defines the production backend boundary for VEXA VPN.

## Responsibilities

- Authentication and device registration
- VPN server catalog and health state
- Subscription/plan enforcement
- Per-device WireGuard configuration allocation
- Revocation of devices and sessions

## Security rules

- Never commit server private keys, JWT signing keys, database passwords, payment secrets, or cloud credentials.
- WireGuard client configuration responses must not be logged.
- Server private keys stay only in deployment secrets/secret management.
- API must require TLS in production.
- Device registration must validate public keys and authenticate the account.

## API contract

The initial HTTP contract is in `docs/api/v1-openapi.yaml`.

## Deployment note

The backend is intentionally not connected to a real cloud provider yet. Production deployment requires a user-owned domain, database, VPN server infrastructure, and secret manager configuration.
