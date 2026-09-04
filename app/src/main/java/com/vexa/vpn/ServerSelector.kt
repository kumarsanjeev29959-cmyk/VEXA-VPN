package com.vexa.vpn

/** Pure, deterministic server ranking for Auto/Fastest selection. */
object ServerSelector {
    fun selectFastest(servers: List<VpnServer>): ServerSelection? {
        val candidates = servers
            .asSequence()
            .filter { it.healthy }
            .filter { it.latencyMs != null && it.latencyMs >= 0 }
            .sortedWith(
                compareBy<VpnServer> { it.latencyMs!! }
                    .thenBy { it.loadPercent.coerceIn(0, 100) }
                    .thenBy { it.id }
            )
            .toList()

        return candidates.firstOrNull()?.let {
            ServerSelection(it, "Lowest healthy latency")
        }
    }
}
