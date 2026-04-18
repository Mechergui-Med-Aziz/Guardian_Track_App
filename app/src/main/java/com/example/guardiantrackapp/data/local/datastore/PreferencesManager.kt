package com.example.guardiantrackapp.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "guardian_settings")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    // Encrypted SharedPreferences for sensitive data
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "guardian_encrypted_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        val SENSITIVITY_THRESHOLD = floatPreferencesKey("sensitivity_threshold")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val SMS_SIMULATION_MODE = booleanPreferencesKey("sms_simulation_mode")
        const val ENCRYPTED_EMERGENCY_NUMBER_KEY = "encrypted_emergency_number"
        const val ENCRYPTED_API_KEY = "encrypted_api_key"
    }

    // --- DataStore Preferences ---

    val sensitivityThreshold: Flow<Float> = dataStore.data.map { prefs ->
        prefs[SENSITIVITY_THRESHOLD] ?: 15.0f
    }

    suspend fun setSensitivityThreshold(threshold: Float) {
        dataStore.edit { prefs ->
            prefs[SENSITIVITY_THRESHOLD] = threshold
        }
    }

    val darkModeEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DARK_MODE_ENABLED] ?: false
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DARK_MODE_ENABLED] = enabled
        }
    }

    val smsSimulationMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SMS_SIMULATION_MODE] ?: true // Default: ACTIVE (simulation ON)
    }

    suspend fun setSmsSimulationMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SMS_SIMULATION_MODE] = enabled
        }
    }

    // --- EncryptedSharedPreferences (for sensitive data) ---

    fun getEmergencyNumber(): String {
        return encryptedPrefs.getString(ENCRYPTED_EMERGENCY_NUMBER_KEY, "") ?: ""
    }

    fun setEmergencyNumber(number: String) {
        encryptedPrefs.edit().putString(ENCRYPTED_EMERGENCY_NUMBER_KEY, number).apply()
    }

    fun getApiKey(): String {
        return encryptedPrefs.getString(ENCRYPTED_API_KEY, "") ?: ""
    }

    fun setApiKey(key: String) {
        encryptedPrefs.edit().putString(ENCRYPTED_API_KEY, key).apply()
    }
}
