package com.example.guardiantrackapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.guardiantrackapp.GuardianTrackApplication
import com.example.guardiantrackapp.R
import com.example.guardiantrackapp.data.local.datastore.PreferencesManager
import com.example.guardiantrackapp.data.local.db.entity.IncidentEntity
import com.example.guardiantrackapp.data.repository.IncidentRepository
import com.example.guardiantrackapp.util.SmsHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class BatteryLowReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BatteryLowReceiver"
    }

    @Inject lateinit var incidentRepository: IncidentRepository
    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_LOW) {
            Log.w(TAG, "🔋 Battery low detected!")

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Create battery critical incident
                    val incident = IncidentEntity(
                        timestamp = System.currentTimeMillis(),
                        type = "BATTERY",
                        latitude = 0.0, // No GPS during battery critical
                        longitude = 0.0,
                        isSynced = false
                    )

                    incidentRepository.insertIncident(incident)
                    Log.i(TAG, "Battery critical incident saved to Room")

                    // Send last resort notification
                    showLastResortNotification(context)

                    // Attempt emergency SMS
                    val simulationMode = preferencesManager.smsSimulationMode.first()
                    val emergencyNumber = preferencesManager.getEmergencyNumber()
                    SmsHelper.sendEmergencySms(
                        context = context,
                        phoneNumber = emergencyNumber,
                        message = "⚠️ ALERTE GUARDIANTRACK — Batterie critique ! " +
                                "L'appareil va bientôt s'éteindre.",
                        simulationMode = simulationMode
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling battery low: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showLastResortNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        val notification = NotificationCompat.Builder(
            context,
            GuardianTrackApplication.ALERT_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔋 Batterie critique !")
            .setContentText("Batterie faible — alerte de dernier recours envoyée.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "La batterie de l'appareil est critique.\n" +
                                "Un SMS d'urgence a été envoyé au contact d'urgence.\n" +
                                "L'incident a été enregistré localement."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3000, notification)
    }
}
