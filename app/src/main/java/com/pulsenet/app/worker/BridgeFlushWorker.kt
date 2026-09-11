package com.pulsenet.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pulsenet.app.data.repository.SyncRepository
import com.pulsenet.app.data.repository.TranslationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Triggered by BridgeManager once internet connectivity is detected. */
@HiltWorker
class BridgeFlushWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val translationService: TranslationService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val success = syncRepository.flushUnsyncedMessages(translationService::translateAndClassify)
        if (success) Result.success() else Result.retry()
    } catch (e: Exception) {
        Result.retry()
    }
}
