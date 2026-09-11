package com.pulsenet.app.mesh

class FakeMeshTransport : MeshTransport {
    val sentPayloads = mutableListOf<Pair<String, ByteArray>>()

    val connectedEndpointIds = mutableListOf<String>()

    override fun sendPayload(endpointId: String, bytes: ByteArray) {
        sentPayloads.add(endpointId to bytes)
    }

    override fun sendPayloadToAll(bytes: ByteArray) {
        connectedEndpointIds.forEach { sentPayloads.add(it to bytes) }
    }

    fun lastPayloadJsonTo(endpointId: String): String =
        sentPayloads.last { it.first == endpointId }.second.toString(Charsets.UTF_8)
}
