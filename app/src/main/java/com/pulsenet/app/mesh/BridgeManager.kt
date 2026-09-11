package com.pulsenet.app.mesh

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches for internet connectivity so cached mesh messages can be flushed to the
 * cloud once a device reaches cellular/Wi-Fi backhaul. Detection only for now —
 * enqueuing the actual WorkManager flush is wired up once BridgeFlushWorker exists.
 */
@Singleton
class BridgeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "BridgeManager"
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                Log.i(TAG, "Internet connectivity detected")
                onInternetAvailable()
            }
        }
    }

    /** Body filled in once BridgeFlushWorker exists, to enqueue the actual cloud sync. */
    private fun onInternetAvailable() = Unit

    fun start() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stop() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }
}
