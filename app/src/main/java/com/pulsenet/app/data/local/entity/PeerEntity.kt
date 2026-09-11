package com.pulsenet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val endpointId: String,
    val alias: String,
    val publicKey: String,
    val lastSeenEpochMs: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)
