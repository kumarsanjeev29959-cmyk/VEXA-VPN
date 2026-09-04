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

data class ProvisioningRequest(
    val deviceId: String,
    val publicKey: String,
    val serverId: String? = null,
    val fastest: Boolean = true
)

data class DeviceProvisioningResponse(
    val deviceToken: String,
    val deviceId: String,
    val expiresAt: String
)

data class VpnConfigResponse(
    val server: VpnServer,
    val config: String,
    val expiresAt: String
)

data class ServerSelection(
    val server: VpnServer,
    val reason: String
)

interface VpnProvisioningRepository {
    suspend fun registerDevice(identity: DeviceIdentity): Result<DeviceProvisioningResponse>
    suspend fun listServers(deviceToken: String): Result<List<VpnServer>>
    suspend fun provisionConfig(deviceToken: String, request: ProvisioningRequest): Result<VpnConfigResponse>
}

class UnconfiguredProvisioningRepository : VpnProvisioningRepository {
    private fun unavailable(): Result<Nothing> = Result.failure(IllegalStateException("VEXA provisioning service is not configured yet."))
    override suspend fun registerDevice(identity: DeviceIdentity) = unavailable()
    override suspend fun listServers(deviceToken: String) = unavailable()
    override suspend fun provisionConfig(deviceToken: String, request: ProvisioningRequest) = unavailable()
}
