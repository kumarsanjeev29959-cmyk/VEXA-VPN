package com.vexa.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VexaApp() }
    }
}

@Composable
fun VexaApp() {
    var connected by remember { mutableStateOf(false) }
    var server by remember { mutableStateOf("Auto • Fastest") }
    MaterialTheme(colorScheme = darkColorScheme()) {
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
                    Text(if (connected) "PROTECTED" else "NOT CONNECTED", color = if (connected) Color(0xFF55E6A5) else Color(0xFF8B93A7), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { connected = !connected },
                        modifier = Modifier.size(130.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = if (connected) Color(0xFF173D31) else Color(0xFF5B5FEF))
                    ) { Text(if (connected) "ON" else "CONNECT", fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(36.dp))
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111622)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("SERVER", color = Color(0xFF8B93A7), fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(server, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ping  —  24 ms", color = Color(0xFFB7BECE), fontSize = 13.sp)
                        Text("Secure", color = Color(0xFF55E6A5), fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("VEXA VPN • v1.0.0", color = Color(0xFF555D70), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
        }
    }
}
