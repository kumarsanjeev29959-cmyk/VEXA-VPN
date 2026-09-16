package com.vexa.vpn

object WireGuardConfigBuilder {
    fun build(privateKey: String, response: VpnConfigResponse): String {
        require(privateKey.isNotBlank()) { "Client private key is empty" }
        require(response.peer.serverPublicKey.isNotBlank()) { "Server public key is empty" }
        require(response.peer.address.isNotBlank()) { "VPN address is empty" }
        require(response.peer.dns.isNotBlank()) { "VPN DNS is empty" }
        require(response.peer.allowedIPs.isNotBlank()) { "Allowed IPs are empty" }
        require(response.server.hostname.isNotBlank()) { "VPN server hostname is empty" }
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
