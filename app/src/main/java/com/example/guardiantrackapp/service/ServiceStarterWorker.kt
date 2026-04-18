package com.example.guardiantrackapp.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


@HiltWorker
class ServiceStarterWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ServiceStarterWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting SurveillanceService from WorkManager...")
            SurveillanceService.startService(applicationContext)
            Log.i(TAG, "SurveillanceService start command sent")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service: ${e.message}", e)
            Result.failure()
        }
    }
}
