package com.pulsenet.app.ui.home

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.domain.model.PeerNode
import com.pulsenet.app.mesh.MeshService
import com.pulsenet.app.mesh.NearbyMeshManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val peers: List<PeerNode> = emptyList(),
    val messageCount: Int = 0,
    val batteryPercent: Int = 100
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    nearbyMeshManager: NearbyMeshManager,
    messageDao: MessageDao
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        nearbyMeshManager.connectedPeers,
        messageDao.observeAllMessages().map { it.size }
    ) { peers, messageCount ->
        HomeUiState(peers = peers, messageCount = messageCount, batteryPercent = currentBatteryPercent())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun ensureMeshServiceRunning() {
        ContextCompat.startForegroundService(context, Intent(context, MeshService::class.java))
    }

    private fun currentBatteryPercent(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
