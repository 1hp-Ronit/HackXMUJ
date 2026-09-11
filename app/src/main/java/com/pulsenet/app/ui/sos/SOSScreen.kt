package com.pulsenet.app.ui.sos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pulsenet.app.ui.components.SOSButton
import com.pulsenet.app.ui.theme.PulseBlue
import com.pulsenet.app.ui.theme.SOSRed
import com.pulsenet.app.ui.theme.TextPrimary

@Composable
fun SOSScreen(
    onBack: () -> Unit,
    viewModel: SOSViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) {
            Text("← Back", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val current = state) {
                is SOSUiState.Idle -> {
                    SOSButton(
                        onTap = { showConfirmDialog = true },
                        onLongPress = { viewModel.triggerSOS() }
                    )
                    Text(
                        "Tap to confirm, or hold for 2 seconds to send immediately.",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }

                is SOSUiState.AcquiringLocation -> {
                    Text(
                        "Acquiring GPS location…",
                        color = PulseBlue,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    TextButton(onClick = { viewModel.cancelPendingSOS() }) {
                        Text("Cancel", color = TextPrimary)
                    }
                }

                is SOSUiState.Sent -> {
                    Text("SOS SENT", color = SOSRed, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Coordinates: %.5f, %.5f".format(current.latitude, current.longitude),
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            Text(
                "If you can't use the screen, tap the back of your phone 5 times rapidly.",
                color = TextPrimary.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 40.dp)
            )
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Are you in danger?") },
            text = { Text("This alerts all nearby PulseNet devices with your location.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    viewModel.triggerSOS()
                }) { Text("Yes, send SOS") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
