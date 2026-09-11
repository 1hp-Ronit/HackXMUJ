package com.pulsenet.app.domain.usecase

import com.pulsenet.app.data.local.UserPreferences
import com.pulsenet.app.domain.model.Priority
import com.pulsenet.app.mesh.NearbyMeshManager
import com.pulsenet.app.sensor.LocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerSOSUseCase @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val locationProvider: LocationProvider,
    private val userPreferences: UserPreferences,
    private val nearbyMeshManager: NearbyMeshManager
) {
    private companion object {
        const val SOS_DISCOVERY_TIMEOUT_MS = 2 * 60_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend operator fun invoke(
        content: String = "SOS - immediate assistance needed"
    ): Pair<Double, Double> {
        // An SOS can't wait on the normal 30s-every-5-minutes duty cycle — force
        // continuous discovery until a peer connects, or give up after 2 minutes
        // so an SOS in a truly empty area doesn't drain the battery forever.
        nearbyMeshManager.setSosOverrideActive(true)
        scope.launch {
            withTimeoutOrNull(SOS_DISCOVERY_TIMEOUT_MS) {
                nearbyMeshManager.connectedPeers.first { it.isNotEmpty() }
            }
            nearbyMeshManager.setSosOverrideActive(false)
        }

        val location = locationProvider.getCurrentLocation() ?: (0.0 to 0.0)
        val alias = userPreferences.getAliasSnapshot()
        sendMessageUseCase(
            content = content,
            latitude = location.first,
            longitude = location.second,
            priority = Priority.SOS,
            senderAlias = alias
        )
        return location
    }
}
