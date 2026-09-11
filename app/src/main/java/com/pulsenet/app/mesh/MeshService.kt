package com.pulsenet.app.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pulsenet.app.BuildConfig
import com.pulsenet.app.R
import com.pulsenet.app.data.local.DemoSeeder
import com.pulsenet.app.data.local.UserPreferences
import com.pulsenet.app.data.local.dao.MessageDao
import com.pulsenet.app.sensor.DistressSensorManager
import com.pulsenet.app.worker.EvictionWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Foreground service hosting the mesh engine and sensor listener, so both keep
 * running through Doze Mode instead of only while the app is in the foreground.
 */
@AndroidEntryPoint
class MeshService : Service() {

    private companion object {
        const val NOTIFICATION_ID = 1
    }

    @Inject lateinit var nearbyMeshManager: NearbyMeshManager
    @Inject lateinit var gossipEngine: GossipEngine
    @Inject lateinit var distressSensorManager: DistressSensorManager
    @Inject lateinit var bridgeManager: BridgeManager
    @Inject lateinit var messageDao: MessageDao
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var demoSeeder: DemoSeeder

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundWithNotification()

        nearbyMeshManager.listener = gossipEngine
        distressSensorManager.start()
        bridgeManager.start()
        scheduleEvictionWorker()

        serviceScope.launch {
            nearbyMeshManager.start(userPreferences.getAliasSnapshot())
        }
        if (BuildConfig.DEBUG) {
            serviceScope.launch { demoSeeder.seedIfNeeded() }
        }
        observeMeshState()
    }

    private fun scheduleEvictionWorker() {
        val request = PeriodicWorkRequestBuilder<EvictionWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "eviction_triage",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        distressSensorManager.stop()
        bridgeManager.stop()
        nearbyMeshManager.stopAll()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeMeshState() {
        serviceScope.launch {
            combine(
                nearbyMeshManager.connectedPeers,
                messageDao.observeAllMessages().map { it.size }
            ) { peers, messageCount -> peers.size to messageCount }
                .collect { (peerCount, messageCount) -> updateNotification(peerCount, messageCount) }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            MESH_NOTIFICATION_CHANNEL_ID,
            getString(R.string.mesh_service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(peerCount: Int, messageCount: Int): Notification =
        NotificationCompat.Builder(this, MESH_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.mesh_service_notification_title))
            .setContentText("$peerCount peers nearby • $messageCount messages cached")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun startForegroundWithNotification() {
        val notification = buildNotification(peerCount = 0, messageCount = 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(peerCount: Int, messageCount: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(peerCount, messageCount))
    }
}
