package com.pulsenet.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Requests a cloud flush. The work carries a CONNECTED constraint, so callers
 * don't need to care whether the device currently has internet — WorkManager
 * runs it now if online, or holds it until connectivity returns.
 */
@Singleton
class CloudSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val WORK_NAME = "bridge_flush"
    }

    fun scheduleFlush() {
        val request = OneTimeWorkRequestBuilder<BridgeFlushWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        // REPLACE, not KEEP: an in-flight flush may have already read its list of
        // unsynced messages before this newest one was written, and restarting is
        // cheap since batches are marked synced as they succeed.
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
