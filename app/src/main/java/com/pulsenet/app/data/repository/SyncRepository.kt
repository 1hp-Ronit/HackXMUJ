package com.pulsenet.app.data.repository

import com.pulsenet.app.BuildConfig
import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.data.local.entity.MessageEntity
import com.pulsenet.app.data.remote.AtlasApiService
import com.pulsenet.app.data.remote.AtlasMessageDocument
import com.pulsenet.app.data.remote.GeoJsonPoint
import com.pulsenet.app.data.remote.InsertManyRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class Syncing(val remaining: Int) : SyncStatus
    data class Completed(val syncedCount: Int) : SyncStatus
    data class Failed(val message: String) : SyncStatus
}

/**
 * Flushes unsynced Room messages to MongoDB Atlas in batches. [BridgeFlushWorker]
 * calls [flushUnsyncedMessages]; this class owns batching, GeoJSON conversion
 * (longitude first, per the 2dsphere index requirement), and exposes progress so
 * a notification or UI can show it.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val atlasApiService: AtlasApiService
) {
    private companion object {
        const val BATCH_SIZE = 50
        const val DATABASE_NAME = "pulsenet"
        const val COLLECTION_NAME = "messages"
        const val DATA_SOURCE = "PulseNet"
    }

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /**
     * Uploads every unsynced message, returning true only if all batches succeeded.
     * [translate] is called per message before upload — a no-op by default; the
     * Sarvam-backed translation + severity tagging step plugs in here.
     */
    suspend fun flushUnsyncedMessages(
        translate: suspend (MessageEntity) -> Pair<String?, String?> = { null to null }
    ): Boolean {
        val unsynced = messageDao.getUnsyncedMessages()
        if (unsynced.isEmpty()) {
            _syncStatus.value = SyncStatus.Completed(0)
            return true
        }

        var totalSynced = 0
        for (batch in unsynced.chunked(BATCH_SIZE)) {
            _syncStatus.value = SyncStatus.Syncing(remaining = unsynced.size - totalSynced)

            val documents = batch.map { message ->
                val (translated, severity) = translate(message)
                message.toAtlasDocument(translated, severity)
            }
            val request = InsertManyRequest(
                collection = COLLECTION_NAME,
                database = DATABASE_NAME,
                dataSource = DATA_SOURCE,
                documents = documents
            )

            val response = try {
                atlasApiService.insertMany(BuildConfig.ATLAS_API_KEY, request)
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Failed(e.message ?: "Network error")
                return false
            }
            if (!response.isSuccessful) {
                _syncStatus.value = SyncStatus.Failed("HTTP ${response.code()}")
                return false
            }

            messageDao.markSynced(batch.map { it.messageId })
            totalSynced += batch.size
        }

        _syncStatus.value = SyncStatus.Completed(totalSynced)
        return true
    }

    private fun MessageEntity.toAtlasDocument(translatedContent: String?, severityTag: String?) =
        AtlasMessageDocument(
            messageId = messageId,
            senderAlias = senderAlias,
            senderPublicKey = senderPublicKey,
            content = content,
            location = GeoJsonPoint(coordinates = listOf(longitude, latitude)),
            priority = priority,
            hopCount = hopCount,
            signature = signature,
            createdAt = Instant.ofEpochMilli(createdAtEpochMs).toString(),
            translatedContent = translatedContent,
            severityTag = severityTag
        )
}
