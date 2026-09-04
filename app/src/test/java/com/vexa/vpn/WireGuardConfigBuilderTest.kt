package com.vexa.vpn

import org.junit.Assert.assertTrue
import org.junit.Test

class WireGuardConfigBuilderTest {
    @Test
    fun buildsClientConfigWithoutChangingPeerFields() {
        val server = VpnServer(
            id = "in-01",
            name = "VEXA India 01",
            countryCode = "IN",
            city = "Mumbai",
            hostname = "vpn.example.test",
            port = 51820,
            protocol = "wireguard",
            premium = false,
            healthy = true,
            loadPercent = 10,
            latencyMs = 25,
        )
        val response = VpnConfigResponse(
            server = server,
            peer = VpnPeerConfig(
                serverPublicKey = "SERVER_PUBLIC_KEY",
                address = "10.64.0.2/32",
                dns = "1.1.1.1",
                allowedIPs = "0.0.0.0/0, ::/0",
            ),
            expiresAt = "2099-01-01T00:00:00Z",
        )

        val config = WireGuardConfigBuilder.build("CLIENT_PRIVATE_KEY", response)

        assertTrue(config.contains("PrivateKey = CLIENT_PRIVATE_KEY"))
        assertTrue(config.contains("Address = 10.64.0.2/32"))
        assertTrue(config.contains("DNS = 1.1.1.1"))
        assertTrue(config.contains("PublicKey = SERVER_PUBLIC_KEY"))
        assertTrue(config.contains("AllowedIPs = 0.0.0.0/0, ::/0"))
        assertTrue(config.contains("Endpoint = vpn.example.test:51820"))
        assertTrue(config.contains("PersistentKeepalive = 25"))
    }
}
