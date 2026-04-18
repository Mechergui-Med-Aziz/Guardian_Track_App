package com.example.guardiantrackapp.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.guardiantrackapp.data.local.db.entity.IncidentEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports incident history to a CSV file.
 * Uses Downloads directory via MediaStore (API 29+) for easy user access,
 * or falls back to app-internal storage + direct file access on older APIs.
 */
object CsvExporter {

    private const val TAG = "CsvExporter"

    /**
     * Export incidents to CSV.
     * On API 29+, writes to Downloads/GuardianTrack/ via MediaStore.
     * On older APIs, writes to external Documents/GuardianTrack/.
     * Also saves a copy to app-internal files directory as a fallback.
     *
     * @return The display name of the exported file, or null on failure.
     */
    fun exportToCsv(context: Context, incidents: List<IncidentEntity>): String? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "GuardianTrack_Export_$fileTimestamp.csv"

        val csvContent = buildString {
            // Header
            appendLine("Date,Heure,Type,Latitude,Longitude,Statut Synchronisation")
            // Data rows
            incidents.forEach { incident ->
                val date = Date(incident.timestamp)
                val dateStr = dateFormat.format(date)
                val timeStr = timeFormat.format(date)
                val syncStatus = if (incident.isSynced) "Synchronisé" else "En attente"
                appendLine("$dateStr,$timeStr,${incident.type},${incident.latitude},${incident.longitude},$syncStatus")
            }
        }

        return try {
            // Always save a copy to app-internal storage (guaranteed to work)
            saveToInternalStorage(context, fileName, csvContent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore to save to Downloads/ (visible in file manager)
                exportToDownloads(context, fileName, csvContent)
            } else {
                // Direct file access for older APIs
                exportDirectly(fileName, csvContent)
            }
            Log.i(TAG, "CSV exported successfully: $fileName")
            fileName
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CSV: ${e.message}", e)
            null
        }
    }

    /**
     * Save to app-internal files directory as a guaranteed fallback.
     * Located at: /data/data/com.example.guardiantrackapp/files/exports/
     */
    private fun saveToInternalStorage(context: Context, fileName: String, content: String) {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, fileName)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(content.toByteArray())
        }
        Log.i(TAG, "CSV saved to internal storage: ${file.absolutePath}")
    }

    /**
     * Export via MediaStore to the Downloads directory (API 29+).
     * This makes the file visible in the system file manager under Downloads/GuardianTrack/.
     */
    private fun exportToDownloads(context: Context, fileName: String, content: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GuardianTrack")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to create MediaStore entry in Downloads")

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        } ?: throw Exception("Failed to open output stream")

        Log.i(TAG, "CSV exported to Downloads/GuardianTrack/$fileName")
    }

    @Suppress("DEPRECATION")
    private fun exportDirectly(fileName: String, content: String) {
        val documentsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "GuardianTrack"
        )
        if (!documentsDir.exists()) documentsDir.mkdirs()

        val file = File(documentsDir, fileName)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(content.toByteArray())
        }
        Log.i(TAG, "CSV exported directly to: ${file.absolutePath}")
    }
}
