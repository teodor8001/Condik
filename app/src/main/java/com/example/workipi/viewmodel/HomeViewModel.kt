package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.repository.AuthRepository
import com.example.workipi.repository.SkillRepository
import com.example.workipi.repository.HistoryRepository
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.UserRepository
import com.example.workipi.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
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

private const val UNIT_MP = "mp"
private const val MP_LOOKBACK_DAYS = 30
private const val CHART_TOP_N_SKILLS = 4

data class MpPerDayPoint(
    val date: LocalDate,
    val mp: Float,
)

data class SkillMpSeries(
    val skillId: Long,
    val name: String,
    /** Una pentru fiecare zi din fereastra (30 zile), in ordine cronologica, ultima = azi */
    val points: List<MpPerDayPoint>,
    val totalMp: Float,
)

data class SkillMpBar(
    val skillId: Long,
    val name: String,
    val totalMp: Float,
)

data class HomeUiState(
    val activeProjects: Int = 0,
    val avgMpPerDay: Float = 0f,
    val peopleCheckedIn: Long = 0,
    val inspectionsToDo: Int = 5, // mocked deocamdata
    val chartSeries: List<SkillMpSeries> = emptyList(),
    val barChart: List<SkillMpBar> = emptyList(),
    val chartSeriesFull: List<ChartSeries> = emptyList(),
    // Pagina 2 — metrici financiare
    val companyProgress: Float? = null,   // diferenta procent profit intre ultimele 2 contracte finalizate
    val efficiency: Float = 0f,           // media procent profit din proiectele finalizate
    val budgetsInProgress: Float = 0f,    // media procent profit din toate proiectele
    val anticipatedProfit: Double = 0.0,  // suma profit din proiectele active
    val finalizedCount: Int = 0,
    val activeCount: Int = 0,
    val offersCount: Int = 0,             // proiecte viitoare (oferte)
    val totalBudget: Double = 0.0,        // suma bugetelor
    val totalProfit: Double = 0.0,        // suma profitului din toate proiectele
    val chartDays: Int = MP_LOOKBACK_DAYS,
    val companyName: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val zoneRepository: ZoneRepository,
    private val historyRepository: HistoryRepository,
    private val skillRepository: SkillRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val companyIdAsync: Deferred<Long> = viewModelScope.async {
        authRepository.getCompanyIdFromAuthUser()
    }

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val companyId = companyIdAsync.await()
                val companyName = runCatching { authRepository.getCompanyName(companyId) }.getOrNull()
                // Proiecte + zonele lor → calculam cate sunt active (progress < 1)
                val projects = projectRepository.getProjectsByCompanyId(companyId)
                val zones = zoneRepository.getZonesForProjects(projects.map { it.projectId })
                    .getOrDefault(emptyList())
                val zonesByProject = zones.groupBy { it.projectId }
                val activeProjects = projects.count { p ->
                    val pZones = zonesByProject[p.projectId].orEmpty()
                    val total = pZones.sumOf { it.surface.toDouble() }
                    val done = pZones.sumOf { (it.surfaceCompleted ?: 0f).toDouble() }
                    total <= 0.0 || done < total
                }

                // ---- Metrici financiare (pagina 2) ----
                val tz = TimeZone.currentSystemDefault()
                data class ProjStat(val profit: Double, val profitPct: Double, val finalized: Boolean, val endDate: LocalDate)
                val stats = projects.map { p ->
                    val pZones = zonesByProject[p.projectId].orEmpty()
                    val total = pZones.sumOf { it.surface.toDouble() }
                    val done = pZones.sumOf { (it.surfaceCompleted ?: 0f).toDouble() }
                    val finalized = total > 0.0 && done >= total
                    val startDate = p.startDate.toLocalDateTime(tz).date
                    val days = startDate.daysUntil(p.endDate).coerceAtLeast(0)
                    val costs = days * (p.totalSalaryPerMonth / 30.0)
                    val budget = (p.budget ?: 0f).toDouble()
                    val profit = budget - costs
                    val profitPct = if (budget > 0.0) profit / budget * 100.0 else 0.0
                    ProjStat(profit, profitPct, finalized, p.endDate)
                }
                val finalizedStats = stats.filter { it.finalized }
                val activeStats = stats.filter { !it.finalized }
                val lastTwoFinalized = finalizedStats.sortedByDescending { it.endDate }.take(2)
                val companyProgress = if (lastTwoFinalized.size == 2)
                    (lastTwoFinalized[0].profitPct - lastTwoFinalized[1].profitPct).toFloat() else null
                val efficiency = if (finalizedStats.isNotEmpty()) finalizedStats.map { it.profitPct }.average().toFloat() else 0f
                val budgetsInProgress = if (stats.isNotEmpty()) stats.map { it.profitPct }.average().toFloat() else 0f
                val anticipatedProfit = activeStats.sumOf { it.profit }
                val totalBudget = projects.sumOf { (it.budget ?: 0f).toDouble() }
                val totalProfit = stats.sumOf { it.profit }
                val offersCount = runCatching { projectRepository.getOffersByCompanyId(companyId).size }.getOrDefault(0)

                // Media mp/zi: pontari peste toate zonele firmei, in ultimele 30 zile,
                // doar pe lucrari cu unitate "mp"
                val skills = skillRepository.getSkillsForCompany(companyId)
                    .getOrDefault(emptyList())
                val mpSkillIds = skills.filter { it.unit.equals(UNIT_MP, ignoreCase = true) }
                    .map { it.id }.toSet()

                val projectHistoryContor = if (zones.isEmpty()) emptyList()
                else historyRepository.getByZones(zones.map { it.id }).getOrDefault(emptyList())

                // Fereastra: ultimele 30 zile, ULTIMA zi = azi
                val today = today()
                val firstDay = today.minus(MP_LOOKBACK_DAYS - 1, DateTimeUnit.DAY)
                val recent = projectHistoryContor.filter { p ->
                    p.idLucrare in mpSkillIds && (p.workDate?.let { it in firstDay..today } ?: false)
                }
                // Top 4 lucrari (mp) pe tot istoricul, fiecare cu punctele zilnice —
                // pentru graficul cu linii navigabil pe timp.
                val chartSeriesFull = projectHistoryContor
                    .filter { it.idLucrare in mpSkillIds && it.workDate != null }
                    .groupBy { it.idLucrare }
                    .entries
                    .sortedByDescending { (_, rows) -> rows.sumOf { it.quantity.toDouble() } }
                    .take(CHART_TOP_N_SKILLS)
                    .map { (id, rows) ->
                        val pts = rows.groupBy { it.workDate!! }
                            .map { (d, l) -> AverageWorkGraphic(d, l.sumOf { it.quantity.toDouble() }) }
                            .sortedBy { it.date }
                        ChartSeries(name = skills.firstOrNull { it.id == id }?.name ?: "Lucrare #$id", points = pts)
                    }

                val totalMp = recent.sumOf { it.quantity.toDouble() }
                val days = recent.mapNotNull { it.workDate }.distinct().size
                val avgMpPerDay = if (days > 0) (totalMp / days).toFloat() else 0f

                // Top 4 lucrari (mp) dupa total mp lucrati in ultimele 30 zile
                val skillName = skills.associateBy({ it.id }, { it.name })
                val perSkill = recent.groupBy { it.idLucrare }

                val topSkillIds = perSkill.entries
                    .sortedByDescending { (_, list) -> list.sumOf { it.quantity.toDouble() } }
                    .take(CHART_TOP_N_SKILLS)
                    .map { it.key }

                val chartSeries = topSkillIds.map { sid ->
                    val list = perSkill[sid].orEmpty()
                    val mpByDay = list.groupBy { it.workDate!! }
                        .mapValues { (_, ps) -> ps.sumOf { it.quantity.toDouble() }.toFloat() }
                    val pts = (0 until MP_LOOKBACK_DAYS).map { offset ->
                        val d = firstDay.plus(offset, DateTimeUnit.DAY)
                        MpPerDayPoint(date = d, mp = mpByDay[d] ?: 0f)
                    }
                    SkillMpSeries(
                        skillId = sid,
                        name = skillName[sid] ?: "Lucrare #$sid",
                        points = pts,
                        totalMp = pts.sumOf { it.mp.toDouble() }.toFloat(),
                    )
                }

                val barChart = chartSeries.map { s ->
                    SkillMpBar(
                        skillId = s.skillId,
                        name = s.name,
                        totalMp = s.totalMp,
                    )
                }


                // Total checked-in pe firma
                val checkedIn = userRepository.countCheckedIn(companyId).getOrDefault(0L)

                _uiState.update {
                    // TODO
                    it.copy(
                        isLoading = false,
                        companyName = companyName,
                        activeProjects = activeProjects,
                        avgMpPerDay = avgMpPerDay,
                        peopleCheckedIn = checkedIn,
                        chartSeries = chartSeries,
                        barChart = barChart,
                        chartSeriesFull = chartSeriesFull,
                        companyProgress = companyProgress,
                        efficiency = efficiency,
                        budgetsInProgress = budgetsInProgress,
                        anticipatedProfit = anticipatedProfit,
                        finalizedCount = finalizedStats.size,
                        activeCount = activeStats.size,
                        offersCount = offersCount,
                        totalBudget = totalBudget,
                        totalProfit = totalProfit,
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Eroare la incarcarea homescreen", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Nu am putut incarca datele.",
                    )
                }
            }
        }
    }

    private fun today(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    companion object {
        private const val TAG = "HomeVM"
    }
}
