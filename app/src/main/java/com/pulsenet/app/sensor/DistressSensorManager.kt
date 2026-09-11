package com.pulsenet.app.sensor

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.pulsenet.app.domain.usecase.TriggerSOSUseCase
import com.pulsenet.app.ui.sos.SOSAlertActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to the accelerometer while [com.pulsenet.app.mesh.MeshService] is alive
 * so a "tap the back of the phone 5 times" pattern can trigger SOS even with the
 * screen off or the phone locked. Runs inside the foreground service so it
 * survives Doze Mode; accelerometer draw alone is negligible (~2mA).
 */
@Singleton
class DistressSensorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val triggerSOSUseCase: TriggerSOSUseCase
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val detector = ShakePatternDetector()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())
        if (detector.onSample(magnitude, System.currentTimeMillis())) {
            onSOSTriggered()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onSOSTriggered() {
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PulseNet:SosWakeLock")
        wakeLock.acquire(10_000L)
        vibrate()

        scope.launch {
            try {
                triggerSOSUseCase()
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }

        context.startActivity(
            Intent(context, SOSAlertActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 400, 150, 400, 150, 400) // long-short-long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }
}
