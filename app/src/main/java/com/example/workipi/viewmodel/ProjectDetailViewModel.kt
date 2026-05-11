package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.Pontare
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.User
import com.example.workipi.data.model.Zone
import com.example.workipi.data.model.ZoneInsert
import com.example.workipi.repository.LucrareRepository
import com.example.workipi.repository.PontareRepository
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.UserProjectRepository
import com.example.workipi.repository.UserRepository
import com.example.workipi.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectPontareEntry(
    val pontare: Pontare,
    val userName: String,
    val lucrareName: String,
    val lucrareUnit: String,
    val zoneName: String,
)

data class ProjectDetailUiState(
    val project: Project? = null,
    val team: List<User> = emptyList(),
    val zones: List<Zone> = emptyList(),
    val pontari: List<ProjectPontareEntry> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val totalSurface: Float get() = zones.sumOf { it.surface.toDouble() }.toFloat()
    val completedSurface: Float get() = zones.sumOf { (it.surfaceCompleted ?: 0f).toDouble() }.toFloat()
    val progress: Float get() = if (totalSurface <= 0f) 0f else (completedSurface / totalSurface).coerceIn(0f, 1f)
}

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val userProjectRepository: UserProjectRepository,
    private val userRepository: UserRepository,
    private val zoneRepository: ZoneRepository,
    private val pontareRepository: PontareRepository,
    private val lucrareRepository: LucrareRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    fun load(projectId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val project = projectRepository.getProjectById(projectId)
                if (project == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Proiectul nu a fost gasit.")
                    }
                    return@launch
                }

                val team = loadTeam(projectId, project.companyId)
                val zones = zoneRepository.getZonesForProject(projectId).getOrDefault(emptyList())
                val pontari = loadPontari(zones, team, project.companyId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        project = project,
                        team = team,
                        zones = zones,
                        pontari = pontari,
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Eroare la incarcarea proiectului", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Nu am putut incarca proiectul.",
                    )
                }
            }
        }
    }

    fun addZone(projectId: Long, name: String, surface: Float) {
        if (name.isBlank() || surface <= 0f) {
            _uiState.update { it.copy(errorMessage = "Numele si suprafata sunt obligatorii.") }
            return
        }
        viewModelScope.launch {
            zoneRepository.createZone(
                ZoneInsert(projectId = projectId, name = name.trim(), surface = surface)
            )
                .onSuccess { zone ->
                    _uiState.update { it.copy(zones = it.zones + zone, errorMessage = null) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la creare zona", e)
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "Crearea zonei a esuat.")
                    }
                }
        }
    }

    fun toggleCheckIn(userId: Long) {
        val state = _uiState.value
        val current = state.team.firstOrNull { it.idUser == userId } ?: return
        val newValue = !current.isCheckedIn
        // Optimist: actualizez state-ul imediat
        _uiState.update { s ->
            s.copy(team = s.team.map { if (it.idUser == userId) it.copy(isCheckedIn = newValue) else it })
        }
        viewModelScope.launch {
            userRepository.setCheckedIn(userId, newValue)
                .onFailure { e ->
                    Log.e(TAG, "Toggle check-in esuat", e)
                    // Rollback
                    _uiState.update { s ->
                        s.copy(team = s.team.map { if (it.idUser == userId) it.copy(isCheckedIn = !newValue) else it })
                    }
                }
        }
    }

    private suspend fun loadPontari(
        zones: List<Zone>,
        team: List<User>,
        companyId: Long,
    ): List<ProjectPontareEntry> {
        if (zones.isEmpty()) return emptyList()
        val pontari = pontareRepository.getByZones(zones.map { it.id }).getOrDefault(emptyList())
        if (pontari.isEmpty()) return emptyList()

        val lucrareById: Map<Long, Lucrare> = lucrareRepository
            .getSkillsForCompany(companyId)
            .getOrDefault(emptyList())
            .associateBy { it.id }
        val zoneById = zones.associateBy { it.id }
        // Echipa nu acopera intotdeauna toti userii — userii pontati pot fi sterși din proiect
        val userById = team.associateBy { it.idUser }

        return pontari.map { p ->
            ProjectPontareEntry(
                pontare = p,
                userName = userById[p.userId]?.fullName ?: "Utilizator #${p.userId}",
                lucrareName = lucrareById[p.idLucrare]?.name ?: "—",
                lucrareUnit = lucrareById[p.idLucrare]?.unit ?: "",
                zoneName = zoneById[p.idZona]?.name ?: "Zona",
            )
        }
    }

    private suspend fun loadTeam(projectId: Long, companyId: Long): List<User> {
        val assignments = userProjectRepository
            .getAssignmentsForProject(projectId)
            .getOrDefault(emptyList())
        if (assignments.isEmpty()) return emptyList()

        val assignedIds = assignments.map { it.userId }.toSet()
        val employees = userRepository
            .getEmployeesByCompanyId(companyId)
            .getOrDefault(emptyList())
        return employees.filter { it.idUser in assignedIds }
    }

    companion object {
        private const val TAG = "ProjectDetailVM"
    }
}
