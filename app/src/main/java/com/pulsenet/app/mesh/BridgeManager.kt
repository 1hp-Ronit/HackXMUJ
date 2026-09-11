package com.pulsenet.app.mesh

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pulsenet.app.worker.CloudSyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches for internet connectivity so cached mesh messages can be flushed to the
 * cloud once a device reaches cellular/Wi-Fi backhaul.
 */
@Singleton
class BridgeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudSyncScheduler: CloudSyncScheduler
) {
    private companion object {
        const val TAG = "BridgeManager"
        const val SYNC_NOTIFICATION_ID = 2
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                Log.i(TAG, "Internet connectivity detected — enqueuing cloud sync flush")
                showSyncStartedNotification()
                cloudSyncScheduler.scheduleFlush()
            }
        }
    }

    fun start() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stop() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    private fun showSyncStartedNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, MESH_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("📡 Network detected!")
            .setContentText("Syncing community data...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(SYNC_NOTIFICATION_ID, notification)
    }
}
