package com.pulsenet.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pulsenet.app.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(messages: List<MessageEntity>): List<Long>

    @Query("SELECT messageId FROM messages")
    suspend fun getAllMessageIds(): List<String>

    @Query("SELECT * FROM messages WHERE messageId IN (:ids)")
    suspend fun getMessagesByIds(ids: List<String>): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE messageId = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE isSynced = 0 ORDER BY priority ASC, createdAtEpochMs ASC")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    @Query("UPDATE messages SET isSynced = 1 WHERE messageId IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    // Triage eviction: delete lowest-priority, oldest messages first.
    // priority > 0 excludes SOS messages, which are never evicted.
    @Query(
        """
        DELETE FROM messages WHERE messageId IN (
            SELECT messageId FROM messages
            WHERE priority > 0 AND isSynced = 1
            ORDER BY priority DESC, createdAtEpochMs ASC
            LIMIT :count
        )
        """
    )
    suspend fun evictOldMessages(count: Int)

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int

    @Query("SELECT * FROM messages ORDER BY priority ASC, createdAtEpochMs DESC")
    fun observeAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE priority = 0 ORDER BY createdAtEpochMs DESC")
    fun observeSOSMessages(): Flow<List<MessageEntity>>
}
