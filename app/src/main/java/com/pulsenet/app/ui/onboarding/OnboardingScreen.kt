package com.pulsenet.app.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pulsenet.app.ui.theme.BorderSubtle
import com.pulsenet.app.ui.theme.PulseBlue
import com.pulsenet.app.ui.theme.SOSRed
import com.pulsenet.app.ui.theme.TextDim
import com.pulsenet.app.ui.theme.TextPrimary

private val runtimePermissions: Array<String> = buildList {
    add(android.Manifest.permission.ACCESS_FINE_LOCATION)
    add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
        add(android.Manifest.permission.BLUETOOTH_CONNECT)
        add(android.Manifest.permission.BLUETOOTH_SCAN)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(android.Manifest.permission.POST_NOTIFICATIONS)
        add(android.Manifest.permission.NEARBY_WIFI_DEVICES)
    }
}.toTypedArray()

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var permissionsGranted by rememberSaveable { mutableStateOf(false) }
    var alias by rememberSaveable { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> permissionsGranted = results.values.all { it } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Welcome to PulseNet", style = MaterialTheme.typography.headlineLarge, color = PulseBlue)
        Text(
            "PulseNet keeps you connected when the network doesn't. It forms an offline " +
                "mesh with nearby phones and lets you send an SOS even without signal.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )

        Text(
            "PulseNet needs Bluetooth and Location access to discover nearby devices.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        OutlinedButton(
            onClick = { permissionLauncher.launch(runtimePermissions) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(1.dp, if (permissionsGranted) PulseBlue else BorderSubtle),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (permissionsGranted) PulseBlue else TextPrimary
            )
        ) {
            Text(if (permissionsGranted) "Permissions granted ✓" else "Grant Permissions")
        }

        Text(
            "Battery optimization can stop PulseNet's mesh from running in the background. " +
                "Disabling it keeps you reachable during an emergency.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        OutlinedButton(
            onClick = { requestIgnoreBatteryOptimizations(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(1.dp, BorderSubtle),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Text("Disable Battery Optimization")
        }

        Text("What should nearby devices call you?", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        OutlinedTextField(
            value = alias,
            onValueChange = { alias = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Your name") }
        )

        Text(
            "Identity: ${viewModel.publicKeyPreview}",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim
        )

        Button(
            onClick = { viewModel.completeOnboarding(alias, onFinished) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SOSRed)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun requestIgnoreBatteryOptimizations(context: android.content.Context) {
    val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}
