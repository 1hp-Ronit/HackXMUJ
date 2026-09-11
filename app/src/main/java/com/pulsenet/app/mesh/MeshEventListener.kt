package com.pulsenet.app.mesh

/**
 * Callback surface NearbyMeshManager drives GossipEngine through.
 *
 * NearbyMeshManager never depends on GossipEngine directly (that would create a
 * constructor injection cycle, since GossipEngine needs NearbyMeshManager to send
 * payloads back out) — instead whoever owns both wires this listener after
 * construction, e.g. `nearbyMeshManager.listener = gossipEngine` in MeshService.
 */
interface MeshEventListener {
    fun onPeerConnected(endpointId: String, endpointName: String)
    fun onPeerDisconnected(endpointId: String)
    fun onPayloadReceived(endpointId: String, payload: ByteArray)
}
