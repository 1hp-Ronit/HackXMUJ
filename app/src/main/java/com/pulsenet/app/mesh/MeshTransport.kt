package com.pulsenet.app.mesh

/**
 * Abstraction over "send these bytes to this peer", so [GossipEngine] can be unit
 * tested against a fake transport instead of a real NearbyMeshManager (which talks
 * to the actual Nearby Connections API and can't run outside a device/emulator).
 */
interface MeshTransport {
    fun sendPayload(endpointId: String, bytes: ByteArray)
}
