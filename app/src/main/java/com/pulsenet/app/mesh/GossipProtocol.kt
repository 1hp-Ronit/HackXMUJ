package com.pulsenet.app.mesh

import com.squareup.moshi.JsonClass

/**
 * Wire protocol exchanged as Moshi JSON over Nearby Connections BYTES payloads.
 * Nearby Connections gives raw M-to-N links between peers only; the gossip
 * (anti-entropy) protocol built on top of it is what actually routes messages
 * across multiple hops.
 */
sealed class GossipMessage {
    abstract val type: String
}

@JsonClass(generateAdapter = true)
data class HashListPayload(
    override val type: String = TYPE,
    val messageIds: List<String>
) : GossipMessage() {
    companion object {
        const val TYPE = "HASH_LIST"
    }
}

@JsonClass(generateAdapter = true)
data class MessageBatchPayload(
    override val type: String = TYPE,
    val messages: List<WireMessage>
) : GossipMessage() {
    companion object {
        const val TYPE = "MESSAGE_BATCH"
    }
}

@JsonClass(generateAdapter = true)
data class WireMessage(
    val messageId: String,
    val senderPublicKey: String,
    val senderAlias: String,
    val content: String,
    val latitude: Double,
    val longitude: Double,
    val priority: Int,
    val hopCount: Int,
    val maxHops: Int,
    val signature: String,
    val createdAtEpochMs: Long
)

/** Minimal envelope used only to read the "type" discriminator before picking an adapter. */
@JsonClass(generateAdapter = true)
data class GossipEnvelope(val type: String)
