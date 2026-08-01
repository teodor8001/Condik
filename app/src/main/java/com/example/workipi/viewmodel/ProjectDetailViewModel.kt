package com.example.workipi.viewmodel

import android.provider.MediaStore.UNKNOWN_STRING
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.History
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.LucrareInsert
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.ProjectStatus
import com.example.workipi.data.model.User
import com.example.workipi.data.model.Zone
import com.example.workipi.data.model.ZoneHistory
import com.example.workipi.data.model.ZoneHistoryInsert
import com.example.workipi.data.model.ZoneInsert
import com.example.workipi.repository.HistoryRepository
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.SkillRepository
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
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
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
    val unit: String,
)

/** Un membru al echipei alocate: nume, salariu si ritmul lui mediu (din istoric_pontari). */
data class TeamMemberItem(
    val userId: Long,
    val name: String,
    val salary: Double,
    val mpPerDay: Double,
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
    private val zoneHistoryRepository: ZoneHistoryRepository,
    private val skillRepository: SkillRepository,
    private val userProjectRepository: UserProjectRepository,
    private val userRepository: UserRepository,
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
                val progressPercent = getProgressPercent(zones)
                val teamMembers = buildTeam(assignedUserIds, usersById, histories)
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
                        finishedQuantity = getFinishedQuantity(project, zones),
                        totalQuantity = zones.sumOf { zone -> zone.surface.toDouble() },
                        pontariCount = histories.size,
                        risks = 0,
                        possibleGains = getPossibleGains(project),
                        budget = (project.budget)?.toDouble() ?: 0.0,
                        projectCosts = getProjectCosts(project),
                        graphPoints = getPointsForAverageWorkGraphic(histories),
                        graphSeries = buildGraphSeries(histories, skillsById),
                        mixLucrari = buildMixLucrari(zoneHistories, skillsById),
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
                unit = skill?.unit ?: "mp",
            )
        }.sortedByDescending { it.quantity }
    }

    /** Creeaza o lucrare noua in firma (apare in Preturi) si o adauga pe zona cu o cantitate. */
    fun addNewLucrare(zoneId: Long?, name: String, unit: String, price: Float, quantity: Float) {
        val pid = currentProjectId ?: return
        val companyId = MockSession.currentUser?.idCompany ?: return
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
        skillsById: Map<Long, Lucrare>,
    ): List<MixLucrareItem> =
        zoneHistories
            .groupBy { it.lucrareId }
            .map { (lucrareId, rows) ->
                val skill = skillsById[lucrareId]
                MixLucrareItem(
                    name = skill?.name ?: "Lucrare #$lucrareId",
                    totalQuantity = rows.sumOf { it.totalQuantity.toDouble() },
                    unit = skill?.unit ?: "mp",
                )
            }
            .sortedByDescending { it.totalQuantity }

    /** Pentru fiecare angajat alocat: salariul si ritmul mediu (mp/zi) din pontari. */
    private fun buildTeam(
        assignedUserIds: List<Long>,
        usersById: Map<Long, User>,
        histories: List<History>,
    ): List<TeamMemberItem> {
        val historiesByUser = histories.groupBy { it.userId }
        return assignedUserIds.mapNotNull { userId ->
            val user = usersById[userId]
            // Echipa = doar angajati si ingineri, fara admini.
            if (user?.role?.equals("admin", ignoreCase = true) == true) return@mapNotNull null
            val userHistories = historiesByUser[userId].orEmpty()
            val workedDays = userHistories.mapNotNull { it.workDate }.distinct().size
            val totalQuantity = userHistories.sumOf { it.quantity.toDouble() }
            val mpPerDay = if (workedDays > 0) totalQuantity / workedDays else 0.0
            TeamMemberItem(
                userId = userId,
                name = user?.fullName ?: "Utilizator #$userId",
                salary = user?.salary ?: 0.0,
                mpPerDay = mpPerDay,
            )
        }.sortedByDescending { it.mpPerDay }
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
