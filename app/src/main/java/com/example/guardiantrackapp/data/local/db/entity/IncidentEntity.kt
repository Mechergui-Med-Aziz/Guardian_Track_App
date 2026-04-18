package com.example.guardiantrackapp.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String, // FALL | BATTERY | MANUAL
    val latitude: Double,
    val longitude: Double,
    val isSynced: Boolean = false
)
