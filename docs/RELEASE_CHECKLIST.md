# VEXA VPN release checklist

## Free build stage

- [x] Android debug build in GitHub Actions
- [x] Android release APK build in GitHub Actions
- [x] APK and SHA-256 checksum uploaded as workflow artifacts
- [x] Backend unit tests
- [x] Backend API integration test
- [x] Persistent control-plane state
- [x] Device-bound WireGuard identity with encrypted private key storage
- [x] Peer-based provisioning contract (server never receives the client private key)

## Required before public production use

- [ ] Generate the real WireGuard server private/public key pair on the VPS
- [ ] Configure a real public VPN hostname/IP and UDP 51820
- [ ] Enable IPv4 forwarding and NAT on the VPN node
- [ ] Deploy the control plane behind HTTPS
- [ ] Configure production API URL in the Android build
- [ ] Deploy and authenticate the VEXA node agent
- [ ] Test Android -> control plane -> node agent -> WireGuard traffic end-to-end
- [ ] Add production monitoring, backups and abuse/rate-limit controls
- [ ] Create a real Android release signing key and keep it outside Git
- [ ] Build a signed release APK/AAB and perform release-device testing

## Important

The unsigned release APK produced by CI is a build artifact for development/testing. Android distribution requires a digitally signed APK/AAB. A real VPN connection also requires the production VPN node and control-plane infrastructure above.
