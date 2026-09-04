# VEXA VPN Backend

This directory defines the production backend boundary for VEXA VPN.

## Product identity model

VEXA is account-less from the end user's perspective. The Android app must not require an email/password login or a Register screen.

- The device creates its WireGuard key pair locally.
- Only the device public key and minimal required device metadata are sent to the backend.
- The WireGuard private key never leaves the device.
- The backend issues device-scoped provisioning/entitlement data and can revoke a device when required.

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

## API contract

The HTTP contract is in `docs/api/v1-openapi.yaml`.

## Deployment note

The backend is intentionally not connected to a real cloud provider yet. Production deployment requires a user-owned domain, database, VPN server infrastructure, monitoring, and secret manager configuration.
