package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.Pontare
import com.example.workipi.data.model.User
import com.example.workipi.repository.LucrareRepository
import com.example.workipi.repository.PontareRepository
import com.example.workipi.repository.UserRepository
import com.example.workipi.repository.UtilizatorLucrareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmployeeSkill(
    val lucrare: Lucrare,
    val level: String,
)

data class PontareEntry(
    val pontare: Pontare,
    val lucrareName: String,
    val lucrareUnit: String,
    val earnedPoints: Long,
)

data class EmployeeDetailUiState(
    val employee: User? = null,
    val skills: List<EmployeeSkill> = emptyList(),
    val pontari: List<PontareEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class EmployeeDetailViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val utilizatorLucrareRepository: UtilizatorLucrareRepository,
    private val lucrareRepository: LucrareRepository,
    private val pontareRepository: PontareRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeDetailUiState())
    val uiState: StateFlow<EmployeeDetailUiState> = _uiState.asStateFlow()

    fun load(userId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val userResult = userRepository.findById(userId)
            val user = userResult.getOrNull()
            if (user == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Angajatul nu a fost gasit.",
                    )
                }
                return@launch
            }

            val skillRows = utilizatorLucrareRepository
                .getSkillsForUser(userId)
                .getOrDefault(emptyList())

            val catalog: Map<Long, Lucrare> = if (user.idCompany == null) emptyMap()
            else lucrareRepository
                .getSkillsForCompany(user.idCompany)
                .getOrDefault(emptyList())
                .associateBy { it.id }

            val skills = skillRows.mapNotNull { row ->
                val lucrare = catalog[row.idLucrare] ?: return@mapNotNull null
                EmployeeSkill(lucrare = lucrare, level = row.skillLevel)
            }

            val pontari = pontareRepository.getByUser(userId)
                .getOrDefault(emptyList())
                .map { p ->
                    val lucrare = catalog[p.idLucrare]
                    PontareEntry(
                        pontare = p,
                        lucrareName = lucrare?.name ?: "—",
                        lucrareUnit = lucrare?.unit ?: "",
                        earnedPoints = ((lucrare?.points ?: 0L) * p.quantity.toLong()),
                    )
                }

            _uiState.update {
                it.copy(isLoading = false, employee = user, skills = skills, pontari = pontari)
            }
        }.also {
            it.invokeOnCompletion { e ->
                if (e != null) {
                    Log.e(TAG, "Eroare la incarcare angajat", e)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Nu am putut incarca angajatul.",
                        )
                    }
                }
            }
        }
    }

    fun removeEmployee(userId: Long) {
        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            userRepository.deleteEmployee(userId)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, deleted = true) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la stergerea angajatului", e)
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = e.message ?: "Stergerea a esuat. Incearca din nou.",
                        )
                    }
                }
        }
    }

    fun consumeDeleted() {
        _uiState.update { it.copy(deleted = false) }
    }

    companion object {
        private const val TAG = "EmployeeDetailVM"
    }
}
