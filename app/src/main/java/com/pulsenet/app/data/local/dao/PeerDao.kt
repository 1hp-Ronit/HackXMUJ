package com.pulsenet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pulsenet.app.data.local.entity.PeerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePeer(peer: PeerEntity)

    @Query("SELECT * FROM peers ORDER BY lastSeenEpochMs DESC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Query("DELETE FROM peers WHERE lastSeenEpochMs < :cutoffEpochMs")
    suspend fun deleteStalePeers(cutoffEpochMs: Long)
}
