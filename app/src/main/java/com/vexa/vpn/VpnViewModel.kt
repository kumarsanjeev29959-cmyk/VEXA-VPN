package com.vexa.vpn

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Owns VPN connection state outside the Compose UI lifecycle. */
class VpnViewModel : ViewModel() {
    private var controller: VpnController? = null
    private val _state = MutableStateFlow<VpnUiState>(VpnUiState.Disconnected)
    val state: StateFlow<VpnUiState> = _state.asStateFlow()

    fun initialize(context: Context) {
        if (controller != null) return
        controller = VpnController(context.applicationContext)
        refreshState()
    }

    fun refreshState() {
        val vpn = controller ?: return
        _state.value = when (runCatching { vpn.state() }.getOrNull()) {
            Tunnel.State.UP -> VpnUiState.Connected
            Tunnel.State.TOGGLE -> VpnUiState.Connecting
            else -> VpnUiState.Disconnected
        }
    }

    fun connect(config: String) {
        val vpn = controller ?: return
        if (config.isBlank()) {
            _state.value = VpnUiState.Error("VPN configuration is not available yet.")
            return
        }
        _state.value = VpnUiState.Connecting
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { vpn.connect(config) }
                .onSuccess { state ->
                    _state.value = if (state == Tunnel.State.UP) VpnUiState.Connected else VpnUiState.Disconnected
                }
                .onFailure { error -> _state.value = VpnUiState.Error(vpn.friendlyError(error)) }
        }
    }

    fun disconnect() {
        val vpn = controller ?: return
        _state.value = VpnUiState.Disconnecting
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { vpn.disconnect() }
                .onSuccess { _state.value = VpnUiState.Disconnected }
                .onFailure { error -> _state.value = VpnUiState.Error(vpn.friendlyError(error)) }
        }
    }
}
