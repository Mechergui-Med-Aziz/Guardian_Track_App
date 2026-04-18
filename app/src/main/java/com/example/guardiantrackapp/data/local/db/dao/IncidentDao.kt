package com.example.guardiantrackapp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.guardiantrackapp.data.local.db.entity.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity): Long

    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE isSynced = 0")
    suspend fun getUnsyncedIncidents(): List<IncidentEntity>

    @Query("UPDATE incidents SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncident(id: Long)

    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    suspend fun getAllIncidentsList(): List<IncidentEntity>
}
