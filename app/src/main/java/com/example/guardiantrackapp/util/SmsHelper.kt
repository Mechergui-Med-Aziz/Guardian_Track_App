package com.example.guardiantrackapp.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.guardiantrackapp.GuardianTrackApplication
import com.example.guardiantrackapp.R

/**
 * Helper class for sending SMS messages.
 * Supports simulation mode (default) which shows a notification + log instead of real SMS.
 */
object SmsHelper {

    private const val TAG = "SmsHelper"
    private var notificationId = 5000

    /**
     * Send an emergency SMS or simulate it based on the simulation mode flag.
     *
     * @param context Application context
     * @param phoneNumber The emergency phone number
     * @param message The SMS message content
     * @param simulationMode If true, simulates SMS via notification and log
     */
    fun sendEmergencySms(
        context: Context,
        phoneNumber: String,
        message: String,
        simulationMode: Boolean
    ) {
        if (phoneNumber.isBlank()) {
            Log.w(TAG, "No emergency number configured. SMS not sent.")
            return
        }

        if (simulationMode) {
            // SIMULATION MODE: Log + Notification
            Log.i(TAG, "📱 [SIMULATION SMS] To: $phoneNumber | Message: $message")
            showSimulationNotification(context, phoneNumber, message)
        } else {
            // REAL MODE: Send actual SMS
            sendRealSms(context, phoneNumber, message)
        }
    }

    private fun sendRealSms(context: Context, phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "SEND_SMS permission not granted. Cannot send real SMS.")
            // Fallback to simulation notification
            showSimulationNotification(context, phoneNumber, "[PERMISSION DENIED] $message")
            return
        }

        try {
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.i(TAG, "✅ Real SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}", e)
            showSimulationNotification(context, phoneNumber, "[SEND FAILED] $message")
        }
    }

    private fun showSimulationNotification(
        context: Context,
        phoneNumber: String,
        message: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(
            context,
            GuardianTrackApplication.SMS_SIMULATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📱 SMS Simulé → $phoneNumber")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId++, notification)
    }
}
