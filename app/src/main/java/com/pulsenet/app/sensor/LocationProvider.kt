package com.pulsenet.app.sensor

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * A fresh high-accuracy fix, falling back to the last known position if one
     * can't be obtained in time — indoors or with a cold GPS, a fresh fix often
     * never arrives, and a slightly stale position is far more useful to a
     * rescuer than none.
     */
    suspend fun getCurrentLocation(timeoutMs: Long = 5000L): Pair<Double, Double>? =
        requestFreshFix(timeoutMs) ?: lastKnownLocation()

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshFix(timeoutMs: Long): Pair<Double, Double>? =
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val request = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .build()
                try {
                    fusedLocationClient.getCurrentLocation(request, null)
                        .addOnSuccessListener { location ->
                            if (continuation.isActive) {
                                continuation.resume(location?.let { it.latitude to it.longitude })
                            }
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                } catch (e: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnownLocation(): Pair<Double, Double>? =
        withTimeoutOrNull(2000L) {
            suspendCancellableCoroutine { continuation ->
                try {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location ->
                            if (continuation.isActive) {
                                continuation.resume(location?.let { it.latitude to it.longitude })
                            }
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                } catch (e: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
}
