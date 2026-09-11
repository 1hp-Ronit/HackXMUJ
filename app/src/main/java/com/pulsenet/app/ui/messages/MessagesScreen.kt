package com.pulsenet.app.ui.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pulsenet.app.domain.model.Priority
import com.pulsenet.app.ui.components.MessageCard
import com.pulsenet.app.ui.theme.BorderSubtle
import com.pulsenet.app.ui.theme.PulseBackground
import com.pulsenet.app.ui.theme.TextPrimary
import com.pulsenet.app.ui.theme.toColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    var showComposeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        containerColor = PulseBackground,
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showComposeSheet = true }) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.messageId }) { message ->
                MessageCard(message = message)
            }
        }
    }

    if (showComposeSheet) {
        ModalBottomSheet(onDismissRequest = { showComposeSheet = false }, sheetState = sheetState) {
            ComposeMessageSheet(
                onSend = { content, priority ->
                    viewModel.sendMessage(content, priority)
                    showComposeSheet = false
                }
            )
        }
    }
}

private fun Priority.label(): String = when (this) {
    Priority.SOS -> "SOS"
    Priority.MEDICAL -> "Medical"
    Priority.RESOURCE -> "Resource"
    Priority.GENERAL -> "General"
}

@Composable
private fun ComposeMessageSheet(onSend: (String, Priority) -> Unit) {
    var content by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.GENERAL) }

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text("New Message", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            label = { Text("What's happening?") }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Priority.entries.forEach { priority ->
                PriorityChip(
                    priority = priority,
                    selected = selectedPriority == priority,
                    onClick = { selectedPriority = priority },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Button(
            onClick = { onSend(content, selectedPriority) },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(56.dp)
        ) {
            Text("Send")
        }
    }
}

@Composable
private fun PriorityChip(
    priority: Priority,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = priority.toColor(), contentColor = PulseBackground),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Text(priority.label(), style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            border = BorderStroke(1.dp, BorderSubtle),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(priority.toColor())
            )
            Text(
                priority.label(),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}
