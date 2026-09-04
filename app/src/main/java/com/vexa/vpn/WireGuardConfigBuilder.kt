package com.vexa.vpn

object WireGuardConfigBuilder {
    fun build(privateKey: String, response: VpnConfigResponse): String = buildString {
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
