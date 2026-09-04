package com.vexa.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnUiStateTest {
    @Test
    fun disconnectedStateIsStable() {
        val state: VpnUiState = VpnUiState.Disconnected
        assertEquals(VpnUiState.Disconnected, state)
    }

    @Test
    fun errorStateKeepsFriendlyMessage() {
        val state = VpnUiState.Error("VPN permission denied")
        assertEquals("VPN permission denied", state.message)
    }
}
