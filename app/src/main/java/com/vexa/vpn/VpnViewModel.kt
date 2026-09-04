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

/** Owns VPN lifecycle and anonymous device provisioning outside the Compose UI lifecycle. */
class VpnViewModel : ViewModel() {
    private var controller: VpnController? = null
    private var identity: DeviceIdentity? = null
    private var provisioning: VpnProvisioningRepository = UnconfiguredProvisioningRepository()
    private val _state = MutableStateFlow<VpnUiState>(VpnUiState.Disconnected)
    val state: StateFlow<VpnUiState> = _state.asStateFlow()

    fun initialize(context: Context) {
        if (controller != null) return
        val appContext = context.applicationContext
        controller = VpnController(appContext)
        identity = DeviceIdentity(appContext)
        provisioning = if (BuildConfig.VEXA_API_BASE_URL.isBlank()) {
            UnconfiguredProvisioningRepository()
        } else {
            HttpVpnProvisioningRepository(appContext, BuildConfig.VEXA_API_BASE_URL)
        }
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

    /** Automatic account-less flow: register the device, select the fastest healthy server, provision config, then connect. */
    fun connectAutomatically() {
        val vpn = controller ?: return
        val device = identity ?: return
        if (_state.value is VpnUiState.Connecting || _state.value is VpnUiState.Disconnecting) return
        _state.value = VpnUiState.Connecting
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val registration = provisioning.registerDevice(device).getOrThrow()
                val servers = provisioning.listServers(registration.deviceToken).getOrThrow()
                val selection = ServerSelector.selectFastest(servers)
                    ?: error("No healthy VPN server is available right now.")
                provisioning.provisionConfig(
                    registration.deviceToken,
                    ProvisioningRequest(
                        deviceId = device.deviceId,
                        publicKey = device.publicKey,
                        serverId = selection.server.id,
                        fastest = true
                    )
                ).getOrThrow()
            }
            result.onSuccess { config ->
                runCatching { vpn.connect(config.config) }
                    .onSuccess { state ->
                        _state.value = if (state == Tunnel.State.UP) VpnUiState.Connected else VpnUiState.Disconnected
                    }
                    .onFailure { error -> _state.value = VpnUiState.Error(vpn.friendlyError(error)) }
            }.onFailure { error ->
                _state.value = VpnUiState.Error(error.message ?: "VEXA could not provision a VPN connection.")
            }
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
        if (_state.value is VpnUiState.Disconnecting) return
        _state.value = VpnUiState.Disconnecting
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { vpn.disconnect() }
                .onSuccess { _state.value = VpnUiState.Disconnected }
                .onFailure { error -> _state.value = VpnUiState.Error(vpn.friendlyError(error)) }
        }
    }
}
