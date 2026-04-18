package com.example.guardiantrackapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.guardiantrackapp.service.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class GuardianTrackApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    companion object {
        private const val TAG = "GuardianTrackApp"
        const val SURVEILLANCE_CHANNEL_ID = "surveillance_channel"
        const val ALERT_CHANNEL_ID = "alert_channel"
        const val SMS_SIMULATION_CHANNEL_ID = "sms_simulation_channel"
        const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        schedulePeriodicSync()
    }

    private fun createNotificationChannels() {
        val surveillanceChannel = NotificationChannel(
            SURVEILLANCE_CHANNEL_ID,
            "Surveillance Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification persistante du service de surveillance"
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Alertes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertes critiques (chute détectée, batterie faible)"
            enableVibration(true)
        }

        val smsSimulationChannel = NotificationChannel(
            SMS_SIMULATION_CHANNEL_ID,
            "Simulation SMS",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications simulant l'envoi de SMS"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(surveillanceChannel)
        notificationManager.createNotificationChannel(alertChannel)
        notificationManager.createNotificationChannel(smsSimulationChannel)
    }

    /**
     * Schedule a periodic SyncWorker that runs every 15 minutes when network is available.
     * This ensures that unsynced incidents are automatically synchronized
     * as soon as the device connects to the internet.
     */
    private fun schedulePeriodicSync() {
        Log.i(TAG, "Scheduling periodic sync worker...")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncWork = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncWork
        )

        Log.i(TAG, "Periodic sync worker scheduled (every 15 min, when network available)")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
