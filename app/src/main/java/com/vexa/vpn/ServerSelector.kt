package com.vexa.vpn

/** Pure, deterministic server ranking for Auto/Fastest selection. */
object ServerSelector {
    fun selectFastest(servers: List<VpnServer>): ServerSelection? {
        val candidates = servers
            .asSequence()
            .filter { it.healthy }
            .sortedWith(
                compareBy<VpnServer> { it.latencyMs ?: Int.MAX_VALUE }
                    .thenBy { it.loadPercent.coerceIn(0, 100) }
                    .thenBy { it.id }
            )
            .toList()

        return candidates.firstOrNull()?.let {
            ServerSelection(
                it,
                if (it.latencyMs != null) "Lowest healthy latency" else "Healthy server with no latency measurement"
            )
        }
    }
}
