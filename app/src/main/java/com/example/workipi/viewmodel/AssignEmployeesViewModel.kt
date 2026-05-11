package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.User
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.UserProjectRepository
import com.example.workipi.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssignEmployeesUiState(
    val employees: List<User> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val initialSelectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class AssignEmployeesViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userProjectRepository: UserProjectRepository,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssignEmployeesUiState())
    val uiState: StateFlow<AssignEmployeesUiState> = _uiState.asStateFlow()

    fun load(projectId: Long) {
        val companyId = MockSession.currentUser?.idCompany
        if (companyId == null) {
            _uiState.update { it.copy(errorMessage = "Nu am putut identifica firma. Reautentifica-te.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val employeesResult = userRepository.getEmployeesByCompanyId(companyId)
            val assignmentsResult = userProjectRepository.getAssignmentsForProject(projectId)

            val employees = employeesResult.getOrNull()
            val assignments = assignmentsResult.getOrNull()

            if (employees == null || assignments == null) {
                val err = employeesResult.exceptionOrNull() ?: assignmentsResult.exceptionOrNull()
                Log.e(TAG, "Eroare la incarcare", err)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Nu am putut incarca datele. Incearca din nou.",
                    )
                }
                return@launch
            }

            val assigned = assignments.map { it.userId }.toSet()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    employees = employees,
                    selectedIds = assigned,
                    initialSelectedIds = assigned,
                )
            }
        }
    }

    fun toggleEmployee(userId: Long) {
        _uiState.update { state ->
            val newSelected = if (userId in state.selectedIds)
                state.selectedIds - userId
            else
                state.selectedIds + userId
            state.copy(selectedIds = newSelected, errorMessage = null)
        }
    }

    fun save(projectId: Long) {
        val state = _uiState.value
        val toAdd = state.selectedIds - state.initialSelectedIds
        val toRemove = state.initialSelectedIds - state.selectedIds

        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            _uiState.update { it.copy(saved = true) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                toAdd.forEach { userId ->
                    userProjectRepository.assignUser(userId, projectId).getOrThrow()
                }
                toRemove.forEach { userId ->
                    userProjectRepository.unassignUser(userId, projectId).getOrThrow()
                }

                val newSalaryCost = state.employees
                    .filter { it.idUser in state.selectedIds }
                    .sumOf { it.salary ?: 0.0 }
                    .toFloat()

                projectRepository.updateSalaryCost(projectId, newSalaryCost).getOrThrow()

                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Throwable) {
                Log.e(TAG, "Eroare la salvarea echipei", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Salvarea a esuat. Incearca din nou.",
                    )
                }
            }
        }
    }

    fun consumeSaved() {
        _uiState.update { it.copy(saved = false) }
    }

    companion object {
        private const val TAG = "AssignEmployeesVM"
    }
}
