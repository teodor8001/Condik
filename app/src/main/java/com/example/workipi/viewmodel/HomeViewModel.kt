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
    val chartDays: Int = MP_LOOKBACK_DAYS,
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
                        activeProjects = activeProjects,
                        avgMpPerDay = avgMpPerDay,
                        peopleCheckedIn = checkedIn,
                        chartSeries = chartSeries,
                        barChart = barChart,
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
