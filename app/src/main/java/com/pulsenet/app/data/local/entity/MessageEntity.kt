package com.pulsenet.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [Index(value = ["messageId"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val senderPublicKey: String,
    val senderAlias: String,
    val content: String,
    val latitude: Double,
    val longitude: Double,
    val priority: Int,
    val hopCount: Int,
    val maxHops: Int = 7,
    val signature: String,
    val createdAtEpochMs: Long,
    val receivedAtEpochMs: Long,
    val isSynced: Boolean = false,
    val isOwnMessage: Boolean = false
)
