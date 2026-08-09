package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
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
    val zoneKey: Long? = null,
    val avgMpPerDay: Double = 0.0,
)

data class OfferZoneDraft(
    val key: Long,
    val name: String,
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
    val zones: List<OfferZoneDraft> = emptyList(),
    val withoutZones: Boolean = false,
    val requiredLucrari: List<OfferLucrareDraft> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdProject: Project? = null,
)

@HiltViewModel
class AddProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val zoneRepository: ZoneRepository,
    private val userRepository: UserRepository,
    private val userProjectRepository: UserProjectRepository,
    private val skillRepository: SkillRepository,
    private val historyRepository: HistoryRepository,
    private val zoneHistoryRepository: ZoneHistoryRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddProjectUiState(isOffer = savedStateHandle.get<Boolean>("offer") ?: false)
    )
    val uiState: StateFlow<AddProjectUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val companyId = sessionStore.state.value.user?.companyId ?: return
        viewModelScope.launch {
            val employees = userRepository.getEmployeesByCompanyId(companyId).getOrDefault(emptyList())
            val skills = skillRepository.getSkillsForCompany(companyId).getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    availableEmployees = employees.filter { user ->
                        user.role?.lowercase() in setOf("angajat", "manager")
                    },
                    availableSkills = skills,
                )
            }
        }
    }

    fun addZone(name: String): Boolean {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Scrie numele zonei.") }
            return false
        }
        if (_uiState.value.zones.any { it.name.equals(cleanName, ignoreCase = true) }) {
            _uiState.update { it.copy(errorMessage = "Zona «$cleanName» este deja in lista.") }
            return false
        }
        _uiState.update { state ->
            val newKey = (state.zones.maxOfOrNull { it.key } ?: -1L) + 1L
            state.copy(
                zones = state.zones + OfferZoneDraft(key = newKey, name = cleanName),
                errorMessage = null,
            )
        }
        return true
    }

    fun removeZone(key: Long) = _uiState.update { state ->
        state.copy(
            zones = state.zones.filterNot { it.key == key },
            requiredLucrari = state.requiredLucrari.map { work ->
                if (work.zoneKey == key) work.copy(zoneKey = null) else work
            },
            errorMessage = null,
        )
    }

    fun onWithoutZonesChange(value: Boolean) = _uiState.update { state ->
        state.copy(
            withoutZones = value,
            requiredLucrari = if (value) {
                state.requiredLucrari.map { it.copy(zoneKey = null) }
            } else {
                state.requiredLucrari
            },
            errorMessage = null,
        )
    }

    fun addRequiredLucrare() = _uiState.update { state ->
        val newKey = (state.requiredLucrari.maxOfOrNull { it.key } ?: -1L) + 1L
        val initialZone = state.zones.singleOrNull()?.key
        state.copy(
            requiredLucrari = state.requiredLucrari + OfferLucrareDraft(
                key = newKey,
                zoneKey = if (state.withoutZones) null else initialZone,
            ),
            errorMessage = null,
        )
    }

    fun removeRequiredLucrare(key: Long) = _uiState.update { state ->
        state.copy(
            requiredLucrari = state.requiredLucrari.filter { it.key != key },
            errorMessage = null,
        )
    }

    fun onRequiredQuantityChange(key: Long, value: String) = _uiState.update { state ->
        state.copy(
            requiredLucrari = state.requiredLucrari.map {
                if (it.key == key) it.copy(quantity = value.filter { c -> c.isDigit() || c == '.' }) else it
            },
            errorMessage = null,
        )
    }

    fun onRequiredZoneSelect(key: Long, zoneKey: Long) = _uiState.update { state ->
        state.copy(
            requiredLucrari = state.requiredLucrari.map {
                if (it.key == key) it.copy(zoneKey = zoneKey) else it
            },
            errorMessage = null,
        )
    }

    fun onRequiredLucrareSelect(key: Long, lucrareId: Long) {
        val skill = _uiState.value.availableSkills.firstOrNull { it.id == lucrareId } ?: return
        _uiState.update { state ->
            state.copy(
                requiredLucrari = state.requiredLucrari.map {
                    if (it.key == key) it.copy(lucrareId = lucrareId, lucrareName = skill.name, unit = skill.unit) else it
                },
                errorMessage = null,
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

    fun validateStep(step: Int): Boolean {
        val state = _uiState.value
        val budget = state.budget.toFloatOrNull()
        val error = when (step) {
            0 -> when {
                state.title.isBlank() -> "Introdu denumirea proiectului."
                state.address.isBlank() -> "Introdu adresa proiectului."
                budget == null || budget <= 0f -> "Valoarea ofertei trebuie sa fie un numar pozitiv."
                state.startDateMillis == null -> "Alege data de start."
                state.endDateMillis == null -> "Alege termenul de finalizare."
                state.endDateMillis < state.startDateMillis ->
                    "Termenul de finalizare trebuie sa fie dupa data de start."
                else -> null
            }
            1 -> when {
                !state.withoutZones && state.zones.isEmpty() -> "Adauga cel putin o zona sau alege «Fara zone»."
                else -> null
            }
            2 -> validateWorks(state)
            else -> null
        }
        _uiState.update { it.copy(errorMessage = error) }
        return error == null
    }

    private fun validateWorks(state: AddProjectUiState): String? {
        if (state.requiredLucrari.isEmpty()) return "Adauga cel putin o lucrare."
        if (state.requiredLucrari.any { it.lucrareId == null }) return "Alege tipul pentru fiecare lucrare."
        if (state.requiredLucrari.any { (it.quantity.toFloatOrNull() ?: 0f) <= 0f }) {
            return "Completeaza o suprafata valida pentru fiecare lucrare."
        }
        if (!state.withoutZones && state.requiredLucrari.any { it.zoneKey == null }) {
            return "Alege zona pentru fiecare lucrare."
        }
        val duplicate = state.requiredLucrari
            .groupBy { (it.zoneKey ?: -1L) to it.lucrareId }
            .values
            .any { it.size > 1 }
        if (duplicate) return "Aceeasi lucrare a fost adaugata de doua ori in aceeasi zona."
        return null
    }

    fun submit() {
        val state = _uiState.value
        val companyId = sessionStore.state.value.user?.companyId
        val budget = state.budget.toFloatOrNull()

        val error = when {
            companyId == null -> "Nu am putut identifica firma. Reautentifica-te."
            !validateStep(0) -> _uiState.value.errorMessage
            !validateStep(1) -> _uiState.value.errorMessage
            !validateStep(2) -> _uiState.value.errorMessage
            else -> null
        }

        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val startInstant = Instant.fromEpochMilliseconds(state.startDateMillis!!)

        viewModelScope.launch {
            var createdProject: Project? = null
            runCatching {
                val salaryCost = state.availableEmployees
                    .filter { it.idUser in state.selectedEmployeeIds }
                    .sumOf { it.salary ?: 0.0 }
                    .toFloat()
                val project = projectRepository.createProject(
                    ProjectInsert(
                        title = state.title.trim(),
                        adress = state.address.trim(),
                        budget = budget!!,
                        totalSalaryPerMonth = salaryCost,
                        startDate = startInstant,
                        endDate = Instant.fromEpochMilliseconds(state.endDateMillis!!)
                            .toLocalDateTime(TimeZone.UTC).date,
                        companyId = companyId!!,
                        isOffer = state.isOffer,
                    )
                ).getOrThrow()
                createdProject = project

                val zoneDrafts = if (state.withoutZones) {
                    listOf(OfferZoneDraft(key = IMPLICIT_ZONE_KEY, name = "_implicit"))
                } else {
                    state.zones
                }
                val zoneIds = mutableMapOf<Long, Long>()
                zoneDrafts.forEach { zoneDraft ->
                    val totalSurface = state.requiredLucrari
                        .filter { state.withoutZones || it.zoneKey == zoneDraft.key }
                        .sumOf { (it.quantity.toFloatOrNull() ?: 0f).toDouble() }
                        .toFloat()
                    val zone = zoneRepository.createZone(
                        ZoneInsert(
                            projectId = project.projectId,
                            name = zoneDraft.name,
                            surface = totalSurface,
                            isImplicit = state.withoutZones,
                        )
                    ).getOrThrow()
                    zoneIds[zoneDraft.key] = zone.id
                }

                state.requiredLucrari.forEach { work ->
                    val draftKey = if (state.withoutZones) IMPLICIT_ZONE_KEY else work.zoneKey!!
                    zoneHistoryRepository.add(
                        ZoneHistoryInsert(
                            zoneId = zoneIds.getValue(draftKey),
                            lucrareId = work.lucrareId!!,
                            totalQuantity = work.quantity.toFloat(),
                        )
                    ).getOrThrow()
                }

                state.selectedEmployeeIds.forEach { userId ->
                    userProjectRepository.assignUser(userId, project.projectId).getOrThrow()
                }
                project
            }.onSuccess { project ->
                _uiState.update { it.copy(isLoading = false, createdProject = project) }
            }.onFailure { errorCause ->
                createdProject?.let { partial ->
                    projectRepository.deleteProject(partial.projectId)
                        .onFailure { cleanupError -> Log.e(TAG, "Oferta partiala nu a putut fi stearsa", cleanupError) }
                }
                Log.e(TAG, "Crearea ofertei a esuat", errorCause)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = userFacingError(errorCause))
                }
            }
        }
    }

    private fun userFacingError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            "row-level security" in message.lowercase() ->
                "Nu ai permisiunea necesara pentru a salva aceasta oferta."
            "duplicate key" in message.lowercase() ->
                "Exista deja o inregistrare identica. Verifica zonele si lucrarile."
            else -> "Oferta nu a putut fi salvata. Verifica datele si incearca din nou."
        }
    }

    fun consumeCreatedProject() {
        _uiState.update { it.copy(createdProject = null) }
    }

    companion object {
        private const val TAG = "AddProjectVM"
        private const val IMPLICIT_ZONE_KEY = -1L
    }
}
