package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.ProjectInsert
import com.example.workipi.data.model.User
import com.example.workipi.data.model.ZoneInsert
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class AddProjectUiState(
    val title: String = "",
    val address: String = "",
    val budget: String = "",
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val isOffer: Boolean = false,
    val availableEmployees: List<User> = emptyList(),
    val selectedEmployeeIds: Set<Long> = emptySet(),
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProjectUiState())
    val uiState: StateFlow<AddProjectUiState> = _uiState.asStateFlow()

    init {
        loadEmployees()
    }

    private fun loadEmployees() {
        val companyId = MockSession.currentUser?.idCompany ?: return
        viewModelScope.launch {
            userRepository.getEmployeesByCompanyId(companyId)
                .onSuccess { list ->
                    _uiState.update { it.copy(availableEmployees = list) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la incarcare angajati", e)
                }
        }
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

    fun submit() {
        val state = _uiState.value
        val companyId = MockSession.currentUser?.idCompany
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
                    // Zonele se adauga ulterior din ecranul de detaliu. La creare punem
                    // doar zona implicita (necesara in DB), pe care se vor atasa lucrarile
                    // pana cand userul adauga zone reale.
                    zoneRepository.createZones(
                        listOf(
                            ZoneInsert(
                                projectId = project.projectId,
                                name = "_implicit",
                                surface = 0f,
                                isImplicit = true,
                            )
                        )
                    ).onFailure { e -> Log.e(TAG, "Zona implicita nu s-a creat (continuam)", e) }

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
