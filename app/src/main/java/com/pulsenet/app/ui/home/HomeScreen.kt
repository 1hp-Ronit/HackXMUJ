package com.pulsenet.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pulsenet.app.ui.components.PulseRadar
import com.pulsenet.app.ui.theme.PulseBlue
import com.pulsenet.app.ui.theme.SOSRed
import com.pulsenet.app.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    onNavigateToSOS: () -> Unit,
    onNavigateToMessages: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.ensureMeshServiceRunning() }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${state.peers.size} peers • ${state.messageCount} messages",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
            Text("⚡ ${state.batteryPercent}%", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
        }

        PulseRadar(
            peerIds = state.peers.map { it.endpointId },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onNavigateToSOS,
                modifier = Modifier.weight(1f).height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SOSRed)
            ) {
                Text("🔴 SOS", style = MaterialTheme.typography.titleLarge)
            }
            Button(
                onClick = onNavigateToMessages,
                modifier = Modifier.weight(1f).height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PulseBlue)
            ) {
                Text("💬 Messages", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
