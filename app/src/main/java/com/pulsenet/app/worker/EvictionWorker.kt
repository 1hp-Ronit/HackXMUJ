package com.pulsenet.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pulsenet.app.data.local.dao.MessageDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic storage triage: if the local vault grows past a rough 50MB estimate,
 * evict the oldest synced non-SOS messages first. Unsynced and SOS messages are
 * never touched — MessageDao.evictOldMessages already restricts to
 * `priority > 0 AND isSynced = 1`.
 */
@HiltWorker
class EvictionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageDao: MessageDao
) : CoroutineWorker(context, params) {

    private companion object {
        const val TAG = "EvictionWorker"
        const val AVG_MESSAGE_SIZE_BYTES = 500L
        const val STORAGE_THRESHOLD_BYTES = 50L * 1024 * 1024
        const val EVICTION_BATCH_SIZE = 200
    }

    override suspend fun doWork(): Result {
        val messageCount = messageDao.getMessageCount()
        val estimatedSizeBytes = messageCount * AVG_MESSAGE_SIZE_BYTES

        if (estimatedSizeBytes > STORAGE_THRESHOLD_BYTES) {
            val excessBytes = estimatedSizeBytes - STORAGE_THRESHOLD_BYTES
            val toEvict = (excessBytes / AVG_MESSAGE_SIZE_BYTES)
                .coerceAtLeast(1)
                .coerceAtMost(EVICTION_BATCH_SIZE.toLong())
                .toInt()
            messageDao.evictOldMessages(toEvict)
            Log.i(
                TAG,
                "Storage triage: evicted up to $toEvict synced non-SOS messages " +
                    "(estimated ${estimatedSizeBytes / 1024}KB before eviction)"
            )
        } else {
            Log.i(TAG, "Storage triage: $messageCount messages (~${estimatedSizeBytes / 1024}KB), under threshold")
        }
        return Result.success()
    }
}
