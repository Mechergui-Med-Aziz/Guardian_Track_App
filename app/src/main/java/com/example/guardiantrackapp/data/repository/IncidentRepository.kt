package com.example.guardiantrackapp.data.repository

import android.util.Log
import com.example.guardiantrackapp.data.local.db.dao.IncidentDao
import com.example.guardiantrackapp.data.local.db.entity.IncidentEntity
import com.example.guardiantrackapp.data.remote.NetworkResult
import com.example.guardiantrackapp.data.remote.api.GuardianApiService
import com.example.guardiantrackapp.data.remote.api.dto.IncidentDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRepository @Inject constructor(
    private val incidentDao: IncidentDao,
    private val apiService: GuardianApiService
) {

    companion object {
        private const val TAG = "IncidentRepository"
    }

    fun getAllIncidents(): Flow<List<IncidentEntity>> {
        return incidentDao.getAllIncidents()
    }


    suspend fun insertIncident(incident: IncidentEntity): Long {
        val id = incidentDao.insertIncident(incident)
        // Attempt immediate sync
        try {
            val dto = IncidentDto(
                timestamp = incident.timestamp,
                type = incident.type,
                latitude = incident.latitude,
                longitude = incident.longitude,
                localId = id
            )
            val response = apiService.sendIncident(dto)
            if (response.isSuccessful) {
                incidentDao.markAsSynced(id)
                Log.i(TAG, "Incident $id synced immediately on insert")
            } else {
                Log.w(TAG, "Immediate sync failed for incident $id: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            // Network unavailable — incident stays unsynced for WorkManager
            Log.w(TAG, "Immediate sync failed for incident $id: ${e.message}")
        }
        return id
    }

    suspend fun deleteIncident(id: Long) {
        incidentDao.deleteIncident(id)
    }

    suspend fun getUnsyncedIncidents(): List<IncidentEntity> {
        return incidentDao.getUnsyncedIncidents()
    }

    suspend fun markAsSynced(id: Long) {
        incidentDao.markAsSynced(id)
    }

    suspend fun getAllIncidentsList(): List<IncidentEntity> {
        return incidentDao.getAllIncidentsList()
    }

    /**
     * Sync a single incident to the remote API.
     * Only checks for HTTP success (200-299) — the server response body
     * is not parsed as IncidentDto since Beeceptor returns a generic response.
     * Returns NetworkResult indicating success or failure.
     */
    suspend fun syncIncident(incident: IncidentEntity): NetworkResult<Boolean> {
        return try {
            val dto = IncidentDto(
                timestamp = incident.timestamp,
                type = incident.type,
                latitude = incident.latitude,
                longitude = incident.longitude,
                localId = incident.id
            )
            val response = apiService.sendIncident(dto)
            if (response.isSuccessful) {
                incidentDao.markAsSynced(incident.id)
                Log.i(TAG, "✅ Incident ${incident.id} synced successfully (HTTP ${response.code()})")
                NetworkResult.Success(true)
            } else {
                Log.e(TAG, "❌ Sync failed for incident ${incident.id}: HTTP ${response.code()} ${response.message()}")
                NetworkResult.Error(
                    message = "HTTP ${response.code()}: ${response.message()}",
                    code = response.code()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync exception for incident ${incident.id}: ${e.message}")
            NetworkResult.Error(message = e.message ?: "Network error")
        }
    }
}
