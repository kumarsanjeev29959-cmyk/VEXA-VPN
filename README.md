# VEXA-VPN

Premium Android VPN application built with Kotlin, Jetpack Compose and WireGuard.

## Current status

- Android app foundation and premium dark UI
- Real WireGuard userspace tunnel library integrated
- Android VPN authorization flow
- Connect/disconnect controller
- Manual WireGuard client configuration input for development
- GitHub Actions Android CI workflow

## Architecture

`app/` contains the Android client. The VPN engine uses the official WireGuard Android tunnel library from Maven Central. Server configuration is intentionally not hard-coded: production credentials and private keys must come from a secure backend or controlled provisioning flow.

## Development

A valid WireGuard client configuration can be entered from the app's **VPN SERVER CONFIG** screen. A typical configuration contains an `[Interface]` section and a `[Peer]` section with a client private key, tunnel address, peer public key, endpoint and allowed IPs.

Do not commit real private keys, API tokens, billing credentials, or production server secrets to this repository.

## Roadmap

1. Secure backend API for authentication and server/config delivery
2. Server health, latency and automatic fastest-server selection
3. Secure key provisioning and rotation
4. Kill switch / lockdown controls and DNS leak protection
5. Free and Premium plans with Google Play Billing validation
6. Account, subscription and device management
7. Production WireGuard server infrastructure and monitoring
8. Automated release signing and Play Store release pipeline

## Important

The Android tunnel engine is now wired into the app, but a production VPN service still requires provisioned WireGuard servers and a secure backend that issues per-device client configurations. Those external infrastructure credentials are not stored in this repository.
