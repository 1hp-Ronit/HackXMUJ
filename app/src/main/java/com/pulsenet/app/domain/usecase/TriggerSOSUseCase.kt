package com.pulsenet.app.domain.usecase

import com.pulsenet.app.data.local.UserPreferences
import com.pulsenet.app.domain.model.Priority
import com.pulsenet.app.sensor.LocationProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerSOSUseCase @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val locationProvider: LocationProvider,
    private val userPreferences: UserPreferences
) {
    suspend operator fun invoke(
        content: String = "SOS - immediate assistance needed"
    ): Pair<Double, Double> {
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
