package com.pulsenet.app.domain.model

import com.pulsenet.app.data.local.entity.MessageEntity

data class Message(
    val messageId: String,
    val senderPublicKey: String,
    val senderAlias: String,
    val content: String,
    val latitude: Double,
    val longitude: Double,
    val priority: Priority,
    val hopCount: Int,
    val maxHops: Int,
    val signature: String,
    val createdAtEpochMs: Long,
    val receivedAtEpochMs: Long,
    val isSynced: Boolean,
    val isOwnMessage: Boolean
)

fun MessageEntity.toDomain(): Message = Message(
    messageId = messageId,
    senderPublicKey = senderPublicKey,
    senderAlias = senderAlias,
    content = content,
    latitude = latitude,
    longitude = longitude,
    priority = Priority.fromValue(priority),
    hopCount = hopCount,
    maxHops = maxHops,
    signature = signature,
    createdAtEpochMs = createdAtEpochMs,
    receivedAtEpochMs = receivedAtEpochMs,
    isSynced = isSynced,
    isOwnMessage = isOwnMessage
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    messageId = messageId,
    senderPublicKey = senderPublicKey,
    senderAlias = senderAlias,
    content = content,
    latitude = latitude,
    longitude = longitude,
    priority = priority.value,
    hopCount = hopCount,
    maxHops = maxHops,
    signature = signature,
    createdAtEpochMs = createdAtEpochMs,
    receivedAtEpochMs = receivedAtEpochMs,
    isSynced = isSynced,
    isOwnMessage = isOwnMessage
)
