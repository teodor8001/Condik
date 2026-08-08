package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.session.SessionStore
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.ProjectInsert
import com.example.workipi.data.model.User
import com.example.workipi.data.model.ZoneHistoryInsert
import com.example.workipi.data.model.ZoneInsert
import com.example.workipi.repository.HistoryRepository
import com.example.workipi.repository.ZoneHistoryRepository
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.SkillRepository
import com.example.workipi.repository.UserProjectRepository
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

/** O lucrare ceruta la oferta + cantitate + media mp/zi a celor care stiu sa o faca. */
data class OfferLucrareDraft(
    val key: Long,
    val lucrareId: Long? = null,
    val lucrareName: String = "",
    val unit: String = "",
    val quantity: String = "",
    val avgMpPerDay: Double = 0.0,
)

data class AddProjectUiState(
    val title: String = "",
    val address: String = "",
    val budget: String = "",
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val isOffer: Boolean = false,
    val availableEmployees: List<User> = emptyList(),
    val selectedEmployeeIds: Set<Long> = emptySet(),
    val availableSkills: List<Lucrare> = emptyList(),
    val requiredLucrari: List<OfferLucrareDraft> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdProject: Project? = null,
)

@HiltViewModel
class AddProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val zoneRepository: ZoneRepository,
    private val userRepository: UserRepository,
    private val userProjectRepository: UserProjectRepository,
    private val skillRepository: SkillRepository,
    private val historyRepository: HistoryRepository,
    private val zoneHistoryRepository: ZoneHistoryRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProjectUiState())
    val uiState: StateFlow<AddProjectUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val companyId = sessionStore.state.value.user?.companyId ?: return
        viewModelScope.launch {
            val employees = userRepository.getEmployeesByCompanyId(companyId).getOrDefault(emptyList())
            val skills = skillRepository.getSkillsForCompany(companyId).getOrDefault(emptyList())
            _uiState.update { it.copy(availableEmployees = employees, availableSkills = skills) }
        }
    }

    fun addRequiredLucrare() = _uiState.update { state ->
        val newKey = (state.requiredLucrari.maxOfOrNull { it.key } ?: -1L) + 1L
        state.copy(requiredLucrari = state.requiredLucrari + OfferLucrareDraft(key = newKey))
    }

    fun removeRequiredLucrare(key: Long) = _uiState.update { state ->
        state.copy(requiredLucrari = state.requiredLucrari.filter { it.key != key })
    }

    fun onRequiredQuantityChange(key: Long, value: String) = _uiState.update { state ->
        state.copy(
            requiredLucrari = state.requiredLucrari.map {
                if (it.key == key) it.copy(quantity = value.filter { c -> c.isDigit() || c == '.' }) else it
            }
        )
    }

    fun onRequiredLucrareSelect(key: Long, lucrareId: Long) {
        val skill = _uiState.value.availableSkills.firstOrNull { it.id == lucrareId } ?: return
        _uiState.update { state ->
            state.copy(
                requiredLucrari = state.requiredLucrari.map {
                    if (it.key == key) it.copy(lucrareId = lucrareId, lucrareName = skill.name, unit = skill.unit) else it
                }
            )
        }
        // Media mp/zi a celor care stiu lucrarea — pentru recomandarea de personal.
        viewModelScope.launch {
            val avg = computeAvgMpPerDay(lucrareId)
            _uiState.update { state ->
                state.copy(
                    requiredLucrari = state.requiredLucrari.map {
                        if (it.key == key) it.copy(avgMpPerDay = avg) else it
                    }
                )
            }
        }
    }

    /** Media mp/zi pe o lucrare = media, intre angajatii care au pontat-o, a ritmului fiecaruia. */
    private suspend fun computeAvgMpPerDay(lucrareId: Long): Double {
        val histories = historyRepository.getBySkill(lucrareId).getOrDefault(emptyList())
        val rates = histories.groupBy { it.userId }.values.mapNotNull { rows ->
            val days = rows.mapNotNull { it.workDate }.distinct().size
            val qty = rows.sumOf { it.quantity.toDouble() }
            if (days > 0) qty / days else null
        }
        return if (rates.isEmpty()) 0.0 else rates.average()
    }

    fun toggleEmployee(userId: Long) = _uiState.update { state ->
        val newSet = if (userId in state.selectedEmployeeIds)
            state.selectedEmployeeIds - userId
        else
            state.selectedEmployeeIds + userId
        state.copy(selectedEmployeeIds = newSet, errorMessage = null)
    }

    fun onTitleChange(value: String) =
        _uiState.update { it.copy(title = value, errorMessage = null) }

    fun onAddressChange(value: String) =
        _uiState.update { it.copy(address = value, errorMessage = null) }

    fun onBudgetChange(value: String) =
        _uiState.update { it.copy(budget = value.filter { c -> c.isDigit() || c == '.' }, errorMessage = null) }

    fun onStartDateChange(millis: Long?) =
        _uiState.update { it.copy(startDateMillis = millis, errorMessage = null) }

    fun onEndDateChange(millis: Long?) =
        _uiState.update { it.copy(endDateMillis = millis, errorMessage = null) }

    fun onIsOfferChange(value: Boolean) =
        _uiState.update { it.copy(isOffer = value, errorMessage = null) }

    /** Marcheaza fluxul ca fiind de oferta (apelat din ecran in functie de ruta). */
    fun setOffer(value: Boolean) = _uiState.update {
        if (it.isOffer == value) it else it.copy(isOffer = value)
    }

    fun submit() {
        val state = _uiState.value
        val companyId = sessionStore.state.value.user?.companyId
        val budget = state.budget.toFloatOrNull()

        val error = when {
            companyId == null -> "Nu am putut identifica firma. Reautentifica-te."
            state.title.isBlank() -> "Introdu denumirea proiectului."
            state.address.isBlank() -> "Introdu adresa."
            budget == null || budget <= 0f -> "Bugetul trebuie sa fie un numar pozitiv."
            state.endDateMillis == null -> "Alege termenul de finalizare."
            else -> null
        }

        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Data de start: cea aleasa sau acum (daca nu s-a ales).
        val startInstant = state.startDateMillis
            ?.let { Instant.fromEpochMilliseconds(it) }
            ?: Clock.System.now()

        viewModelScope.launch {
            projectRepository.createProject(
                ProjectInsert(
                    title = state.title.trim(),
                    adress = state.address.trim(),
                    budget = budget!!,
                    totalSalaryPerMonth = 0f,
                    startDate = startInstant,
                    endDate = Instant.fromEpochMilliseconds(state.endDateMillis!!)
                        .toLocalDateTime(TimeZone.UTC).date,
                    companyId = companyId!!,
                    isOffer = state.isOffer,
                )
            )
                .onSuccess { project ->
                    Log.d(TAG, "Proiect creat: id=${project.projectId}")
                    // Zonele reale se adauga ulterior din ecranul de detaliu. La creare punem
                    // o zona implicita (necesara in DB) si atasam pe ea lucrarile cerute la oferta.
                    val implicitZone = zoneRepository.createZone(
                        ZoneInsert(
                            projectId = project.projectId,
                            name = "_implicit",
                            surface = 0f,
                            isImplicit = true,
                        )
                    ).getOrNull()
                    if (implicitZone != null) {
                        var totalQty = 0f
                        state.requiredLucrari.forEach { req ->
                            val qty = req.quantity.toFloatOrNull()
                            if (req.lucrareId != null && qty != null && qty > 0f) {
                                zoneHistoryRepository.add(
                                    ZoneHistoryInsert(zoneId = implicitZone.id, lucrareId = req.lucrareId, totalQuantity = qty)
                                ).onFailure { e -> Log.e(TAG, "Lucrare oferta neadaugata", e) }
                                totalQty += qty
                            }
                        }
                        if (totalQty > 0f) {
                            zoneRepository.addTotalSurface(implicitZone.id, totalQty)
                        }
                    }

                    state.selectedEmployeeIds.forEach { userId ->
                        userProjectRepository.assignUser(userId, project.projectId)
                            .onFailure { e -> Log.e(TAG, "Asignare user=$userId esuata", e) }
                    }

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
