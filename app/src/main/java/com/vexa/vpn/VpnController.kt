package com.vexa.vpn

import android.content.Context
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream

/** Controls a real WireGuard userspace tunnel through the official Android tunnel library. */
class VpnController(context: Context) {
    private val backend = GoBackend(context.applicationContext)
    private val tunnel = object : Tunnel {
        override fun getName(): String = "vexa"
        override fun onStateChange(newState: Tunnel.State) = Unit
    }

    @Synchronized
    @Throws(Exception::class)
    fun connect(wireGuardConfig: String): Tunnel.State {
        require(wireGuardConfig.isNotBlank()) { "WireGuard configuration is empty" }
        val config = Config.parse(ByteArrayInputStream(wireGuardConfig.toByteArray(Charsets.UTF_8)))
        return backend.setState(tunnel, Tunnel.State.UP, config)
    }

    @Synchronized
    @Throws(Exception::class)
    fun disconnect(): Tunnel.State = backend.setState(tunnel, Tunnel.State.DOWN, null)

    fun state(): Tunnel.State = backend.getState(tunnel)

    fun isVpnAuthorizationRequired(context: Context): Boolean =
        android.net.VpnService.prepare(context) != null

    fun friendlyError(error: Throwable): String = when (error) {
        is BackendException -> error.reason.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        else -> error.message ?: "Unable to start VPN"
    }
}
