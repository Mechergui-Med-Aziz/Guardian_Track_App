package com.example.guardiantrackapp.data.remote.api.dto

import com.google.gson.annotations.SerializedName

data class IncidentDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("type")
    val type: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("localId")
    val localId: Long
)
