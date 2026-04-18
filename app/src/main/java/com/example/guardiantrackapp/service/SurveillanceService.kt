package com.example.guardiantrackapp.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.HandlerThread
import android.os.IBinder
import android.os.Handler
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.guardiantrackapp.GuardianTrackApplication
import com.example.guardiantrackapp.MainActivity
import com.example.guardiantrackapp.R
import com.example.guardiantrackapp.data.local.datastore.PreferencesManager
import com.example.guardiantrackapp.data.local.db.entity.IncidentEntity
import com.example.guardiantrackapp.data.repository.IncidentRepository
import com.example.guardiantrackapp.util.FallDetector
import com.example.guardiantrackapp.util.LocationHelper
import com.example.guardiantrackapp.util.SmsHelper
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SurveillanceService : Service(), SensorEventListener, FallDetector.FallListener {

    companion object {
        private const val TAG = "SurveillanceService"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.example.guardiantrackapp.action.START_SURVEILLANCE"
        const val ACTION_STOP = "com.example.guardiantrackapp.action.STOP_SURVEILLANCE"

        fun startService(context: Context) {
            val intent = Intent(context, SurveillanceService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SurveillanceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var incidentRepository: IncidentRepository
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val fallDetector = FallDetector()
    private lateinit var sensorThread: HandlerThread
    private lateinit var sensorHandler: Handler

    // Current sensor values for UI observation
    private var lastAx = 0f
    private var lastAy = 0f
    private var lastAz = 0f

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SurveillanceService created")

        // Set up dedicated thread for sensor processing
        sensorThread = HandlerThread("SensorThread").also { it.start() }
        sensorHandler = Handler(sensorThread.looper)

        // Initialize sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Set up fall detector
        fallDetector.listener = this

        // Observe sensitivity threshold changes
        serviceScope.launch {
            preferencesManager.sensitivityThreshold.collect { threshold ->
                fallDetector.updateThreshold(threshold)
                Log.d(TAG, "Sensitivity threshold updated: $threshold m/s²")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startSensorListening()
                Log.i(TAG, "Surveillance started")
            }
        }
        return START_STICKY
    }

    private fun startSensorListening() {
        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME,
                sensorHandler
            )
            Log.d(TAG, "Accelerometer listener registered on dedicated thread")
        } ?: run {
            Log.e(TAG, "No accelerometer sensor available on this device")
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GuardianTrackApplication.SURVEILLANCE_CHANNEL_ID)
            .setContentTitle("GuardianTrack — Surveillance active")
            .setContentText("Surveillance de l'accéléromètre en cours...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // --- SensorEventListener ---

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                lastAx = it.values[0]
                lastAy = it.values[1]
                lastAz = it.values[2]
                fallDetector.processSensorData(
                    lastAx, lastAy, lastAz,
                    System.currentTimeMillis()
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for fall detection
    }

    // --- FallDetector.FallListener ---

    override fun onFallDetected(magnitude: Float) {
        Log.w(TAG, "🚨 FALL DETECTED! Magnitude: $magnitude m/s²")
        serviceScope.launch {
            handleFallDetected(magnitude)
        }
    }

    private suspend fun handleFallDetected(magnitude: Float) {
        // Get current location
        val coordinates = LocationHelper.getCurrentLocation(this@SurveillanceService, fusedLocationClient)

        // Create incident
        val incident = IncidentEntity(
            timestamp = System.currentTimeMillis(),
            type = "FALL",
            latitude = coordinates.latitude,
            longitude = coordinates.longitude,
            isSynced = false
        )

        // Save to Room and attempt sync
        val incidentId = incidentRepository.insertIncident(incident)
        Log.i(TAG, "Fall incident saved with id: $incidentId")

        // Schedule WorkManager sync for unsynced incidents
        scheduleSyncWork()

        // Send emergency SMS
        val simulationMode = preferencesManager.smsSimulationMode.first()
        val emergencyNumber = preferencesManager.getEmergencyNumber()
        SmsHelper.sendEmergencySms(
            context = this@SurveillanceService,
            phoneNumber = emergencyNumber,
            message = "⚠️ ALERTE GUARDIANTRACK — Chute détectée ! " +
                    "Position: ${coordinates.latitude}, ${coordinates.longitude} | " +
                    "Magnitude: ${"%.1f".format(magnitude)} m/s²",
            simulationMode = simulationMode
        )

        // Show alert notification
        showAlertNotification(magnitude, coordinates)
    }

    private fun showAlertNotification(
        magnitude: Float,
        coordinates: LocationHelper.Coordinates
    ) {
        val notification = NotificationCompat.Builder(
            this,
            GuardianTrackApplication.ALERT_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🚨 Chute détectée !")
            .setContentText("Magnitude: ${"%.1f".format(magnitude)} m/s²")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Une chute a été détectée.\n" +
                                "Magnitude: ${"%.1f".format(magnitude)} m/s²\n" +
                                "Position: ${coordinates.latitude}, ${coordinates.longitude}\n" +
                                "Un SMS d'urgence a été envoyé."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(2000, notification)
    }

    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(syncWork)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        sensorThread.quitSafely()
        serviceScope.cancel()
        Log.d(TAG, "SurveillanceService destroyed")
    }
}
