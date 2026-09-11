package com.pulsenet.app.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pulsenet.app.domain.model.Priority
import com.pulsenet.app.ui.components.MessageCard
import com.pulsenet.app.ui.theme.PulseBackground
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
                Text("+", fontSize = 28.sp)
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
                Button(
                    onClick = { selectedPriority = priority },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPriority == priority) priority.toColor() else PulseBackground
                    )
                ) {
                    Text(priority.name)
                }
            }
        }
        Button(
            onClick = { onSend(content, selectedPriority) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)
        ) {
            Text("Send")
        }
    }
}
