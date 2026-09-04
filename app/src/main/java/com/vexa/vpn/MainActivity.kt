package com.vexa.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val vpnViewModel: VpnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vpnViewModel.initialize(this)
        setContent { VexaApp(vpnViewModel) }
    }

    override fun onResume() {
        super.onResume()
        vpnViewModel.refreshState()
    }
}

@Composable
fun VexaApp(vpnViewModel: VpnViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vpnState by vpnViewModel.state.collectAsStateWithLifecycle()
    var configText by rememberSaveable { mutableStateOf("") }
    var showConfig by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    fun connectNow() {
        if (configText.isBlank()) {
            showConfig = true
            return
        }
        vpnViewModel.connect(configText)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) connectNow()
        else message = "VPN permission is required to protect your connection."
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        val connected = vpnState is VpnUiState.Connected
        val busy = vpnState is VpnUiState.Connecting || vpnState is VpnUiState.Disconnecting

        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF090B12)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            Text("VEXA", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("PRIVATE • FAST • SECURE", color = Color(0xFF8B93A7), fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(55.dp))

            Box(
                modifier = Modifier.size(240.dp).background(Color(0xFF111622), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (vpnState) {
                            VpnUiState.Connecting -> "CONNECTING"
                            VpnUiState.Connected -> "PROTECTED"
                            VpnUiState.Disconnecting -> "DISCONNECTING"
                            is VpnUiState.Error -> "ERROR"
                            VpnUiState.Disconnected -> "NOT CONNECTED"
                        },
                        color = when (vpnState) {
                            VpnUiState.Connected -> Color(0xFF55E6A5)
                            is VpnUiState.Error -> Color(0xFFFF7B7B)
                            else -> Color(0xFF8B93A7)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        enabled = !busy,
                        onClick = {
                            if (connected) vpnViewModel.disconnect()
                            else {
                                val intent: Intent? = VpnService.prepare(context)
                                if (intent != null) permissionLauncher.launch(intent) else connectNow()
                            }
                        },
                        modifier = Modifier.size(130.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (connected) Color(0xFF173D31) else Color(0xFF5B5FEF)
                        )
                    ) { Text(if (connected) "OFF" else "CONNECT", fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(36.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111622)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("SERVER", color = Color(0xFF8B93A7), fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Auto • Fastest", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("WireGuard", color = Color(0xFFB7BECE), fontSize = 13.sp)
                        Text(if (connected) "Secure" else "Ready", color = Color(0xFF55E6A5), fontSize = 13.sp)
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { showConfig = true }) { Text("DEV SERVER CONFIG") }
            }
            Spacer(Modifier.weight(1f))
            Text("VEXA VPN • v1.0.0", color = Color(0xFF555D70), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
        }

        if (showConfig) {
            AlertDialog(
                onDismissRequest = { showConfig = false },
                title = { Text("WireGuard development config") },
                text = {
                    Column {
                        Text("Development only. Paste a valid WireGuard client configuration. It stays in memory and is not uploaded by this screen.", fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = configText,
                            onValueChange = { configText = it },
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            placeholder = { Text("[Interface]\nPrivateKey = ...\nAddress = ...\n[Peer]\n...") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showConfig = false; connectNow() }) { Text("CONNECT") }
                },
                dismissButton = { TextButton(onClick = { showConfig = false }) { Text("CANCEL") } }
            )
        }

        message?.let { text ->
            AlertDialog(
                onDismissRequest = { message = null },
                title = { Text("VEXA VPN") },
                text = { Text(text) },
                confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }
            )
        }

        if (vpnState is VpnUiState.Error) {
            LaunchedEffect(vpnState) { message = (vpnState as VpnUiState.Error).message }
        }
    }
}
