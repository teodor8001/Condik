package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.Material
import com.example.workipi.data.model.MaterialInsert
import com.example.workipi.data.model.History
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.User
import com.example.workipi.data.model.Zone
import com.example.workipi.data.model.ZoneInsert
import com.example.workipi.data.model.ZoneHistoryInsert
import com.example.workipi.repository.SkillRepository
import com.example.workipi.repository.MaterialRepository
import com.example.workipi.repository.HistoryRepository
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.UserProjectRepository
import com.example.workipi.repository.UserRepository
import com.example.workipi.repository.ZoneHistoryRepository
import com.example.workipi.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.math.max

data class ProjectPontareEntry(
    val history: History,
    val userName: String,
    val lucrareName: String,
    val lucrareUnit: String,
    val zoneName: String,
)

data class ProjectLeaderboardEntry(
    val userId: Long,
    val userName: String,
    val points: Long,
)

data class ZoneLucrareEntry(
    val zoneId: Long,
    val lucrareId: Long,
    val lucrareName: String,
    val lucrareUnit: String,
    val totalQuantity: Float,
    val completedQuantity: Float,
) {
    val progress: Float
        get() = if (totalQuantity <= 0f) 0f else (completedQuantity / totalQuantity).coerceIn(0f, 1f)
}

data class ZoneSection(
    val zone: Zone,
    val lucrari: List<ZoneLucrareEntry>,
) {
    val isImplicit: Boolean get() = zone.isImplicit
    val progress: Float
        get() = if (lucrari.isEmpty()) 0f
        else (lucrari.sumOf { it.progress.toDouble() } / lucrari.size).toFloat()
}

data class ProjectDetailUiState(
    val project: Project? = null,
    val team: List<User> = emptyList(),
    val zones: List<Zone> = emptyList(),
    val zoneSections: List<ZoneSection> = emptyList(),
    val firmaLucrari: List<Lucrare> = emptyList(),
    val materiale: List<Material> = emptyList(),
    val pontari: List<ProjectPontareEntry> = emptyList(),
    val leaderboard: List<ProjectLeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasOnlyImplicitZone: Boolean get() = zones.size == 1 && zones.first().isImplicit
    val totalSurface: Float get() = zones.filterNot { it.isImplicit }.sumOf { it.surface.toDouble() }.toFloat()
    val completedSurface: Float
        get() = zones.filterNot { it.isImplicit }.sumOf { (it.surfaceCompleted ?: 0f).toDouble() }.toFloat()
    val progress: Float
        get() = if (zoneSections.isEmpty()) 0f
        else (zoneSections.sumOf { it.progress.toDouble() } / zoneSections.size).toFloat()

    val materialeTotalCost: Float get() = materiale.sumOf { it.totalCost.toDouble() }.toFloat()

    val salaryCostTotal: Float
        get() {
            val p = project ?: return 0f
            val startLocal = p.startDate.toLocalDateTime(TimeZone.UTC).date
            val days = max(1, p.endDate.toEpochDays() - startLocal.toEpochDays())
            val perDayPerEmployee = team.sumOf { (it.salary ?: 0.0) / 30.0 }
            return (perDayPerEmployee * days).toFloat()
        }
}

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val userProjectRepository: UserProjectRepository,
    private val userRepository: UserRepository,
    private val zoneRepository: ZoneRepository,
    private val zoneHistoryRepository: ZoneHistoryRepository,
    private val historyRepository: HistoryRepository,
    private val skillRepository: SkillRepository,
    private val materialRepository: MaterialRepository,
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
                val firmaLucrari = skillRepository
                    .getSkillsForCompany(project.companyId)
                    .getOrDefault(emptyList())
                val materiale = materialRepository
                    .getByProject(projectId)
                    .getOrDefault(emptyList())
                val lucrareById = firmaLucrari.associateBy { it.id }
                val allPontari = if (zones.isEmpty()) emptyList()
                else historyRepository.getByZones(zones.map { it.id }).getOrDefault(emptyList())
                val zoneSections = buildZoneSections(zones, lucrareById)
                val (pontari, leaderboard) =
                    buildPontariAndLeaderboard(zones, team, allPontari, lucrareById)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        project = project,
                        team = team,
                        zones = zones,
                        zoneSections = zoneSections,
                        firmaLucrari = firmaLucrari,
                        materiale = materiale,
                        pontari = pontari,
                        leaderboard = leaderboard,
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
                .onSuccess { reload(projectId) }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la creare zona", e)
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "Crearea zonei a esuat.")
                    }
                }
        }
    }

    fun addLucrareToZone(zoneId: Long, lucrareId: Long, quantity: Float) {
        if (quantity <= 0f) {
            _uiState.update { it.copy(errorMessage = "Cantitatea trebuie sa fie pozitiva.") }
            return
        }
        val projectId = _uiState.value.project?.projectId ?: return
        viewModelScope.launch {
            zoneHistoryRepository.add(
                ZoneHistoryInsert(zoneId = zoneId, lucrareId = lucrareId, totalQuantity = quantity)
            )
                .onSuccess { reload(projectId) }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la adaugare lucrare pe zona", e)
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "Adaugarea lucrarii a esuat.")
                    }
                }
        }
    }

    fun deleteZone(zoneId: Long) {
        val projectId = _uiState.value.project?.projectId ?: return
        viewModelScope.launch {
            zoneRepository.deleteZone(zoneId)
                .onSuccess { reload(projectId) }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la stergere zona", e)
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "Stergerea zonei a esuat.")
                    }
                }
        }
    }

    fun acceptOffer(onAccepted: () -> Unit) {
        val projectId = _uiState.value.project?.projectId ?: return
        viewModelScope.launch {
            projectRepository.acceptOffer(projectId)
                .onSuccess { onAccepted() }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la acceptare oferta", e)
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "Acceptarea ofertei a esuat.")
                    }
                }
        }
    }

    fun deleteProject(onDeleted: () -> Unit) {
        val projectId = _uiState.value.project?.projectId ?: return
        Log.d(TAG, "deleteProject: incep stergerea pentru id=$projectId")
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
                .onSuccess {
                    Log.d(TAG, "deleteProject: verific daca proiectul mai exista...")
                    val stillThere = projectRepository.getProjectById(projectId)
                    if (stillThere != null) {
                        val msg = "Stergerea pare sa fi reusit dar proiectul inca exista (posibil RLS sau policy blocheaza)."
                        Log.e(TAG, msg)
                        _uiState.update { it.copy(errorMessage = msg) }
                    } else {
                        Log.d(TAG, "deleteProject: confirmat sters")
                        onDeleted()
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la stergere proiect", e)
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "Stergerea proiectului a esuat.")
                    }
                }
        }
    }

    fun removeLucrareFromZone(zoneId: Long, lucrareId: Long) {
        val projectId = _uiState.value.project?.projectId ?: return
        viewModelScope.launch {
            zoneHistoryRepository.remove(zoneId, lucrareId)
                .onSuccess { reload(projectId) }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la stergere lucrare", e)
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "Stergerea a esuat.")
                    }
                }
        }
    }

    fun addMaterial(name: String, quantity: Float, unit: String, unitCost: Float) {
        val projectId = _uiState.value.project?.projectId ?: return
        if (name.isBlank() || quantity <= 0f || unit.isBlank() || unitCost < 0f) {
            _uiState.update { it.copy(errorMessage = "Completeaza toate campurile cu valori valide.") }
            return
        }
        viewModelScope.launch {
            materialRepository.add(
                MaterialInsert(
                    projectId = projectId,
                    name = name.trim(),
                    quantity = quantity,
                    unit = unit.trim(),
                    unitCost = unitCost,
                )
            )
                .onSuccess { reload(projectId) }
                .onFailure { e ->
                    Log.e(TAG, "Adaugare material esuata", e)
                    _uiState.update { it.copy(errorMessage = e.message ?: "Adaugarea materialului a esuat.") }
                }
        }
    }

    fun updateMaterial(id: Long, name: String, quantity: Float, unit: String, unitCost: Float) {
        val projectId = _uiState.value.project?.projectId ?: return
        if (name.isBlank() || quantity <= 0f || unit.isBlank() || unitCost < 0f) {
            _uiState.update { it.copy(errorMessage = "Completeaza toate campurile cu valori valide.") }
            return
        }
        viewModelScope.launch {
            materialRepository.update(id, name.trim(), quantity, unit.trim(), unitCost)
                .onSuccess { reload(projectId) }
                .onFailure { e ->
                    Log.e(TAG, "Update material esuat", e)
                    _uiState.update { it.copy(errorMessage = e.message ?: "Actualizarea materialului a esuat.") }
                }
        }
    }

    fun removeMaterial(id: Long) {
        val projectId = _uiState.value.project?.projectId ?: return
        viewModelScope.launch {
            materialRepository.remove(id)
                .onSuccess { reload(projectId) }
                .onFailure { e ->
                    Log.e(TAG, "Stergere material esuata", e)
                    _uiState.update { it.copy(errorMessage = e.message ?: "Stergerea materialului a esuat.") }
                }
        }
    }

    fun toggleCheckIn(userId: Long) {
        val state = _uiState.value
        val current = state.team.firstOrNull { it.idUser == userId } ?: return
        val newValue = !current.isCheckedIn
        _uiState.update { s ->
            s.copy(team = s.team.map { if (it.idUser == userId) it.copy(isCheckedIn = newValue) else it })
        }
        viewModelScope.launch {
            userRepository.setCheckedIn(userId, newValue)
                .onFailure { e ->
                    Log.e(TAG, "Toggle check-in esuat", e)
                    _uiState.update { s ->
                        s.copy(team = s.team.map { if (it.idUser == userId) it.copy(isCheckedIn = !newValue) else it })
                    }
                }
        }
    }

    private fun reload(projectId: Long) = load(projectId)

    private suspend fun buildZoneSections(
        zones: List<Zone>,
        lucrareById: Map<Long, Lucrare>,
    ): List<ZoneSection> {
        if (zones.isEmpty()) return emptyList()
        val zoneLucrari = zoneHistoryRepository
            .getByZones(zones.map { it.id })
            .getOrDefault(emptyList())

        return zones.map { zone ->
            val entries = zoneLucrari
                .filter { it.zoneId == zone.id }
                .map { zl ->
                    val lucrare = lucrareById[zl.lucrareId]
                    ZoneLucrareEntry(
                        zoneId = zl.zoneId,
                        lucrareId = zl.lucrareId,
                        lucrareName = lucrare?.name ?: "Lucrare #${zl.lucrareId}",
                        lucrareUnit = lucrare?.unit ?: "",
                        totalQuantity = zl.totalQuantity,
                        completedQuantity = zl.completedQuantity,
                    )
                }
            ZoneSection(zone = zone, lucrari = entries)
        }
    }

    private fun buildPontariAndLeaderboard(
        zones: List<Zone>,
        team: List<User>,
        pontari: List<History>,
        lucrareById: Map<Long, Lucrare>,
    ): Pair<List<ProjectPontareEntry>, List<ProjectLeaderboardEntry>> {
        if (pontari.isEmpty()) return emptyList<ProjectPontareEntry>() to emptyList()
        val zoneById = zones.associateBy { it.id }
        val userById = team.associateBy { it.idUser }

        val entries = pontari.map { p ->
            ProjectPontareEntry(
                history = p,
                userName = userById[p.userId]?.fullName ?: "Utilizator #${p.userId}",
                lucrareName = lucrareById[p.idLucrare]?.name ?: "—",
                lucrareUnit = lucrareById[p.idLucrare]?.unit ?: "",
                zoneName = zoneById[p.idZona]?.name ?: "Zona",
            )
        }

        val leaderboard = pontari
            .groupBy { it.userId }
            .map { (userId, userPontari) ->
                val pts = userPontari.sumOf { p ->
                    val lucrarePoints = lucrareById[p.idLucrare]?.points ?: 0L
                    (lucrarePoints * p.quantity).toLong()
                }
                ProjectLeaderboardEntry(
                    userId = userId,
                    userName = userById[userId]?.fullName ?: "Utilizator #$userId",
                    points = pts,
                )
            }
            .sortedByDescending { it.points }

        return entries to leaderboard
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
