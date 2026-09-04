# VEXA-VPN

Premium Android VPN application built with Kotlin, Jetpack Compose and WireGuard.

## Current status

- Android app foundation and premium dark UI
- Real WireGuard userspace tunnel library integrated
- Android VPN authorization flow
- Connect/disconnect controller
- Manual WireGuard client configuration input for development
- GitHub Actions Android CI workflow
- Account-less product direction: no login/register screen for end users

## Architecture

`app/` contains the Android client. The VPN engine uses the official WireGuard Android tunnel library from Maven Central. Server configuration is intentionally not hard-coded: production credentials and private keys must come from a secure backend or controlled provisioning flow.

VEXA is designed around device-based identity rather than an email/password account. The device generates its WireGuard key pair locally; only the public key and required device metadata should be sent to the backend. The private key must never be uploaded to the backend.

## Development

A valid WireGuard client configuration can be entered from the app's **VPN SERVER CONFIG** screen. This is a development/provisioning tool and is not the intended final consumer flow.

Do not commit real private keys, API tokens, billing credentials, or production server secrets to this repository.

## Roadmap

1. Device-based backend provisioning with no user login/register flow
2. Server health, latency and automatic fastest-server selection
3. Secure local key storage, provisioning and rotation
4. Kill switch / lockdown controls and DNS leak protection
5. Free and Premium plans with Google Play Billing validation
6. Device, entitlement and session management without an app login screen
7. Production WireGuard server infrastructure and monitoring
8. Automated release signing and Play Store release pipeline

## Important

The Android tunnel engine is now wired into the app, but a production VPN service still requires provisioned WireGuard servers and a secure backend that issues per-device client configurations. Those external infrastructure credentials are not stored in this repository.
