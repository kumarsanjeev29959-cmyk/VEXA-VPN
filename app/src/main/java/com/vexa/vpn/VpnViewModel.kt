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

class VpnViewModel:ViewModel(){
 private var controller:VpnController?=null; private var identity:DeviceIdentity?=null; private var provisioning:VpnProvisioningRepository=UnconfiguredProvisioningRepository()
 private val _state=MutableStateFlow<VpnUiState>(VpnUiState.Disconnected); val state:StateFlow<VpnUiState>=_state.asStateFlow()
 fun initialize(context:Context){if(controller!=null)return;val app=context.applicationContext;controller=VpnController(app);identity=DeviceIdentity(app);provisioning=if(BuildConfig.VEXA_API_BASE_URL.isBlank())UnconfiguredProvisioningRepository()else HttpVpnProvisioningRepository(app,BuildConfig.VEXA_API_BASE_URL);refreshState()}
 fun refreshState(){val vpn=controller?:return;_state.value=when(runCatching{vpn.state()}.getOrNull()){Tunnel.State.UP->VpnUiState.Connected;Tunnel.State.TOGGLE->VpnUiState.Connecting;else->VpnUiState.Disconnected}}
 fun connectAutomatically(){val vpn=controller?:return;val device=identity?:return;if(_state.value is VpnUiState.Connecting||_state.value is VpnUiState.Disconnecting)return;_state.value=VpnUiState.Connecting;viewModelScope.launch(Dispatchers.IO){val result=runCatching{val reg=provisioning.registerDevice(device).getOrThrow();val servers=provisioning.listServers(reg.deviceToken).getOrThrow();val selection=ServerSelector.selectFastest(servers)?:error("No healthy VPN server is available right now.");provisioning.provisionConfig(reg.deviceToken,ProvisioningRequest(device.deviceId,device.publicKey,selection.server.id,true)).getOrThrow()};result.onSuccess{response->val config=buildWireGuardConfig(device.privateKey(),response);runCatching{vpn.connect(config)}.onSuccess{state->_state.value=if(state==Tunnel.State.UP)VpnUiState.Connected else VpnUiState.Disconnected}.onFailure{e->_state.value=VpnUiState.Error(vpn.friendlyError(e))}}.onFailure{e->_state.value=VpnUiState.Error(e.message?:"VEXA could not provision a VPN connection.")}}}
 private fun buildWireGuardConfig(privateKey:String,response:VpnConfigResponse):String=buildString{appendLine("[Interface]");appendLine("PrivateKey = $privateKey");appendLine("Address = ${response.peer.address}");appendLine("DNS = ${response.peer.dns}");appendLine();appendLine("[Peer]");appendLine("PublicKey = ${response.peer.serverPublicKey}");appendLine("AllowedIPs = ${response.peer.allowedIPs}");appendLine("Endpoint = ${response.server.hostname}:${response.server.port}");appendLine("PersistentKeepalive = ${response.peer.persistentKeepalive}")}
 fun connect(config:String){val vpn=controller?:return;if(config.isBlank()){_state.value=VpnUiState.Error("VPN configuration is not available yet.");return};_state.value=VpnUiState.Connecting;viewModelScope.launch(Dispatchers.IO){runCatching{vpn.connect(config)}.onSuccess{state->_state.value=if(state==Tunnel.State.UP)VpnUiState.Connected else VpnUiState.Disconnected}.onFailure{e->_state.value=VpnUiState.Error(vpn.friendlyError(e))}}}
 fun disconnect(){val vpn=controller?:return;if(_state.value is VpnUiState.Disconnecting)return;_state.value=VpnUiState.Disconnecting;viewModelScope.launch(Dispatchers.IO){runCatching{vpn.disconnect()}.onSuccess{_state.value=VpnUiState.Disconnected}.onFailure{e->_state.value=VpnUiState.Error(vpn.friendlyError(e))}}}
}
