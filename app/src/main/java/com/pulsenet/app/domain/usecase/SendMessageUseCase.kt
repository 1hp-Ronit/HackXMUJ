package com.pulsenet.app.domain.usecase

import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.data.local.entity.MessageEntity
import com.pulsenet.app.domain.model.Priority
import com.pulsenet.app.mesh.GossipEngine
import com.pulsenet.app.security.KeySigner
import com.pulsenet.app.security.MessageSigner
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Originates a new message on this device: sign it, persist it locally, and push
 * it out to any already-connected peers immediately rather than waiting for the
 * next gossip sync round. Shared by manual message composition and SOS triggers.
 */
@Singleton
class SendMessageUseCase @Inject constructor(
    private val messageDao: MessageDao,
    private val messageSigner: MessageSigner,
    private val keySigner: KeySigner,
    private val gossipEngine: GossipEngine
) {
    suspend operator fun invoke(
        content: String,
        latitude: Double,
        longitude: Double,
        priority: Priority,
        senderAlias: String
    ): MessageEntity {
        val messageId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        val signature = messageSigner.sign(messageId, content, latitude, longitude, priority.value, createdAt)

        val entity = MessageEntity(
            messageId = messageId,
            senderPublicKey = keySigner.getPublicKeyBase64(),
            senderAlias = senderAlias,
            content = content,
            latitude = latitude,
            longitude = longitude,
            priority = priority.value,
            hopCount = 0,
            signature = signature,
            createdAtEpochMs = createdAt,
            receivedAtEpochMs = createdAt,
            isOwnMessage = true
        )

        messageDao.insertMessage(entity)
        gossipEngine.broadcastOwnMessage(entity)
        return entity
    }
}
