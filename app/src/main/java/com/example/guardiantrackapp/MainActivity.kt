package com.example.guardiantrackapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.example.guardiantrackapp.data.repository.PreferencesRepository
import com.example.guardiantrackapp.service.SurveillanceService
import com.example.guardiantrackapp.ui.navigation.GuardianNavigation
import com.example.guardiantrackapp.ui.theme.GuardianTrackAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            // Show explanation for denied permissions
            val deniedPermissions = permissions.filter { !it.value }.keys
            val messages = deniedPermissions.mapNotNull { perm ->
                when (perm) {
                    Manifest.permission.ACCESS_FINE_LOCATION ->
                        "📍 Localisation: nécessaire pour enregistrer la position des incidents"
                    Manifest.permission.SEND_SMS ->
                        "📱 SMS: nécessaire pour envoyer des alertes d'urgence"
                    Manifest.permission.POST_NOTIFICATIONS ->
                        "🔔 Notifications: nécessaire pour les alertes de surveillance"
                    else -> null
                }
            }
            if (messages.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "Permissions refusées. Certaines fonctionnalités seront limitées.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request permissions
        requestPermissionsIfNeeded()

        // Start the surveillance service
        startSurveillanceService()

        setContent {
            // Observe dark mode preference
            val isDarkMode by preferencesRepository.darkModeEnabled
                .collectAsState(initial = false)

            GuardianTrackAppTheme(darkTheme = isDarkMode) {
                GuardianNavigation()
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun startSurveillanceService() {
        SurveillanceService.startService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            // Only stop service if the activity is truly finishing, not on config changes
            // SurveillanceService.stopService(this) // Keep running in background
        }
    }
}