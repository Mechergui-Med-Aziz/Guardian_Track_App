package com.example.guardiantrackapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardiantrackapp.data.local.db.entity.EmergencyContactEntity
import com.example.guardiantrackapp.data.repository.ContactRepository
import com.example.guardiantrackapp.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val sensitivityThreshold: Float = 15.0f,
    val darkModeEnabled: Boolean = false,
    val emergencyNumber: String = "",
    val smsSimulationMode: Boolean = true,
    val emergencyContacts: List<EmergencyContactEntity> = emptyList(),
    val showAddContactDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Collect preferences
        viewModelScope.launch {
            preferencesRepository.sensitivityThreshold.collect { threshold ->
                _uiState.value = _uiState.value.copy(sensitivityThreshold = threshold)
            }
        }
        viewModelScope.launch {
            preferencesRepository.darkModeEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(darkModeEnabled = enabled)
            }
        }
        viewModelScope.launch {
            preferencesRepository.smsSimulationMode.collect { enabled ->
                _uiState.value = _uiState.value.copy(smsSimulationMode = enabled)
            }
        }
        viewModelScope.launch {
            contactRepository.getAllContacts().collect { contacts ->
                _uiState.value = _uiState.value.copy(emergencyContacts = contacts)
            }
        }

        // Load encrypted emergency number
        viewModelScope.launch(Dispatchers.IO) {
            val number = preferencesRepository.getEmergencyNumber()
            _uiState.value = _uiState.value.copy(emergencyNumber = number)
        }
    }

    fun setSensitivityThreshold(threshold: Float) {
        viewModelScope.launch {
            preferencesRepository.setSensitivityThreshold(threshold)
        }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkModeEnabled(enabled)
        }
    }

    fun setEmergencyNumber(number: String) {
        _uiState.value = _uiState.value.copy(emergencyNumber = number)
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.setEmergencyNumber(number)
        }
    }

    fun setSmsSimulationMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSmsSimulationMode(enabled)
        }
    }

    fun showAddContactDialog() {
        _uiState.value = _uiState.value.copy(showAddContactDialog = true)
    }

    fun hideAddContactDialog() {
        _uiState.value = _uiState.value.copy(showAddContactDialog = false)
    }

    fun addEmergencyContact(name: String, phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.insertContact(
                EmergencyContactEntity(name = name, phoneNumber = phoneNumber)
            )
        }
        hideAddContactDialog()
    }

    fun deleteEmergencyContact(contact: EmergencyContactEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            contactRepository.deleteContact(contact)
        }
    }
}
