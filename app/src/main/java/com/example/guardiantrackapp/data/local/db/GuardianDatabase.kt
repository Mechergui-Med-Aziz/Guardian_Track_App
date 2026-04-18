package com.example.guardiantrackapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.guardiantrackapp.data.local.db.dao.EmergencyContactDao
import com.example.guardiantrackapp.data.local.db.dao.IncidentDao
import com.example.guardiantrackapp.data.local.db.entity.EmergencyContactEntity
import com.example.guardiantrackapp.data.local.db.entity.IncidentEntity

@Database(
    entities = [IncidentEntity::class, EmergencyContactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        const val DATABASE_NAME = "guardian_track_db"
    }
}
