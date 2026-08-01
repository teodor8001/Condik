package com.example.workipi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.model.History
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.SiteAttendance
import com.example.workipi.data.model.SiteAttendanceInsert
import com.example.workipi.data.model.SiteCorrection
import com.example.workipi.data.model.SiteCorrectionInsert
import com.example.workipi.data.model.SiteDailyClosure
import com.example.workipi.data.model.SiteDailyClosureInsert
import com.example.workipi.data.model.SiteJournalEntry
import com.example.workipi.data.model.SiteJournalEntryInsert
import com.example.workipi.data.model.User
import com.example.workipi.data.model.Zone
import com.example.workipi.data.model.ZoneHistory
import com.example.workipi.repository.HistoryRepository
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.SiteOperationsRepository
import com.example.workipi.repository.SkillRepository
import com.example.workipi.repository.UserProjectRepository
import com.example.workipi.repository.UserRepository
import com.example.workipi.repository.ZoneHistoryRepository
import com.example.workipi.repository.ZoneRepository
import com.example.workipi.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class SiteAttendanceRow(val user: User, val attendance: SiteAttendance?)

data class SiteWorkRow(
    val zoneName: String,
    val workName: String,
    val unit: String,
    val completed: Float,
    val total: Float,
    val today: Float,
) {
    val progress: Float get() = if (total <= 0f) 0f else (completed / total).coerceIn(0f, 1f)
}

data class SiteTimeEntryRow(
    val id: Long,
    val date: LocalDate?,
    val employee: String,
    val work: String,
    val zone: String,
    val hours: Double,
    val quantity: Float,
    val unit: String,
    val quality: Float?,
)

data class SantierUiState(
    val projects: List<Project> = emptyList(),
    val selectedProjectId: Long? = null,
    val attendance: List<SiteAttendanceRow> = emptyList(),
    val workItems: List<SiteWorkRow> = emptyList(),
    val timeEntries: List<SiteTimeEntryRow> = emptyList(),
    val corrections: List<SiteCorrection> = emptyList(),
    val journal: List<SiteJournalEntry> = emptyList(),
    val photoUrls: Map<String, String> = emptyMap(),
    val closure: SiteDailyClosure? = null,
    val usersById: Map<Long, User> = emptyMap(),
    val zones: List<Zone> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val selectedProject: Project? get() = projects.firstOrNull { it.projectId == selectedProjectId }
    val presentCount: Int get() = attendance.count { it.attendance?.status == "prezent" }
    val todayQuantity: Float get() = workItems.sumOf { it.today.toDouble() }.toFloat()
    val openCorrections: Int get() = corrections.count { it.status == "deschisa" || it.status == "in_lucru" }
    val incidentsToday: Int get() = journal.count { it.type == "incident" && it.date == today() }
}

@HiltViewModel
class SantierViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val userProjectRepository: UserProjectRepository,
    private val userRepository: UserRepository,
    private val zoneRepository: ZoneRepository,
    private val zoneHistoryRepository: ZoneHistoryRepository,
    private val historyRepository: HistoryRepository,
    private val skillRepository: SkillRepository,
    private val siteRepository: SiteOperationsRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SantierUiState())
    val uiState: StateFlow<SantierUiState> = _uiState.asStateFlow()

    init { loadProjects() }

    fun loadProjects() {
        val companyId = sessionStore.state.value.user?.companyId ?: return showError("Firma nu este disponibilă.")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { projectRepository.getProjectsByCompanyId(companyId) }
                .onSuccess { projects ->
                    val selected = _uiState.value.selectedProjectId
                        ?.takeIf { id -> projects.any { it.projectId == id } }
                        ?: projects.firstOrNull()?.projectId
                    _uiState.update { it.copy(projects = projects, selectedProjectId = selected) }
                    if (selected != null) loadProject(selected) else _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { showError(it.message ?: "Proiectele nu au putut fi încărcate.") }
        }
    }

    fun selectProject(projectId: Long) {
        _uiState.update { it.copy(selectedProjectId = projectId) }
        loadProject(projectId)
    }

    fun refresh() = _uiState.value.selectedProjectId?.let(::loadProject) ?: loadProjects()

    private fun loadProject(projectId: Long) {
        val companyId = sessionStore.state.value.user?.companyId ?: return showError("Firma nu este disponibilă.")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                coroutineScope {
                    val zonesDeferred = async { zoneRepository.getZonesForProject(projectId).getOrThrow() }
                    val assignmentsDeferred = async { userProjectRepository.getAssignmentsForProject(projectId).getOrThrow() }
                    val usersDeferred = async { userRepository.getEmployeesByCompanyId(companyId).getOrThrow() }
                    val skillsDeferred = async { skillRepository.getSkillsForCompany(companyId).getOrThrow() }
                    val attendanceDeferred = async { siteRepository.getAttendance(projectId, today()) }
                    val correctionsDeferred = async { siteRepository.getCorrections(projectId) }
                    val journalDeferred = async { siteRepository.getJournal(projectId) }
                    val closureDeferred = async { siteRepository.getClosure(projectId, today()) }

                    val zones = zonesDeferred.await()
                    val zoneWork = zoneHistoryRepository.getByZones(zones.map { it.id }).getOrThrow()
                    val histories = historyRepository.getByZones(zones.map { it.id }).getOrThrow()
                    val assignments = assignmentsDeferred.await()
                    val users = usersDeferred.await()
                    val skills = skillsDeferred.await()
                    val journal = journalDeferred.await()
                    val photoUrls = journal.mapNotNull { it.photoPath }.associateWith { path ->
                        siteRepository.createPhotoUrl(path)
                    }
                    LoadedSiteData(
                        zones = zones,
                        zoneWork = zoneWork,
                        histories = histories,
                        users = users.filter { user -> assignments.any { it.userId == user.idUser } },
                        skills = skills,
                        attendance = attendanceDeferred.await(),
                        corrections = correctionsDeferred.await(),
                        journal = journal,
                        photoUrls = photoUrls,
                        closure = closureDeferred.await(),
                    )
                }
            }.onSuccess { data ->
                val usersById = data.users.associateBy { it.idUser }
                val zonesById = data.zones.associateBy { it.id }
                val skillsById = data.skills.associateBy { it.id }
                val attendanceByUser = data.attendance.associateBy { it.userId }
                _uiState.update { current ->
                    current.copy(
                        attendance = data.users.map { SiteAttendanceRow(it, attendanceByUser[it.idUser]) },
                        workItems = buildWorkItems(data.zoneWork, data.histories, zonesById, skillsById),
                        timeEntries = buildTimeEntries(data.histories, usersById, zonesById, skillsById),
                        corrections = data.corrections,
                        journal = data.journal,
                        photoUrls = data.photoUrls,
                        closure = data.closure,
                        usersById = usersById,
                        zones = data.zones,
                        isLoading = false,
                    )
                }
            }.onFailure { showError(it.message ?: "Datele șantierului nu au putut fi încărcate.") }
        }
    }

    fun setAttendance(userId: Long, status: String) = savingAction("Prezența a fost actualizată.") {
        ensureOpenDay()
        val projectId = requireProject()
        val recorder = requireUser()
        siteRepository.saveAttendance(
            SiteAttendanceInsert(
                projectId = projectId,
                userId = userId,
                date = today(),
                status = status,
                recordedBy = recorder,
            )
        )
    }

    fun createCorrection(title: String, description: String, severity: String, assignedTo: Long?) =
        savingAction("Corectura a fost creată.") {
            ensureOpenDay()
            require(title.isNotBlank() && description.isNotBlank()) { "Completează titlul și descrierea." }
            siteRepository.createCorrection(
                SiteCorrectionInsert(
                    projectId = requireProject(),
                    title = title.trim(),
                    description = description.trim(),
                    severity = severity,
                    assignedTo = assignedTo,
                    createdBy = requireUser(),
                )
            )
        }

    fun updateCorrectionStatus(id: Long, status: String) = savingAction("Corectura a fost actualizată.") {
        siteRepository.updateCorrectionStatus(id, status)
    }

    fun createJournalEntry(
        type: String,
        title: String,
        description: String,
        severity: String?,
        photoBytes: ByteArray? = null,
        mimeType: String? = null,
    ) = savingAction("Înregistrarea a fost adăugată în jurnal.") {
        ensureOpenDay()
        require(title.isNotBlank() && description.isNotBlank()) { "Completează titlul și descrierea." }
        val projectId = requireProject()
        val photoPath = photoBytes?.let {
            val extension = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            siteRepository.uploadPhoto(projectId, it, extension)
        }
        val resolvedType = if (photoPath != null) "fotografie" else type
        siteRepository.createJournalEntry(
            SiteJournalEntryInsert(
                projectId = projectId,
                date = today(),
                type = resolvedType,
                title = title.trim(),
                description = description.trim(),
                severity = severity.takeIf { resolvedType == "incident" },
                photoPath = photoPath,
                createdBy = requireUser(),
            )
        )
    }

    fun closeDay(summary: String, blockers: String, nextDayPlan: String) = savingAction("Ziua a fost închisă.") {
        require(_uiState.value.closure == null) { "Ziua este deja închisă." }
        require(summary.isNotBlank()) { "Rezumatul zilei este obligatoriu." }
        siteRepository.closeDay(
            SiteDailyClosureInsert(
                projectId = requireProject(),
                date = today(),
                summary = summary.trim(),
                blockers = blockers.trim().ifBlank { null },
                nextDayPlan = nextDayPlan.trim().ifBlank { null },
                closedBy = requireUser(),
            )
        )
    }

    fun consumeMessage() = _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    private fun savingAction(success: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            runCatching { action() }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, successMessage = success) }
                    _uiState.value.selectedProjectId?.let(::loadProject)
                }
                .onFailure { showError(it.message ?: "Operația nu a putut fi salvată.") }
        }
    }

    private fun requireProject(): Long = _uiState.value.selectedProjectId ?: error("Selectează un proiect.")
    private fun requireUser(): Long = sessionStore.state.value.user?.id ?: error("Sesiunea a expirat.")
    private fun ensureOpenDay() = require(_uiState.value.closure == null) { "Ziua este închisă. Datele nu mai pot fi modificate." }
    private fun showError(message: String) = _uiState.update { it.copy(isLoading = false, isSaving = false, errorMessage = message) }

    private data class LoadedSiteData(
        val zones: List<Zone>,
        val zoneWork: List<ZoneHistory>,
        val histories: List<History>,
        val users: List<User>,
        val skills: List<Lucrare>,
        val attendance: List<SiteAttendance>,
        val corrections: List<SiteCorrection>,
        val journal: List<SiteJournalEntry>,
        val photoUrls: Map<String, String>,
        val closure: SiteDailyClosure?,
    )

    companion object {
        private fun buildWorkItems(
            zoneWork: List<ZoneHistory>,
            histories: List<History>,
            zones: Map<Long, Zone>,
            skills: Map<Long, Lucrare>,
        ): List<SiteWorkRow> = zoneWork.mapNotNull { item ->
            val zone = zones[item.zoneId] ?: return@mapNotNull null
            val skill = skills[item.lucrareId] ?: return@mapNotNull null
            SiteWorkRow(
                zoneName = zone.name ?: "Zonă",
                workName = skill.name,
                unit = skill.unit,
                completed = item.completedQuantity,
                total = item.totalQuantity,
                today = histories.filter {
                    it.idZona == item.zoneId && it.idLucrare == item.lucrareId && it.workDate == today()
                }.sumOf { it.quantity.toDouble() }.toFloat(),
            )
        }.filter { it.completed < it.total || it.today > 0f }

        private fun buildTimeEntries(
            histories: List<History>,
            users: Map<Long, User>,
            zones: Map<Long, Zone>,
            skills: Map<Long, Lucrare>,
        ): List<SiteTimeEntryRow> = histories.map { history ->
            SiteTimeEntryRow(
                id = history.id,
                date = history.workDate,
                employee = users[history.userId]?.fullName ?: "Utilizator #${history.userId}",
                work = skills[history.idLucrare]?.name ?: "Lucrare",
                zone = zones[history.idZona]?.name ?: "Zonă",
                hours = history.hours ?: 0.0,
                quantity = history.quantity,
                unit = skills[history.idLucrare]?.unit ?: "",
                quality = history.quality,
            )
        }.sortedByDescending { it.date }
    }
}

private fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
