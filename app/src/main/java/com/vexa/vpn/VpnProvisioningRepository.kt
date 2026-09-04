package com.vexa.vpn

/** Device-scoped response returned by the provisioning service. */
data class DeviceProvisioningResponse(
    val deviceToken: String,
    val deviceId: String,
    val expiresAt: String
)

/** WireGuard configuration response for a provisioned device. */
data class VpnConfigResponse(
    val server: VpnServer,
    val config: String,
    val expiresAt: String
)

/** Network boundary for the real control-plane API. No credentials are hard-coded in the app. */
interface VpnProvisioningRepository {
    suspend fun registerDevice(identity: DeviceIdentity): Result<DeviceProvisioningResponse>
    suspend fun listServers(deviceToken: String): Result<List<VpnServer>>
    suspend fun provisionConfig(
        deviceToken: String,
        request: ProvisioningRequest
    ): Result<VpnConfigResponse>
}

/** Safe default until a real VEXA control-plane endpoint is deployed. */
class UnconfiguredProvisioningRepository : VpnProvisioningRepository {
    private fun unavailable(): Result<Nothing> =
        Result.failure(IllegalStateException("VEXA provisioning service is not configured."))

    override suspend fun registerDevice(identity: DeviceIdentity): Result<DeviceProvisioningResponse> =
        unavailable()

    override suspend fun listServers(deviceToken: String): Result<List<VpnServer>> =
        unavailable()

    override suspend fun provisionConfig(
        deviceToken: String,
        request: ProvisioningRequest
    ): Result<VpnConfigResponse> = unavailable()
}
