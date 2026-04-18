package com.example.guardiantrackapp.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.guardiantrackapp.data.remote.NetworkResult
import com.example.guardiantrackapp.data.repository.IncidentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager CoroutineWorker for deferred synchronization of unsynced incidents.
 * Constrained to NETWORK_CONNECTED — runs when connectivity is restored.
 * Also used as an expedited worker for BOOT_COMPLETED on API 31+.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val incidentRepository: IncidentRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "SyncWorker started — syncing unsynced incidents...")

        return try {
            val unsyncedIncidents = incidentRepository.getUnsyncedIncidents()

            if (unsyncedIncidents.isEmpty()) {
                Log.i(TAG, "No unsynced incidents found.")
                return Result.success()
            }

            Log.i(TAG, "Found ${unsyncedIncidents.size} unsynced incidents")
            var allSuccess = true

            unsyncedIncidents.forEach { incident ->
                when (val result = incidentRepository.syncIncident(incident)) {
                    is NetworkResult.Success -> {
                        Log.i(TAG, "✅ Incident ${incident.id} synced successfully")
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "❌ Failed to sync incident ${incident.id}: ${result.message}")
                        allSuccess = false
                    }
                    is NetworkResult.Loading -> { /* Should not happen here */ }
                }
            }

            if (allSuccess) {
                Log.i(TAG, "All incidents synced successfully")
                Result.success()
            } else {
                Log.w(TAG, "Some incidents failed to sync. Retrying...")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker failed: ${e.message}", e)
            Result.retry()
        }
    }
}
