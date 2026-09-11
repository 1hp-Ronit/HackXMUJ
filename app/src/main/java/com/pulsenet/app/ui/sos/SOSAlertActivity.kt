package com.pulsenet.app.ui.sos

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen SOS confirmation launched directly by [com.pulsenet.app.sensor.DistressSensorManager]
 * so it can appear even over the lock screen. The in-app, navigable SOS flow with the
 * giant panic button lives separately under ui/sos/SOSScreen.kt.
 */
@AndroidEntryPoint
class SOSAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        setContent {
            SOSAlertScreen(onDismiss = { finish() })
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
private fun SOSAlertScreen(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SOS SENT", color = Color(0xFFFF2D2D), fontSize = 40.sp)
        Text(
            "Your distress signal is broadcasting to nearby devices.",
            color = Color(0xFFF0F0F0),
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )
        Button(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
}
