package com.pulsenet.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.data.local.dao.PeerDao
import com.pulsenet.app.data.local.entity.MessageEntity
import com.pulsenet.app.data.local.entity.PeerEntity

@Database(
    entities = [MessageEntity::class, PeerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun peerDao(): PeerDao
}
