package com.example.guardiantrackapp.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.guardiantrackapp.data.local.db.entity.IncidentEntity
import com.example.guardiantrackapp.data.repository.IncidentRepository
import com.example.guardiantrackapp.util.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val incidents: List<IncidentEntity> = emptyList(),
    val isExporting: Boolean = false,
    val exportResult: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    application: Application,
    private val incidentRepository: IncidentRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            incidentRepository.getAllIncidents().collect { incidents ->
                _uiState.value = _uiState.value.copy(incidents = incidents)
            }
        }
    }

    fun deleteIncident(incidentId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            incidentRepository.deleteIncident(incidentId)
        }
    }

    fun exportToCsv() {
        _uiState.value = _uiState.value.copy(isExporting = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val incidents = incidentRepository.getAllIncidentsList()
                val fileName = CsvExporter.exportToCsv(
                    getApplication<Application>(),
                    incidents
                )
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = if (fileName != null) {
                        "✓ Exporté dans Téléchargements/GuardianTrack/$fileName"
                    } else {
                        "Erreur lors de l'export"
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = "Erreur: ${e.message}"
                )
            }
        }
    }

    fun clearExportResult() {
        _uiState.value = _uiState.value.copy(exportResult = null)
    }
}
