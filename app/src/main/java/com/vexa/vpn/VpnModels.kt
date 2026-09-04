package com.vexa.vpn

/** API model for a VPN server advertised by the control plane. */
data class VpnServer(
    val id: String,
    val name: String,
    val countryCode: String,
    val city: String,
    val hostname: String,
    val port: Int,
    val protocol: String,
    val premium: Boolean,
    val healthy: Boolean,
    val loadPercent: Int,
    val latencyMs: Int?
)

/** Device-scoped request used to obtain a short-lived WireGuard configuration. */
data class ProvisioningRequest(
    val deviceId: String,
    val publicKey: String,
    val serverId: String? = null,
    val fastest: Boolean = true
)

/** Server selection result, kept separate from network/provisioning concerns. */
data class ServerSelection(
    val server: VpnServer,
    val reason: String
)
