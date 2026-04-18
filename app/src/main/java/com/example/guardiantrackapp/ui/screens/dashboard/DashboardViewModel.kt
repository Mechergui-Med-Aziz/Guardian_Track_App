package com.example.guardiantrackapp.ui.screens.dashboard

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardiantrackapp.data.local.datastore.PreferencesManager
import com.example.guardiantrackapp.data.local.db.entity.IncidentEntity
import com.example.guardiantrackapp.data.repository.IncidentRepository
import com.example.guardiantrackapp.util.LocationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val accelerometerX: Float = 0f,
    val accelerometerY: Float = 0f,
    val accelerometerZ: Float = 0f,
    val magnitude: Float = 9.8f,
    val batteryLevel: Int = -1,
    val isGpsEnabled: Boolean = false,
    val isServiceRunning: Boolean = false,
    val incidentCount: Int = 0,
    val unsyncedCount: Int = 0,
    val isAlertSending: Boolean = false,
    val lastAlertMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val incidentRepository: IncidentRepository,
    private val preferencesManager: PreferencesManager,
    private val fusedLocationClient: FusedLocationProviderClient
) : AndroidViewModel(application), SensorEventListener {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    init {
        // Register accelerometer for dashboard display
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        // Observe incidents count
        viewModelScope.launch {
            incidentRepository.getAllIncidents().collect { incidents ->
                _uiState.value = _uiState.value.copy(
                    incidentCount = incidents.size,
                    unsyncedCount = incidents.count { !it.isSynced }
                )
            }
        }

        // Periodically check battery and GPS status
        updateBatteryStatus()
        updateGpsStatus()
    }

    private fun updateBatteryStatus() {
        val context = getApplication<Application>()
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = if (scale > 0) (level * 100) / scale else -1
        _uiState.value = _uiState.value.copy(batteryLevel = batteryPercent)
    }

    private fun updateGpsStatus() {
        val context = getApplication<Application>()
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        _uiState.value = _uiState.value.copy(isGpsEnabled = isGpsEnabled)
    }

    fun setServiceRunning(running: Boolean) {
        _uiState.value = _uiState.value.copy(isServiceRunning = running)
    }

    fun triggerManualAlert() {
        _uiState.value = _uiState.value.copy(isAlertSending = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val coordinates = LocationHelper.getCurrentLocation(context, fusedLocationClient)

                val incident = IncidentEntity(
                    timestamp = System.currentTimeMillis(),
                    type = "MANUAL",
                    latitude = coordinates.latitude,
                    longitude = coordinates.longitude,
                    isSynced = false
                )

                incidentRepository.insertIncident(incident)

                // Send SMS
                val simulationMode = preferencesManager.smsSimulationMode.first()
                val emergencyNumber = preferencesManager.getEmergencyNumber()
                com.example.guardiantrackapp.util.SmsHelper.sendEmergencySms(
                    context = context,
                    phoneNumber = emergencyNumber,
                    message = "⚠️ ALERTE GUARDIANTRACK — Alerte manuelle déclenchée ! " +
                            "Position: ${coordinates.latitude}, ${coordinates.longitude}",
                    simulationMode = simulationMode
                )

                _uiState.value = _uiState.value.copy(
                    isAlertSending = false,
                    lastAlertMessage = "Alerte envoyée avec succès"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAlertSending = false,
                    lastAlertMessage = "Erreur: ${e.message}"
                )
            }
        }
    }

    fun clearAlertMessage() {
        _uiState.value = _uiState.value.copy(lastAlertMessage = null)
    }

    fun refreshStatus() {
        updateBatteryStatus()
        updateGpsStatus()
    }

    // --- SensorEventListener ---

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val ax = it.values[0]
                val ay = it.values[1]
                val az = it.values[2]
                val mag = kotlin.math.sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
                _uiState.value = _uiState.value.copy(
                    accelerometerX = ax,
                    accelerometerY = ay,
                    accelerometerZ = az,
                    magnitude = mag
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }
}
