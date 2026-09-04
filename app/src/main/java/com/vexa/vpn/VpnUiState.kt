package com.vexa.vpn

/** UI-level states used by the connection screen. */
sealed interface VpnUiState {
    data object Disconnected : VpnUiState
    data object Connecting : VpnUiState
    data object Connected : VpnUiState
    data object Disconnecting : VpnUiState
    data class Error(val message: String) : VpnUiState
}
