package com.pulsenet.app.ui.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulsenet.app.domain.usecase.TriggerSOSUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SOSUiState {
    data object Idle : SOSUiState
    data object AcquiringLocation : SOSUiState
    data class Sent(val latitude: Double, val longitude: Double) : SOSUiState
}

@HiltViewModel
class SOSViewModel @Inject constructor(
    private val triggerSOSUseCase: TriggerSOSUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SOSUiState>(SOSUiState.Idle)
    val uiState: StateFlow<SOSUiState> = _uiState.asStateFlow()

    private var pendingJob: Job? = null

    fun triggerSOS() {
        if (_uiState.value is SOSUiState.AcquiringLocation) return
        _uiState.value = SOSUiState.AcquiringLocation
        pendingJob = viewModelScope.launch {
            val (lat, lng) = triggerSOSUseCase()
            _uiState.value = SOSUiState.Sent(lat, lng)
        }
    }

    /** Only meaningful while still acquiring a GPS fix; once sent, the mesh already has it. */
    fun cancelPendingSOS() {
        pendingJob?.cancel()
        _uiState.value = SOSUiState.Idle
    }
}
