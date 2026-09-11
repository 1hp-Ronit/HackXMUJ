package com.pulsenet.app.mesh

import android.util.Log
import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.data.local.entity.MessageEntity
import com.pulsenet.app.security.MessageSigner
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anti-entropy gossip protocol layered on top of Nearby Connections' raw M-to-N
 * links, so messages actually propagate across multiple hops instead of only
 * between directly connected devices.
 *
 * Each sync round is a single stateless HASH_LIST exchange: both sides send their
 * own hash list on connect, and upon receiving the peer's list each side pushes
 * back whatever the peer is missing — no separate request/ack round trip needed.
 */
@Singleton
class GossipEngine @Inject constructor(
    private val transport: MeshTransport,
    private val messageDao: MessageDao,
    private val messageSigner: MessageSigner
) : MeshEventListener {

    private companion object {
        const val TAG = "GossipEngine"
    }

    private val moshi = Moshi.Builder().build()
    private val envelopeAdapter = moshi.adapter(GossipEnvelope::class.java)
    private val hashListAdapter = moshi.adapter(HashListPayload::class.java)
    private val messageBatchAdapter = moshi.adapter(MessageBatchPayload::class.java)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onPeerConnected(endpointId: String, endpointName: String) {
        scope.launch { syncWith(endpointId) }
    }

    override fun onPeerDisconnected(endpointId: String) {
        // Nothing to clean up: sync rounds are stateless, keyed only by the
        // HASH_LIST/MESSAGE_BATCH exchange itself.
    }

    override fun onPayloadReceived(endpointId: String, payload: ByteArray) {
        scope.launch { processIncoming(endpointId, payload) }
    }

    /**
     * Pushes a just-created local message to every currently connected peer right
     * away, instead of waiting for the next HASH_LIST sync round. Used for SOS and
     * user-composed messages, where an extra sync-cycle delay is undesirable.
     * Safe to call with zero connected peers: the message still sits in Room ready
     * for the next peer that connects.
     */
    suspend fun broadcastOwnMessage(message: MessageEntity) {
        val outgoing = prepareOutgoing(listOf(message))
        if (outgoing.isEmpty()) return
        val json = messageBatchAdapter.toJson(MessageBatchPayload(messages = outgoing))
        transport.sendPayloadToAll(json.toByteArray())
    }

    suspend fun syncWith(endpointId: String) {
        val localIds = messageDao.getAllMessageIds()
        val json = hashListAdapter.toJson(HashListPayload(messageIds = localIds))
        transport.sendPayload(endpointId, json.toByteArray())
    }

    suspend fun processIncoming(endpointId: String, payload: ByteArray) {
        val json = String(payload)
        val type = try {
            envelopeAdapter.fromJson(json)?.type
        } catch (e: Exception) {
            Log.w(TAG, "Malformed gossip payload from $endpointId", e)
            null
        } ?: return

        when (type) {
            HashListPayload.TYPE -> handleHashList(endpointId, json)
            MessageBatchPayload.TYPE -> handleMessageBatch(json)
            else -> Log.w(TAG, "Unknown gossip payload type '$type' from $endpointId")
        }
    }

    private suspend fun handleHashList(endpointId: String, json: String) {
        val peerIds = hashListAdapter.fromJson(json)?.messageIds?.toSet() ?: return
        val localIds = messageDao.getAllMessageIds().toSet()
        val missingOnPeer = localIds - peerIds
        if (missingOnPeer.isEmpty()) return

        val outgoing = prepareOutgoing(messageDao.getMessagesByIds(missingOnPeer.toList()))
        if (outgoing.isEmpty()) return

        val batchJson = messageBatchAdapter.toJson(MessageBatchPayload(messages = outgoing))
        transport.sendPayload(endpointId, batchJson.toByteArray())
    }

    private suspend fun handleMessageBatch(json: String) {
        val incoming = messageBatchAdapter.fromJson(json)?.messages ?: return
        for (wireMessage in incoming) {
            if (wireMessage.hopCount >= wireMessage.maxHops) continue // TTL exhausted

            val entity = wireMessage.toEntity()
            if (!messageSigner.verify(entity)) {
                Log.w(TAG, "Dropping message ${wireMessage.messageId}: signature verification failed")
                continue
            }
            // insertMessage IGNOREs on duplicate messageId, so re-broadcasts already
            // seen elsewhere in the mesh are deduped here for free.
            messageDao.insertMessage(entity.copy(hopCount = entity.hopCount + 1))
        }
    }

    /**
     * SOS messages transfer first, then newest-first within a priority tier.
     * Messages already at their hop limit are excluded so they stop propagating;
     * the receiver re-checks the limit defensively in [handleMessageBatch] too.
     */
    private fun prepareOutgoing(messages: List<MessageEntity>): List<WireMessage> = messages
        .filter { it.hopCount < it.maxHops }
        .sortedWith(compareBy<MessageEntity> { it.priority }.thenByDescending { it.createdAtEpochMs })
        .map { it.toWireMessage() }

    private fun MessageEntity.toWireMessage() = WireMessage(
        messageId = messageId,
        senderPublicKey = senderPublicKey,
        senderAlias = senderAlias,
        content = content,
        latitude = latitude,
        longitude = longitude,
        priority = priority,
        hopCount = hopCount,
        maxHops = maxHops,
        signature = signature,
        createdAtEpochMs = createdAtEpochMs
    )

    private fun WireMessage.toEntity() = MessageEntity(
        messageId = messageId,
        senderPublicKey = senderPublicKey,
        senderAlias = senderAlias,
        content = content,
        latitude = latitude,
        longitude = longitude,
        priority = priority,
        hopCount = hopCount,
        maxHops = maxHops,
        signature = signature,
        createdAtEpochMs = createdAtEpochMs,
        receivedAtEpochMs = System.currentTimeMillis(),
        isSynced = false,
        isOwnMessage = false
    )
}
