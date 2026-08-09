package com.example.workipi.viewmodel

import android.provider.MediaStore.UNKNOWN_STRING
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.session.SessionStore
import com.example.workipi.data.model.History
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.LucrareInsert
import com.example.workipi.data.model.Material
import com.example.workipi.data.model.MaterialInsert
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.ProjectStatus
import com.example.workipi.data.model.User
import com.example.workipi.data.model.Unealta
import com.example.workipi.data.model.Zone
import com.example.workipi.data.model.ZoneHistory
import com.example.workipi.data.model.ZoneHistoryInsert
import com.example.workipi.data.model.ZoneInsert
import com.example.workipi.repository.HistoryRepository
import com.example.workipi.repository.MaterialRepository
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.SkillRepository
import com.example.workipi.repository.SiteOperationsRepository
import com.example.workipi.repository.UserProjectRepository
import com.example.workipi.repository.UserRepository
import com.example.workipi.repository.UneltaRepository
import com.example.workipi.repository.ZoneHistoryRepository
import com.example.workipi.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

const val UNKNOWNS = ""
data class ProjectDetailScreenUi(
    val name: String = "",
    val address: String = "",
    val status: ProjectStatus = ProjectStatus.ACTIV,
    val progressPercent: Int = 0,
    val endDate: String = "",
    val estimatedEndDate: String = "",
    val finishedQuantity: Double = 0.0,
    val totalQuantity: Double = 0.0,
    val projectPace: Double = 0.0,
    val companyCompletedProjectsPace: Double = 0.0,
    val presentOnSiteCount: Int = 0,
    val materialCosts: Double = 0.0,
    val materials: List<Material> = emptyList(),
    val tools: List<Unealta> = emptyList(),
    val toolsUnavailable: Boolean = false,
    val pontariCount: Int = 0,
    val risks: Long = 0,
    val possibleGains: Double = 0.0,
    val budget: Double = 0.0,
    val projectCosts: Double = 0.0,
    val graphPoints: List<AverageWorkGraphic> = emptyList(),
    val graphSeries: List<ChartSeries> = emptyList(),
    val mixLucrari: List<MixLucrareItem> = emptyList(),
    val lucrariEntries: List<LucrareEntryItem> = emptyList(),
    val team: List<TeamMemberItem> = emptyList(),
    val teamSalaryTotal: Double = 0.0,
    val zoneItems: List<ZoneItem> = emptyList(),
    val zonePickers: List<ZonePickItem> = emptyList(),
    val availableSkills: List<Lucrare> = emptyList(),
    val pontari: List<PontareRowItem> = emptyList(),
    val error: String? = null
)

data class AverageWorkGraphic(
    val date: LocalDate,
    val quantity: Double,
)

/** O serie pentru graficul cu linii pe timp (ex. o lucrare, cu punctele ei zilnice). */
data class ChartSeries(
    val name: String,
    val points: List<AverageWorkGraphic>,
)

/** O lucrare a proiectului + suprafata totala alocata ei (din zone_lucrari). */
data class MixLucrareItem(
    val name: String,
    val totalQuantity: Double,
    val completedQuantity: Double,
    val pacePerDay: Double,
    val unit: String,
)

/** Un membru al echipei alocate: nume, salariu si ritmul lui mediu (din istoric_pontari). */
data class TeamMemberItem(
    val userId: Long,
    val name: String,
    val role: String,
    val salary: Double,
    val mpPerDay: Double,
    val isPresent: Boolean,
)

/** O zona a proiectului + cat e finalizat din ea. */
data class ZoneItem(
    val id: Long,
    val name: String,
    val completedQuantity: Double,
    val totalQuantity: Double,
    val percent: Int,
)

/** Zona pentru dropdown-uri (id + nume). */
data class ZonePickItem(
    val id: Long,
    val name: String,
)

/** O intrare lucrare-pe-zona (zone_lucrari), individuala — pentru edit/stergere. */
data class LucrareEntryItem(
    val zoneId: Long,
    val zoneName: String,
    val lucrareId: Long,
    val lucrareName: String,
    val quantity: Double,
    val completedQuantity: Double,
    val unit: String,
)

/** O pontare facuta in proiect (pentru lista / raportari recente). */
data class PontareRowItem(
    val employeeName: String,
    val hours: Double,
    val lucrareName: String,
    val quantity: Double,
    val unit: String,
    val quality: Double,
    val date: String,
    val dateSort: Long,
    val zoneName: String,
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val zoneRepository: ZoneRepository,
    private val historyRepository: HistoryRepository,
    private val materialRepository: MaterialRepository,
    private val uneltaRepository: UneltaRepository,
    private val siteOperationsRepository: SiteOperationsRepository,
    private val zoneHistoryRepository: ZoneHistoryRepository,
    private val skillRepository: SkillRepository,
    private val userProjectRepository: UserProjectRepository,
    private val userRepository: UserRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState: MutableStateFlow<ProjectDetailScreenUi> =
        MutableStateFlow(ProjectDetailScreenUi())
    val uiState: StateFlow<ProjectDetailScreenUi> = _uiState.asStateFlow()

    private var currentProjectId: Long? = null
    private var implicitZoneId: Long? = null

    fun load(projectId: Long) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                _uiState.update {
                    it.copy(error = "Proiectul nu exista!")
                }
                Log.e(TAG, "The Project is null")
            } else {
                val companyId = project.companyId
                val zones: List<Zone> = zoneRepository.getZonesForProject(project.projectId)
                    .getOrDefault(emptyList())
                val zoneIds = zones.map { it.id }
                val histories: List<History> = historyRepository.getByZones(zoneIds)
                    .getOrDefault(emptyList())
                val zoneHistories: List<ZoneHistory> = zoneHistoryRepository.getByZones(zoneIds)
                    .getOrDefault(emptyList())
                val skillsById: Map<Long, Lucrare> = skillRepository.getSkillsForCompany(companyId)
                    .getOrDefault(emptyList())
                    .associateBy { it.id }
                val assignedUserIds: List<Long> = userProjectRepository
                    .getAssignmentsForProject(project.projectId)
                    .getOrDefault(emptyList())
                    .map { it.userId }
                val usersById: Map<Long, User> = userRepository.getEmployeesByCompanyId(companyId)
                    .getOrDefault(emptyList())
                    .associateBy { it.idUser }
                val materials = materialRepository.getByProject(project.projectId)
                    .getOrDefault(emptyList())
                val materialsCost = materials
                    .sumOf { it.totalCost.toDouble() }
                val toolsResult = uneltaRepository.getByCompany(companyId)
                val tools = toolsResult.getOrDefault(emptyList())
                val attendance = siteOperationsRepository
                    .getAttendance(project.projectId, operationalAttendanceDate())
                val presentUserIds = attendance
                    .filter { it.status.equals("prezent", ignoreCase = true) }
                    .map { it.userId }
                    .toSet()
                val presentCount = presentUserIds.size
                // O lucrare poate fi adăugată în mai multe zone. Pentru proiect, o agregăm
                // după lucrare, iar totalul este suma tuturor cantităților planificate.
                val mixLucrari = buildMixLucrari(zoneHistories, histories, skillsById)
                val totalWorkSurface = mixLucrari.sumOf { it.totalQuantity }
                val finishedSurface = getFinishedQuantity(project, zones)
                val progressPercent = getProgressPercent(finishedSurface, totalWorkSurface)
                val projectPace = getWorkPace(histories)
                val companyPace = getCompletedProjectsPace(companyId)
                val teamMembers = buildTeam(assignedUserIds, usersById, histories, presentUserIds)
                val salaryCosts = getTeamSalaryCosts(project, teamMembers)
                val projectCosts = salaryCosts + materialsCost
                val realZones = zones.filter { !it.isImplicit }
                currentProjectId = project.projectId
                implicitZoneId = zones.firstOrNull { it.isImplicit }?.id
                _uiState.update {
                    it.copy(
                        name = project.title,
                        address = project.adress,
                        status = computeStatus(project, progressPercent),
                        progressPercent = progressPercent,
                        endDate = project.endDate.formatRoLong(),
                        estimatedEndDate = getEstimatedEndDate(project, zones),
                        finishedQuantity = finishedSurface,
                        totalQuantity = totalWorkSurface,
                        projectPace = projectPace,
                        companyCompletedProjectsPace = companyPace,
                        presentOnSiteCount = presentCount,
                        pontariCount = histories.size,
                        risks = 0,
                        possibleGains = ((project.budget ?: 0f).toDouble() - projectCosts),
                        budget = (project.budget)?.toDouble() ?: 0.0,
                        projectCosts = projectCosts,
                        materialCosts = materialsCost,
                        materials = materials.sortedBy { it.name.lowercase() },
                        tools = tools.sortedBy { it.name.lowercase() },
                        toolsUnavailable = toolsResult.isFailure,
                        graphPoints = getPointsForAverageWorkGraphic(histories),
                        graphSeries = buildGraphSeries(histories, skillsById),
                        mixLucrari = mixLucrari,
                        lucrariEntries = buildLucrariEntries(zoneHistories, skillsById, zones),
                        team = teamMembers,
                        teamSalaryTotal = teamMembers.sumOf { it.salary },
                        zoneItems = buildZones(zones),
                        zonePickers = realZones.map { zone -> ZonePickItem(zone.id, zone.name ?: "Zona") },
                        availableSkills = skillsById.values.toList(),
                        pontari = buildPontari(histories, usersById, skillsById, zones),
                        error = null
                    )
                }
            }

        }

    }

    // ---- Mutatii: zone si lucrari (se adauga/editeaza din popup-urile ecranului de detaliu) ----

    fun addZone(name: String) {
        val pid = currentProjectId ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            zoneRepository.createZone(
                ZoneInsert(projectId = pid, name = name.trim(), surface = 0f, isImplicit = false)
            ).onFailure { Log.e(TAG, "Adaugare zona esuata", it) }
            load(pid)
        }
    }

    fun addMaterial(name: String, quantity: Float, unit: String, unitCost: Float) {
        val pid = currentProjectId ?: return
        if (name.isBlank() || quantity <= 0f || unitCost < 0f) return
        viewModelScope.launch {
            materialRepository.add(
                MaterialInsert(
                    projectId = pid,
                    name = name.trim(),
                    quantity = quantity,
                    unit = unit.trim().ifBlank { "buc" },
                    unitCost = unitCost,
                )
            ).onFailure { Log.e(TAG, "Adăugare material eșuată", it) }
            load(pid)
        }
    }

    fun updateZone(zoneId: Long, name: String, surface: Float) {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            zoneRepository.updateZone(zoneId, name.trim(), surface)
                .onFailure { Log.e(TAG, "Editare zona esuata", it) }
            load(pid)
        }
    }

    fun deleteZone(zoneId: Long) {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            zoneRepository.deleteZone(zoneId)
                .onFailure { Log.e(TAG, "Stergere zona esuata", it) }
            load(pid)
        }
    }

    /**
     * Adauga o lucrare (cu cantitate) pe o zona. Daca proiectul n-are zone reale, se foloseste
     * zona implicita. Cantitatea totala a proiectului creste (incrementam suprafata zonei).
     */
    fun addLucrare(zoneId: Long?, lucrareId: Long, quantity: Float) {
        val pid = currentProjectId ?: return
        val targetZone = zoneId ?: implicitZoneId ?: return
        if (quantity <= 0f) return
        viewModelScope.launch {
            zoneHistoryRepository.add(
                ZoneHistoryInsert(zoneId = targetZone, lucrareId = lucrareId, totalQuantity = quantity)
            ).onFailure { Log.e(TAG, "Adaugare lucrare esuata", it) }
            zoneRepository.addTotalSurface(targetZone, quantity)
                .onFailure { Log.e(TAG, "Incrementare suprafata esuata", it) }
            load(pid)
        }
    }

    private fun buildPontari(
        histories: List<History>,
        usersById: Map<Long, User>,
        skillsById: Map<Long, Lucrare>,
        zones: List<Zone>,
    ): List<PontareRowItem> {
        val zoneNameById = zones.associate { it.id to (it.name ?: "Zona") }
        return histories
            .sortedByDescending { it.workDate }
            .map { h ->
                val skill = skillsById[h.idLucrare]
                PontareRowItem(
                    employeeName = usersById[h.userId]?.fullName ?: "Utilizator #${h.userId}",
                    hours = h.hours ?: 0.0,
                    lucrareName = skill?.name ?: "Lucrare",
                    quantity = h.quantity.toDouble(),
                    unit = skill?.unit ?: "mp",
                    quality = (h.quality ?: 0f).toDouble(),
                    date = h.workDate?.let { "${it.dayOfMonth} ${monthShortPontari(it.monthNumber)}" } ?: "—",
                    dateSort = h.workDate?.toEpochDays()?.toLong() ?: 0L,
                    zoneName = zoneNameById[h.idZona] ?: "Zona",
                )
            }
    }

    /** Top 4 lucrari ale proiectului, fiecare cu punctele zilnice — pentru graficul cu linii. */
    private fun buildGraphSeries(histories: List<History>, skillsById: Map<Long, Lucrare>): List<ChartSeries> =
        histories.filter { it.workDate != null }
            .groupBy { it.idLucrare }
            .entries
            .sortedByDescending { (_, rows) -> rows.sumOf { it.quantity.toDouble() } }
            .take(4)
            .map { (lucrareId, rows) ->
                val points = rows.groupBy { it.workDate!! }
                    .map { (d, list) -> AverageWorkGraphic(d, list.sumOf { it.quantity.toDouble() }) }
                    .sortedBy { it.date }
                ChartSeries(name = skillsById[lucrareId]?.name ?: "Lucrare", points = points)
            }

    private fun monthShortPontari(m: Int): String =
        listOf("ian.", "feb.", "mar.", "apr.", "mai", "iun.", "iul.", "aug.", "sep.", "oct.", "nov.", "dec.")[(m - 1).coerceIn(0, 11)]

    fun updateLucrare(zoneId: Long, lucrareId: Long, oldQuantity: Float, newQuantity: Float) {
        val pid = currentProjectId ?: return
        if (newQuantity <= 0f) return
        viewModelScope.launch {
            zoneHistoryRepository.updateQuantity(zoneId, lucrareId, newQuantity)
                .onFailure { Log.e(TAG, "Editare lucrare esuata", it) }
            zoneRepository.addTotalSurface(zoneId, newQuantity - oldQuantity)
            load(pid)
        }
    }

    fun deleteLucrare(zoneId: Long, lucrareId: Long, quantity: Float) {
        val pid = currentProjectId ?: return
        viewModelScope.launch {
            zoneHistoryRepository.remove(zoneId, lucrareId)
                .onFailure { Log.e(TAG, "Stergere lucrare esuata", it) }
            zoneRepository.addTotalSurface(zoneId, -quantity)
            load(pid)
        }
    }

    private fun buildLucrariEntries(
        zoneHistories: List<ZoneHistory>,
        skillsById: Map<Long, Lucrare>,
        zones: List<Zone>,
    ): List<LucrareEntryItem> {
        val zoneNameById = zones.associate { it.id to (it.name ?: "Zona") }
        return zoneHistories.map { zh ->
            val skill = skillsById[zh.lucrareId]
                LucrareEntryItem(
                zoneId = zh.zoneId,
                zoneName = zoneNameById[zh.zoneId] ?: "Zona",
                lucrareId = zh.lucrareId,
                lucrareName = skill?.name ?: "Lucrare",
                quantity = zh.totalQuantity.toDouble(),
                completedQuantity = zh.completedQuantity.toDouble(),
                unit = skill?.unit ?: "mp",
            )
        }.sortedByDescending { it.quantity }
    }

    /** Creeaza o lucrare noua in firma (apare in Preturi) si o adauga pe zona cu o cantitate. */
    fun addNewLucrare(zoneId: Long?, name: String, unit: String, price: Float, quantity: Float) {
        val pid = currentProjectId ?: return
        val companyId = sessionStore.state.value.user?.companyId ?: return
        val targetZone = zoneId ?: implicitZoneId ?: return
        if (name.isBlank() || quantity <= 0f) return
        viewModelScope.launch {
            val skill = skillRepository.createSkill(
                LucrareInsert(name = name.trim(), unit = unit.trim().ifBlank { "mp" }, price = price, points = 0L, idFirma = companyId)
            ).getOrNull()
            if (skill != null) {
                zoneHistoryRepository.add(
                    ZoneHistoryInsert(zoneId = targetZone, lucrareId = skill.id, totalQuantity = quantity)
                ).onFailure { Log.e(TAG, "Adaugare lucrare noua esuata", it) }
                zoneRepository.addTotalSurface(targetZone, quantity)
            }
            load(pid)
        }
    }

    /** Zonele reale ale proiectului (fara cea implicita) + procentul finalizat. */
    private fun buildZones(zones: List<Zone>): List<ZoneItem> =
        zones.filter { !it.isImplicit }.map { zone ->
            val total = zone.surface.toDouble()
            val completed = (zone.surfaceCompleted ?: 0f).toDouble()
            ZoneItem(
                id = zone.id,
                name = zone.name ?: "Zona",
                completedQuantity = completed,
                totalQuantity = total,
                percent = if (total > 0) (completed / total * 100).toInt() else 0,
            )
        }

    /** Grupeaza zone_lucrari pe lucrare si insumeaza suprafata totala alocata fiecareia. */
    private fun buildMixLucrari(
        zoneHistories: List<ZoneHistory>,
        histories: List<History>,
        skillsById: Map<Long, Lucrare>,
    ): List<MixLucrareItem> =
        zoneHistories
            .groupBy { it.lucrareId }
            .map { (lucrareId, rows) ->
                val skill = skillsById[lucrareId]
                val workHistory = histories.filter { it.idLucrare == lucrareId }
                val workedDays = workHistory.mapNotNull { it.workDate }.distinct().size
                MixLucrareItem(
                    name = skill?.name ?: "Lucrare #$lucrareId",
                    totalQuantity = rows.sumOf { it.totalQuantity.toDouble() },
                    completedQuantity = rows.sumOf { it.completedQuantity.toDouble() },
                    pacePerDay = if (workedDays > 0) workHistory.sumOf { it.quantity.toDouble() } / workedDays else 0.0,
                    unit = skill?.unit ?: "mp",
                )
            }
            .sortedByDescending { it.totalQuantity }

    /** Pentru fiecare angajat alocat: salariul si ritmul mediu (mp/zi) din pontari. */
    private fun buildTeam(
        assignedUserIds: List<Long>,
        usersById: Map<Long, User>,
        histories: List<History>,
        presentUserIds: Set<Long>,
    ): List<TeamMemberItem> {
        val historiesByUser = histories.groupBy { it.userId }
        return assignedUserIds.mapNotNull { userId ->
            val user = usersById[userId]
            // Echipa de șantier exclude conturile administrative și clienții atașați proiectului.
            if (user?.role?.equals("admin", ignoreCase = true) == true ||
                user?.role?.equals("client", ignoreCase = true) == true
            ) return@mapNotNull null
            val userHistories = historiesByUser[userId].orEmpty()
            val workedDays = userHistories.mapNotNull { it.workDate }.distinct().size
            val totalQuantity = userHistories.sumOf { it.quantity.toDouble() }
            val mpPerDay = if (workedDays > 0) totalQuantity / workedDays else 0.0
            TeamMemberItem(
                userId = userId,
                name = user?.fullName ?: "Utilizator #$userId",
                role = user.projectRoleLabel(),
                salary = user?.salary ?: 0.0,
                mpPerDay = mpPerDay,
                isPresent = userId in presentUserIds,
            )
        }.sortedByDescending { it.mpPerDay }
    }

    private fun User?.projectRoleLabel(): String = when (this?.role?.lowercase()) {
        "manager" -> "Manager proiect"
        "inginer" -> "Inginer"
        "sef_echipa" -> "Șef echipă"
        "angajat" -> "Angajat"
        "client" -> "Client"
        "admin" -> "Administrator"
        else -> "Angajat"
    }

    private fun computeStatus(project: Project, progressPercent: Int): ProjectStatus {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        return when {
            project.isOffer          -> ProjectStatus.OFERTA
            progressPercent >= 100   -> ProjectStatus.FINALIZAT
            project.endDate < today  -> ProjectStatus.INTARZIAT
            else                     -> ProjectStatus.ACTIV
        }
    }

    private fun getDaysSinceProjectIsActive(project: Project): Int {
        val startDate = project.startDate.toLocalDateTime(timeZone).date
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val daySinceProjectIsActive = startDate.daysUntil(today)

        return daySinceProjectIsActive
    }

    private fun getTotalProjectDays(project: Project): Int {
        val startDate = project.startDate.toLocalDateTime(timeZone).date
        val endDate = project.endDate
        val totalDays = startDate.daysUntil(endDate)

        return totalDays
    }

    private fun getEstimatedEndDate(project: Project, zones: List<Zone>): String {
        val surfaceCompletedInProject: Double = getFinishedQuantity(project, zones)
        val today: LocalDate = Clock.System.now().toLocalDateTime(timeZone).date
        val daysSinceProjectIsActive = getDaysSinceProjectIsActive(project)

        if (daysSinceProjectIsActive == 0) return "Unpredictable"

        val averageCompletedSurface = surfaceCompletedInProject / daysSinceProjectIsActive
        val surfaceLeft = getSurfaceLeft(project, zones)

        if (averageCompletedSurface == 0.0) return "Unpredictable"

        val estimatedDaysUntilEnd = surfaceLeft / averageCompletedSurface
        val estimatedEndDate: LocalDate = today
            .plus(estimatedDaysUntilEnd.toInt(), DateTimeUnit.DAY)

        return estimatedEndDate.formatRoLong()
    }

    private fun getFinishedQuantity(project: Project, zones: List<Zone>): Double {
        val surfaceCompletedInProject: Double = zones
            .map { zone -> zone.surfaceCompleted ?: 0.0f}
            .sumOf { it.toDouble() }
        return surfaceCompletedInProject
    }

    private fun getSurfaceLeft(project: Project, zones: List<Zone>): Double {
        val totalProjectSurface = zones
            .sumOf { it.surface.toDouble() }
        val completedProjectSurface = zones
            .sumOf { it.surfaceCompleted?.toDouble() ?: 0.0 }

        return totalProjectSurface - completedProjectSurface
    }

    fun getProgressPercent(zones: List<Zone>): Int {
        val totalQuantity: Double = zones.sumOf { zone ->
            zone.surface.toDouble()
        }
        val finishedQuantity: Double =  zones.sumOf { zone ->
            zone.surfaceCompleted?.toDouble() ?: 0.0
        }

        if (totalQuantity > 0 && finishedQuantity > 0) {
            val percent: Int = (finishedQuantity / totalQuantity * 100).toInt()
            return percent
        }

        return 0

    }

    private fun getProgressPercent(finished: Double, total: Double): Int =
        if (total > 0.0) ((finished / total) * 100).toInt().coerceIn(0, 100) else 0

    /** Ritmul este calculat din zilele în care s-au raportat efectiv pontări. */
    private fun getWorkPace(histories: List<History>): Double {
        val workedDays = histories.mapNotNull { it.workDate }.distinct().size
        return if (workedDays > 0) histories.sumOf { it.quantity.toDouble() } / workedDays else 0.0
    }

    /**
     * Baza nu are încă un câmp explicit de „finalizat”; folosim proiectele al căror termen a trecut.
     * Media este a ritmului zilnic din pontările fiecărui proiect eligibil.
     */
    private suspend fun getCompletedProjectsPace(companyId: Long): Double {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val completed = projectRepository.getProjectsByCompanyId(companyId)
            .filter { it.endDate < today }
        if (completed.isEmpty()) return 0.0
        val paces = completed.mapNotNull { completedProject ->
            val zones = zoneRepository.getZonesForProject(completedProject.projectId).getOrDefault(emptyList())
            val histories = historyRepository.getByZones(zones.map { it.id }).getOrDefault(emptyList())
            getWorkPace(histories).takeIf { it > 0.0 }
        }
        return if (paces.isEmpty()) 0.0 else paces.average()
    }

    // in the future we might need to refactor this method, by predicting the gains
    private fun getPossibleGains(project: Project): Double {
        val budget = project.budget ?: 0f
        // the formula will change over time when we will add the tools and rents
        val costs = getProjectCosts(project)

        return budget - costs
    }

    fun getProjectCosts(project: Project): Double {
        val salaryPerDay = project.totalSalaryPerMonth / 30
        // the formula will change over time when we will add the tools and rents
        val costs = getTotalProjectDays(project) * salaryPerDay

        return costs.toDouble()
    }

    private fun getTeamSalaryCosts(project: Project, team: List<TeamMemberItem>): Double {
        val monthlyTeamSalary = team.sumOf { it.salary }
        val dailySalary = if (monthlyTeamSalary > 0.0) monthlyTeamSalary / 30.0 else project.totalSalaryPerMonth / 30.0
        return getTotalProjectDays(project).coerceAtLeast(0) * dailySalary
    }

    /** După 03:00 începe o nouă zi operațională; înainte, prezența aparține încă zilei anterioare. */
    private fun operationalAttendanceDate(): LocalDate {
        val now = Clock.System.now().toLocalDateTime(timeZone)
        return if (now.hour < 3) now.date.minus(1, DateTimeUnit.DAY) else now.date
    }

    fun getPointsForAverageWorkGraphic(histories: List<History>): List<AverageWorkGraphic> {
        return histories
            .filter { it.workDate != null }
            .groupBy { it.workDate!! }
            .map { (date, entries) ->
                AverageWorkGraphic(
                    date = date,
                    quantity = entries.sumOf { it.quantity.toDouble() },
                )
            }
            .sortedBy { it.date }
    }

    companion object {
        private const val TAG = "ProjectDetailVM"
        val timeZone = TimeZone.currentSystemDefault()
        private val MonthsRo = listOf(
            "ianuarie", "februarie", "martie", "aprilie", "mai", "iunie",
            "iulie", "august", "septembrie", "octombrie", "noiembrie", "decembrie",
        )

        private fun LocalDate.formatRoLong(): String =
            "$dayOfMonth ${MonthsRo[monthNumber - 1]} $year"

    }
}
