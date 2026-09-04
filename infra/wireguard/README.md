# VEXA WireGuard node

This directory is a deployment template, not a live VPN server.

## Required production node

- Linux VPS with a public IPv4 address
- WireGuard installed on the host
- UDP 51820 reachable from the Internet
- IPv4 forwarding enabled
- NAT/masquerading from the VEXA tunnel subnet to the node's WAN interface
- Host firewall allowing only the required management and WireGuard traffic
- Server private key generated and stored only on the node/secret manager
- Server public key registered with the VEXA control plane

## Do not commit

Never commit a populated WireGuard configuration, server private key, cloud credentials, SSH keys, or device peer secrets.

The node must eventually receive device peer changes through an authenticated provisioning channel. Until that agent/control-plane integration is deployed, `/v1/vpn/config` must remain unavailable rather than returning a configuration that cannot actually route traffic.
