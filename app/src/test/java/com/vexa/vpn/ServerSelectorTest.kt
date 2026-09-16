package com.vexa.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerSelectorTest {
    private fun server(
        id: String,
        healthy: Boolean = true,
        latencyMs: Int? = 50,
        loadPercent: Int = 20
    ) = VpnServer(
        id = id,
        name = id,
        countryCode = "IN",
        city = "Patna",
        hostname = "$id.vexa.test",
        port = 51820,
        protocol = "wireguard",
        premium = false,
        healthy = healthy,
        loadPercent = loadPercent,
        latencyMs = latencyMs
    )

    @Test
    fun selectsLowestHealthyLatency() {
        val result = ServerSelector.selectFastest(
            listOf(server("slow", latencyMs = 120), server("fast", latencyMs = 25))
        )
        assertEquals("fast", result?.server?.id)
    }

    @Test
    fun fallsBackToHealthyServerWhenLatencyIsUnknown() {
        val result = ServerSelector.selectFastest(
            listOf(server("unknown", latencyMs = null), server("healthy", latencyMs = null))
        )
        assertEquals("healthy", result?.server?.id)
    }

    @Test
    fun prefersMeasuredLatencyOverUnknownLatency() {
        val result = ServerSelector.selectFastest(
            listOf(server("unknown", latencyMs = null), server("measured", latencyMs = 500))
        )
        assertEquals("measured", result?.server?.id)
    }

    @Test
    fun usesLoadAndIdAsStableTieBreakers() {
        val result = ServerSelector.selectFastest(
            listOf(
                server("z", latencyMs = 30, loadPercent = 70),
                server("a", latencyMs = 30, loadPercent = 20)
            )
        )
        assertEquals("a", result?.server?.id)
    }

    @Test
    fun returnsNullWhenNoHealthyServerExists() {
        assertNull(ServerSelector.selectFastest(listOf(server("offline", healthy = false, latencyMs = 10))))
    }
}
