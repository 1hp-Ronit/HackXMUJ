package com.pulsenet.app.worker

/**
 * Abstraction over "go try to sync now", so callers (GossipEngine,
 * SendMessageUseCase) can be unit tested against a fake instead of a real
 * CloudSyncScheduler, which needs a live WorkManager/Context.
 */
fun interface SyncTrigger {
    fun scheduleFlush()
}
