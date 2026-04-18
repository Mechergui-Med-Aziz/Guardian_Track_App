package com.example.guardiantrackapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.guardiantrackapp.service.ServiceStarterWorker
import com.example.guardiantrackapp.service.SurveillanceService

/**
 * Static BroadcastReceiver for ACTION_BOOT_COMPLETED.
 * Restarts the SurveillanceService after device reboot.
 *
 * Android 12+ (API 31) constraint:
 * Cannot start foreground services directly from a BroadcastReceiver.
 * Uses WorkManager with setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
 * as the solution de repli.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
        private const val WORK_NAME = "start_surveillance_after_boot"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "📱 Boot completed — restarting surveillance service")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ — Use WorkManager with setExpedited as workaround
                Log.d(TAG, "API 31+ detected. Using WorkManager expedited work.")
                val workRequest = OneTimeWorkRequestBuilder<ServiceStarterWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            } else {
                // API < 31 — Start foreground service directly
                Log.d(TAG, "API < 31. Starting service directly.")
                SurveillanceService.startService(context)
            }
        }
    }
}
