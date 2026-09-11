package com.pulsenet.app.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulsenet.app.data.local.UserPreferences
import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.domain.model.Message
import com.pulsenet.app.domain.model.Priority
import com.pulsenet.app.domain.model.toDomain
import com.pulsenet.app.domain.usecase.SendMessageUseCase
import com.pulsenet.app.sensor.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    messageDao: MessageDao,
    private val sendMessageUseCase: SendMessageUseCase,
    private val locationProvider: LocationProvider,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val messages: StateFlow<List<Message>> = messageDao.observeAllMessages()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendMessage(content: String, priority: Priority) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation() ?: (0.0 to 0.0)
            val alias = userPreferences.getAliasSnapshot()
            sendMessageUseCase(
                content = content,
                latitude = location.first,
                longitude = location.second,
                priority = priority,
                senderAlias = alias
            )
        }
    }
}
