package com.vexa.vpn

object WireGuardConfigBuilder {
    private fun requireSingleLine(name: String, value: String) {
        require(value.isNotBlank()) { "$name is empty" }
        require('\n' !in value && '\r' !in value) { "$name contains an invalid line break" }
    }

    private fun requireTunnelAddress(value: String) {
        require(Regex("^10\\.64\\.(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[1-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-4])/32$").matches(value)) {
            "VPN address is invalid"
        }
    }

    fun build(privateKey: String, response: VpnConfigResponse): String {
        requireSingleLine("Client private key", privateKey)
        requireSingleLine("Server public key", response.peer.serverPublicKey)
        requireSingleLine("VPN address", response.peer.address)
        requireTunnelAddress(response.peer.address)
        requireSingleLine("VPN DNS", response.peer.dns)
        requireSingleLine("Allowed IPs", response.peer.allowedIPs)
        requireSingleLine("VPN server hostname", response.server.hostname)
        require(response.server.port in 1..65535) { "VPN server port is invalid" }
        require(response.peer.persistentKeepalive in 0..65535) { "Persistent keepalive is invalid" }

        return buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = $privateKey")
            appendLine("Address = ${response.peer.address}")
            appendLine("DNS = ${response.peer.dns}")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = ${response.peer.serverPublicKey}")
            appendLine("AllowedIPs = ${response.peer.allowedIPs}")
            appendLine("Endpoint = ${response.server.hostname}:${response.server.port}")
            appendLine("PersistentKeepalive = ${response.peer.persistentKeepalive}")
        }
    }
}
