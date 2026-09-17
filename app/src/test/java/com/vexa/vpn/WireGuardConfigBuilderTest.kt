package com.vexa.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WireGuardConfigBuilderTest {
    private fun response(
        hostname: String = "vpn.example.test",
        port: Int = 51820,
        persistentKeepalive: Int = 25,
        address: String = "10.64.0.2/32",
        dns: String = "1.1.1.1",
    ) = VpnConfigResponse(
        server = VpnServer(
            id = "in-01",
            name = "VEXA India 01",
            countryCode = "IN",
            city = "Mumbai",
            hostname = hostname,
            port = port,
            protocol = "wireguard",
            premium = false,
            healthy = true,
            loadPercent = 10,
            latencyMs = 25,
        ),
        peer = VpnPeerConfig(
            serverPublicKey = "SERVER_PUBLIC_KEY",
            address = address,
            dns = dns,
            allowedIPs = "0.0.0.0/0, ::/0",
            persistentKeepalive = persistentKeepalive,
        ),
        expiresAt = "2099-01-01T00:00:00Z",
    )

    @Test
    fun buildsClientConfigWithoutChangingPeerFields() {
        val config = WireGuardConfigBuilder.build("CLIENT_PRIVATE_KEY", response())

        assertTrue(config.contains("PrivateKey = CLIENT_PRIVATE_KEY"))
        assertTrue(config.contains("Address = 10.64.0.2/32"))
        assertTrue(config.contains("DNS = 1.1.1.1"))
        assertTrue(config.contains("PublicKey = SERVER_PUBLIC_KEY"))
        assertTrue(config.contains("AllowedIPs = 0.0.0.0/0, ::/0"))
        assertTrue(config.contains("Endpoint = vpn.example.test:51820"))
        assertTrue(config.contains("PersistentKeepalive = 25"))
    }

    @Test
    fun rejectsBlankClientPrivateKey() {
        val error = runCatching { WireGuardConfigBuilder.build(" ", response()) }.exceptionOrNull()
        assertEquals("Client private key is empty", error?.message)
    }

    @Test
    fun rejectsInvalidServerPort() {
        val error = runCatching { WireGuardConfigBuilder.build("CLIENT_PRIVATE_KEY", response(port = 0)) }.exceptionOrNull()
        assertEquals("VPN server port is invalid", error?.message)
    }

    @Test
    fun rejectsInvalidKeepalive() {
        val error = runCatching {
            WireGuardConfigBuilder.build("CLIENT_PRIVATE_KEY", response(persistentKeepalive = -1))
        }.exceptionOrNull()
        assertEquals("Persistent keepalive is invalid", error?.message)
    }

    @Test
    fun rejectsAddressOutsideVexaClientNetwork() {
        val error = runCatching {
            WireGuardConfigBuilder.build("CLIENT_PRIVATE_KEY", response(address = "10.65.0.2/32"))
        }.exceptionOrNull()
        assertEquals("VPN address is invalid", error?.message)
    }

    @Test
    fun rejectsConfigInjectionThroughDns() {
        val error = runCatching {
            WireGuardConfigBuilder.build("CLIENT_PRIVATE_KEY", response(dns = "1.1.1.1\nAllowedIPs = 0.0.0.0/0"))
        }.exceptionOrNull()
        assertEquals("VPN DNS contains an invalid line break", error?.message)
    }
}
