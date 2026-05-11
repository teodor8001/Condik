package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.PontareInsert
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.User
import com.example.workipi.data.model.Zone
import com.example.workipi.repository.LucrareRepository
import com.example.workipi.repository.PontareRepository
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.UserRepository
import com.example.workipi.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

private const val UNIT_MP = "mp"

data class StandalonePontareUiState(
    val projects: List<Project> = emptyList(),
    val employees: List<User> = emptyList(),
    val skills: List<Lucrare> = emptyList(),
    val zonesForProject: List<Zone> = emptyList(),
    val selectedProjectId: Long? = null,
    val selectedUserId: Long? = null,
    val selectedZoneId: Long? = null,
    val selectedSkillId: Long? = null,
    val employeeQuery: String = "",
    val quantity: String = "",
    val hours: String = "",
    val workDateMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
) {
    val filteredEmployees: List<User>
        get() = if (employeeQuery.isBlank()) employees
        else employees.filter { it.fullName.contains(employeeQuery, ignoreCase = true) }
}

@HiltViewModel
class StandalonePontareViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val userRepository: UserRepository,
    private val lucrareRepository: LucrareRepository,
    private val zoneRepository: ZoneRepository,
    private val pontareRepository: PontareRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StandalonePontareUiState())
    val uiState: StateFlow<StandalonePontareUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val companyId = MockSession.currentUser?.idCompany
        if (companyId == null) {
            _uiState.update { it.copy(errorMessage = "Nu am putut identifica firma.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val projects = projectRepository.getProjectsByCompanyId(companyId)
                val employees = userRepository.getEmployeesByCompanyId(companyId).getOrDefault(emptyList())
                val skills = lucrareRepository.getSkillsForCompany(companyId).getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        projects = projects,
                        employees = employees,
                        skills = skills,
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Eroare la incarcarea datelor", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Nu am putut incarca datele.",
                    )
                }
            }
        }
    }

    fun selectProject(projectId: Long) {
        _uiState.update { it.copy(
            selectedProjectId = projectId,
            selectedZoneId = null,
            zonesForProject = emptyList(),
            errorMessage = null,
        ) }
        viewModelScope.launch {
            val zones = zoneRepository.getZonesForProject(projectId).getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    zonesForProject = zones,
                    selectedZoneId = zones.firstOrNull()?.id,
                )
            }
        }
    }

    fun selectEmployee(userId: Long) {
        val name = _uiState.value.employees.firstOrNull { it.idUser == userId }?.fullName ?: ""
        _uiState.update {
            it.copy(selectedUserId = userId, employeeQuery = name, errorMessage = null)
        }
    }

    fun selectZone(zoneId: Long) =
        _uiState.update { it.copy(selectedZoneId = zoneId, errorMessage = null) }

    fun selectSkill(skillId: Long) =
        _uiState.update { it.copy(selectedSkillId = skillId, errorMessage = null) }

    fun onEmployeeQueryChange(value: String) {
        _uiState.update {
            // Daca user-ul a sters/modifficat textul, deselectam selectia veche
            val keepSelection = it.employees
                .firstOrNull { u -> u.idUser == it.selectedUserId }
                ?.fullName == value
            it.copy(
                employeeQuery = value,
                selectedUserId = if (keepSelection) it.selectedUserId else null,
                errorMessage = null,
            )
        }
    }

    fun onQuantityChange(value: String) =
        _uiState.update { it.copy(quantity = value.filter { c -> c.isDigit() || c == '.' }, errorMessage = null) }

    fun onHoursChange(value: String) =
        _uiState.update { it.copy(hours = value.filter { c -> c.isDigit() || c == '.' }, errorMessage = null) }

    fun onWorkDateChange(millis: Long) =
        _uiState.update { it.copy(workDateMillis = millis, errorMessage = null) }

    fun submit() {
        val state = _uiState.value
        val skill = state.skills.firstOrNull { it.id == state.selectedSkillId }
        val quantity = state.quantity.toFloatOrNull()
        val hours = state.hours.toDoubleOrNull()

        val error = when {
            state.selectedProjectId == null -> "Alege un proiect."
            state.selectedUserId == null -> "Alege un angajat."
            state.selectedZoneId == null -> "Alege o zona."
            skill == null -> "Alege tipul de lucrare."
            quantity == null || quantity <= 0f -> "Cantitatea trebuie sa fie un numar pozitiv."
            hours == null || hours <= 0.0 -> "Numarul de ore trebuie sa fie un numar pozitiv."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val workDate = Instant.fromEpochMilliseconds(state.workDateMillis)
                    .toLocalDateTime(TimeZone.UTC).date

                pontareRepository.createPontare(
                    PontareInsert(
                        userId = state.selectedUserId!!,
                        idLucrare = skill!!.id,
                        idZona = state.selectedZoneId!!,
                        hours = hours!!,
                        quantity = quantity!!,
                        quality = 1.0f,
                        workDate = workDate,
                    )
                ).getOrThrow()

                val earnedPoints = skill.points.toDouble() * quantity
                userRepository.addPoints(state.selectedUserId, earnedPoints).getOrThrow()

                if (skill.unit.equals(UNIT_MP, ignoreCase = true)) {
                    zoneRepository.addCompletedSurface(state.selectedZoneId, quantity).getOrThrow()
                }

                _uiState.update {
                    StandalonePontareUiState(
                        projects = it.projects,
                        employees = it.employees,
                        skills = it.skills,
                        saved = true,
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Eroare la salvarea pontarii", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Salvarea a esuat. Incearca din nou.",
                    )
                }
            }
        }
    }

    fun consumeSaved() {
        _uiState.update { it.copy(saved = false) }
    }

    companion object {
        private const val TAG = "StandalonePontareVM"
    }
}
