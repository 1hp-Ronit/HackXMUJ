package com.pulsenet.app.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.pulsenet.app.domain.model.PeerNode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Nearby Connections. Nearby Connections gives M-to-N direct links between
 * peers via P2P_CLUSTER — it does NOT route messages through intermediate nodes.
 * Multi-hop delivery is implemented on top by [GossipEngine], driven through
 * [listener].
 */
@Singleton
class NearbyMeshManager @Inject constructor(
    @ApplicationContext private val context: Context
) : MeshTransport {
    companion object {
        private const val TAG = "NearbyMeshManager"
        const val SERVICE_ID = "com.pulsenet.mesh"
        private const val DISCOVERY_WINDOW_MS = 30_000L
        private const val DISCOVERY_INTERVAL_MS = 5 * 60_000L
        private val STRATEGY = Strategy.P2P_CLUSTER
    }

    var listener: MeshEventListener? = null

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectedPeers = MutableStateFlow<Map<String, PeerNode>>(emptyMap())
    val connectedPeers: StateFlow<List<PeerNode>> = _connectedPeers
        .map { it.values.toList() }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private var localAlias: String = "PulseNet User"
    private var isAdvertising = false
    private var isDiscovering = false
    private var sosOverrideActive = false
    private var dutyCycleJob: Job? = null

    fun start(localAlias: String) {
        this.localAlias = localAlias
        startAdvertising()
        restartDutyCycle()
    }

    fun stopAll() {
        dutyCycleJob?.cancel()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        isAdvertising = false
        isDiscovering = false
        _connectedPeers.value = emptyMap()
    }

    fun setSosOverrideActive(active: Boolean) {
        if (sosOverrideActive == active) return
        sosOverrideActive = active
        restartDutyCycle()
    }

    /**
     * Kicks off an immediate discovery burst instead of waiting for the next
     * duty-cycled window. Natural windows are only 30s every 5 minutes and start
     * whenever each device's MeshService happened to launch, so two phones sitting
     * side by side can easily have non-overlapping windows for minutes — this is
     * a manual escape hatch for testing/demoing without waiting that out.
     */
    fun forceDiscoveryBurst() {
        if (sosOverrideActive) return // already continuous
        restartDutyCycle()
    }

    override fun sendPayload(endpointId: String, bytes: ByteArray) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnFailureListener { e -> Log.w(TAG, "sendPayload to $endpointId failed", e) }
    }

    override fun sendPayloadToAll(bytes: ByteArray) {
        _connectedPeers.value.keys.forEach { endpointId -> sendPayload(endpointId, bytes) }
    }

    private fun restartDutyCycle() {
        dutyCycleJob?.cancel()
        dutyCycleJob = scope.launch {
            if (sosOverrideActive) {
                startDiscovery()
                awaitCancellation()
            } else {
                while (true) {
                    startDiscovery()
                    delay(DISCOVERY_WINDOW_MS)
                    stopDiscovery()
                    delay(DISCOVERY_INTERVAL_MS - DISCOVERY_WINDOW_MS)
                }
            }
        }
    }

    private fun startAdvertising() {
        if (isAdvertising) return
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            localAlias, SERVICE_ID, connectionLifecycleCallback, options
        ).addOnSuccessListener {
            isAdvertising = true
        }.addOnFailureListener { e ->
            Log.w(TAG, "startAdvertising failed", e)
        }
    }

    private fun startDiscovery() {
        if (isDiscovering) return
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            SERVICE_ID, endpointDiscoveryCallback, options
        ).addOnSuccessListener {
            isDiscovering = true
        }.addOnFailureListener { e ->
            Log.w(TAG, "startDiscovery failed", e)
        }
    }

    private fun stopDiscovery() {
        if (!isDiscovering) return
        connectionsClient.stopDiscovery()
        isDiscovering = false
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection(localAlias, endpointId, connectionLifecycleCallback)
                .addOnFailureListener { e -> Log.w(TAG, "requestConnection to $endpointId failed", e) }
        }

        override fun onEndpointLost(endpointId: String) {
            // No action needed: onDisconnected() handles cleanup for active connections.
        }
    }

    // ConnectionInfo (which carries the peer's advertised alias) is only handed to
    // onConnectionInitiated, not onConnectionResult — bridge it across the handshake
    // rather than falling back to the opaque endpointId as the displayed name.
    private val pendingEndpointNames = mutableMapOf<String, String>()

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            pendingEndpointNames[endpointId] = info.endpointName
            // Disaster scenario: auto-accept, no pairing UI friction.
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            val alias = pendingEndpointNames.remove(endpointId) ?: endpointId
            if (resolution.status.isSuccess) {
                val peer = PeerNode(
                    endpointId = endpointId,
                    alias = alias,
                    connectedAtEpochMs = System.currentTimeMillis()
                )
                _connectedPeers.update { it + (endpointId to peer) }
                listener?.onPeerConnected(endpointId, peer.alias)
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectedPeers.update { it - endpointId }
            listener?.onPeerDisconnected(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes -> listener?.onPayloadReceived(endpointId, bytes) }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // BYTES payloads complete atomically; nothing to track for gossip traffic.
        }
    }
}
