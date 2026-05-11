package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.ProjectInsert
import com.example.workipi.data.model.ZoneInsert
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class ZoneDraft(
    val key: Long,
    val name: String = "",
    val surface: String = "",
)

data class AddProjectUiState(
    val title: String = "",
    val address: String = "",
    val budget: String = "",
    val endDateMillis: Long? = null,
    val zones: List<ZoneDraft> = listOf(ZoneDraft(key = 0)),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdProject: Project? = null,
)

@HiltViewModel
class AddProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val zoneRepository: ZoneRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProjectUiState())
    val uiState: StateFlow<AddProjectUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) =
        _uiState.update { it.copy(title = value, errorMessage = null) }

    fun onAddressChange(value: String) =
        _uiState.update { it.copy(address = value, errorMessage = null) }

    fun onBudgetChange(value: String) =
        _uiState.update { it.copy(budget = value.filter { c -> c.isDigit() || c == '.' }, errorMessage = null) }

    fun onEndDateChange(millis: Long?) =
        _uiState.update { it.copy(endDateMillis = millis, errorMessage = null) }

    fun onZoneNameChange(key: Long, value: String) =
        _uiState.update { state ->
            state.copy(
                zones = state.zones.map { if (it.key == key) it.copy(name = value) else it },
                errorMessage = null,
            )
        }

    fun onZoneSurfaceChange(key: Long, value: String) =
        _uiState.update { state ->
            state.copy(
                zones = state.zones.map {
                    if (it.key == key) it.copy(surface = value.filter { c -> c.isDigit() || c == '.' })
                    else it
                },
                errorMessage = null,
            )
        }

    fun addZone() = _uiState.update { state ->
        val newKey = (state.zones.maxOfOrNull { it.key } ?: -1L) + 1L
        state.copy(zones = state.zones + ZoneDraft(key = newKey))
    }

    fun removeZone(key: Long) = _uiState.update { state ->
        if (state.zones.size <= 1) state
        else state.copy(zones = state.zones.filter { it.key != key })
    }

    fun submit() {
        val state = _uiState.value
        val companyId = MockSession.currentUser?.idCompany
        val budget = state.budget.toFloatOrNull()

        val zonesParsed = state.zones.map { it to it.surface.toFloatOrNull() }
        val zoneError = zonesParsed.firstOrNull { (z, s) ->
            z.name.isBlank() || s == null || s <= 0f
        }

        val error = when {
            companyId == null -> "Nu am putut identifica firma. Reautentifica-te."
            state.title.isBlank() -> "Introdu denumirea proiectului."
            state.address.isBlank() -> "Introdu adresa."
            budget == null || budget <= 0f -> "Bugetul trebuie sa fie un numar pozitiv."
            state.endDateMillis == null -> "Alege termenul de finalizare."
            state.zones.isEmpty() -> "Adauga cel putin o zona."
            zoneError != null -> "Fiecare zona trebuie sa aiba nume si suprafata pozitiva."
            else -> null
        }

        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            projectRepository.createProject(
                ProjectInsert(
                    title = state.title.trim(),
                    adress = state.address.trim(),
                    budget = budget!!,
                    totalSalaryPerMonth = 0f,
                    endDate = Instant.fromEpochMilliseconds(state.endDateMillis!!)
                        .toLocalDateTime(TimeZone.UTC).date,
                    companyId = companyId!!,
                )
            )
                .onSuccess { project ->
                    Log.d(TAG, "Proiect creat: id=${project.projectId}")
                    val zoneInserts = state.zones.map { draft ->
                        ZoneInsert(
                            projectId = project.projectId,
                            name = draft.name.trim(),
                            surface = draft.surface.toFloat(),
                        )
                    }
                    zoneRepository.createZones(zoneInserts)
                        .onFailure { e -> Log.e(TAG, "Zonele nu s-au creat (continuam)", e) }

                    _uiState.update {
                        it.copy(isLoading = false, createdProject = project)
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Crearea proiectului a esuat", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Crearea proiectului a esuat. Incearca din nou.",
                        )
                    }
                }
        }
    }

    fun consumeCreatedProject() {
        _uiState.update { it.copy(createdProject = null) }
    }

    companion object {
        private const val TAG = "AddProjectVM"
    }
}
