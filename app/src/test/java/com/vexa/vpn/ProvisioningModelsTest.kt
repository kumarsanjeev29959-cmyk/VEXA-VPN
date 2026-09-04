package com.vexa.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvisioningModelsTest {
    @Test
    fun defaultProvisioningRequestUsesFastestMode() {
        val request = ProvisioningRequest(deviceId = "device", publicKey = "public")
        assertTrue(request.fastest)
        assertEquals(null, request.serverId)
    }

    @Test
    fun serverModelKeepsHealthAndLatencyMetadata() {
        val server = VpnServer(
            id = "in-1",
            name = "India 1",
            countryCode = "IN",
            city = "Mumbai",
            hostname = "in-1.vexa.example",
            port = 51820,
            protocol = "wireguard",
            premium = true,
            healthy = false,
            loadPercent = 91,
            latencyMs = null
        )
        assertFalse(server.healthy)
        assertEquals(91, server.loadPercent)
        assertEquals(null, server.latencyMs)
        assertTrue(server.premium)
    }
}
