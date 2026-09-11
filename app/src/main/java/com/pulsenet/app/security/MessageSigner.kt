package com.pulsenet.app.security

import com.pulsenet.app.data.local.entity.MessageEntity
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageSigner @Inject constructor(
    private val keySigner: KeySigner
) {

    private fun buildPayload(
        messageId: String,
        content: String,
        latitude: Double,
        longitude: Double,
        priority: Int,
        createdAtEpochMs: Long
    ): ByteArray = "$messageId|$content|$latitude|$longitude|$priority|$createdAtEpochMs"
        .toByteArray(StandardCharsets.UTF_8)

    fun sign(
        messageId: String,
        content: String,
        latitude: Double,
        longitude: Double,
        priority: Int,
        createdAtEpochMs: Long
    ): String {
        val payload = buildPayload(messageId, content, latitude, longitude, priority, createdAtEpochMs)
        return Base64.getEncoder().encodeToString(keySigner.sign(payload))
    }

    fun verify(message: MessageEntity): Boolean {
        val payload = buildPayload(
            message.messageId,
            message.content,
            message.latitude,
            message.longitude,
            message.priority,
            message.createdAtEpochMs
        )
        val signatureBytes = try {
            Base64.getDecoder().decode(message.signature)
        } catch (e: IllegalArgumentException) {
            return false
        }
        return keySigner.verify(payload, signatureBytes, message.senderPublicKey)
    }
}
