package com.pulsenet.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulsenet.app.domain.model.PeerNode
import com.pulsenet.app.ui.theme.PulseBlue
import com.pulsenet.app.ui.theme.TextPrimary

@Composable
fun PeerListSheet(peers: List<PeerNode>) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Nearby Mesh Devices", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text(
            "Connected via Bluetooth / Wi-Fi Direct — no internet needed",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (peers.isEmpty()) {
            Text(
                "No devices connected yet. Discovery runs in short bursts to save " +
                    "battery — keep the app open nearby another PulseNet device.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.7f)
            )
        } else {
            peers.forEachIndexed { index, peer ->
                PeerRow(peer)
                if (index != peers.lastIndex) {
                    HorizontalDivider(color = TextPrimary.copy(alpha = 0.08f))
                }
            }
        }
    }
}

@Composable
private fun PeerRow(peer: PeerNode) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(PulseBlue, CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(peer.alias, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(
                "Connected ${connectedDurationLabel(peer.connectedAtEpochMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.5f)
            )
        }
    }
}

private fun connectedDurationLabel(connectedAtEpochMs: Long): String {
    val elapsedSeconds = (System.currentTimeMillis() - connectedAtEpochMs) / 1000
    return when {
        elapsedSeconds < 60 -> "just now"
        elapsedSeconds < 3600 -> "${elapsedSeconds / 60}m ago"
        else -> "${elapsedSeconds / 3600}h ago"
    }
}
