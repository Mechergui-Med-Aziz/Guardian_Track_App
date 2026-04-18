package com.example.guardiantrackapp.data.repository

import com.example.guardiantrackapp.data.local.datastore.PreferencesManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    val sensitivityThreshold: Flow<Float> = preferencesManager.sensitivityThreshold
    val darkModeEnabled: Flow<Boolean> = preferencesManager.darkModeEnabled
    val smsSimulationMode: Flow<Boolean> = preferencesManager.smsSimulationMode

    suspend fun setSensitivityThreshold(threshold: Float) {
        preferencesManager.setSensitivityThreshold(threshold)
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        preferencesManager.setDarkModeEnabled(enabled)
    }

    suspend fun setSmsSimulationMode(enabled: Boolean) {
        preferencesManager.setSmsSimulationMode(enabled)
    }

    fun getEmergencyNumber(): String {
        return preferencesManager.getEmergencyNumber()
    }

    fun setEmergencyNumber(number: String) {
        preferencesManager.setEmergencyNumber(number)
    }
}
