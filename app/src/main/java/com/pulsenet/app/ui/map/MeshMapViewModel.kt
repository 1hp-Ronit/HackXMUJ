package com.pulsenet.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.domain.model.Message
import com.pulsenet.app.domain.model.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MeshMapViewModel @Inject constructor(
    messageDao: MessageDao
) : ViewModel() {
    val markers: StateFlow<List<Message>> = messageDao.observeAllMessages()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
