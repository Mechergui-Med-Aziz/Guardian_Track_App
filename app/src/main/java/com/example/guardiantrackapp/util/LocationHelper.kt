package com.example.guardiantrackapp.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Wrapper around FusedLocationProviderClient for getting the current GPS location.
 * Returns (0.0, 0.0) sentinel values if permission is denied.
 */
object LocationHelper {

    private const val TAG = "LocationHelper"

    data class Coordinates(val latitude: Double, val longitude: Double)

    /**
     * Get current location coordinates.
     * Returns (0.0, 0.0) if permission is not granted.
     */
    suspend fun getCurrentLocation(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient
    ): Coordinates {
        // Check permission
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted. Using sentinel values (0.0, 0.0)")
            return Coordinates(0.0, 0.0)
        }

        return try {
            val location = getLastKnownLocation(fusedLocationClient)
            if (location != null) {
                Coordinates(location.latitude, location.longitude)
            } else {
                Log.w(TAG, "Location is null. Using sentinel values.")
                Coordinates(0.0, 0.0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location: ${e.message}", e)
            Coordinates(0.0, 0.0)
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getLastKnownLocation(
        fusedLocationClient: FusedLocationProviderClient
    ): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            continuation.resume(location)
        }.addOnFailureListener {
            continuation.resume(null)
        }

        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }
    }
}
