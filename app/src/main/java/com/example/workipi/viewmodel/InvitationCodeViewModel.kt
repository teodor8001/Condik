package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.SkillLevel
import com.example.workipi.data.model.UserRole
import com.example.workipi.data.model.toDbValue
import com.example.workipi.repository.AuthRepository
import com.example.workipi.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SkillSelection(
    val lucrare: Lucrare,
    val level: SkillLevel,
)

data class InvitationCodeUiState(
    val isLoading: Boolean = false,
    // Numele angajatului dupa ce contul a fost creat cu succes (null = inca pe formular).
    val createdEmployeeName: String? = null,
    val errorMessage: String? = null,
    val availableSkills: List<Lucrare> = emptyList(),
    val selectedSkills: Map<Long, SkillLevel> = emptyMap(),
)

@HiltViewModel
class InvitationCodeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val skillRepository: SkillRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvitationCodeUiState())
    val uiState: StateFlow<InvitationCodeUiState> = _uiState.asStateFlow()

    init {
        loadAvailableSkills()
    }

    private fun loadAvailableSkills() {
        viewModelScope.launch {
            try {
                val companyId = authRepository.getCompanyIdFromAuthUser()
                skillRepository.getSkillsForCompany(companyId)
                    .onSuccess { list -> _uiState.update { it.copy(availableSkills = list) } }
                    .onFailure { e -> Log.e(TAG, "Nu am putut incarca lucrari", e) }
            } catch (e: Throwable) {
                Log.e(TAG, "Nu am putut identifica firma pentru skills", e)
            }
        }
    }

    fun toggleSkill(idLucrare: Long) {
        _uiState.update { state ->
            val newMap = state.selectedSkills.toMutableMap()
            if (idLucrare in newMap) newMap.remove(idLucrare)
            else newMap[idLucrare] = SkillLevel.JUNIOR
            state.copy(selectedSkills = newMap)
        }
    }

    fun setSkillLevel(idLucrare: Long, level: SkillLevel) {
        _uiState.update { state ->
            if (idLucrare !in state.selectedSkills) state
            else state.copy(selectedSkills = state.selectedSkills + (idLucrare to level))
        }
    }

    /**
     * Adminul creeaza complet contul angajatului (email + parola initiala). Angajatul ramane "in
     * asteptare" pana isi schimba parola la prima logare, dar este folosibil imediat in proiecte.
     */
    fun createEmployeeAccount(
        fullName: String,
        email: String,
        phoneNumber: String,
        role: UserRole,
        salary: Float?,
        password: String,
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val companyId = authRepository.getCompanyIdFromAuthUser()
                val skills = _uiState.value.selectedSkills.map { (idLucrare, lvl) ->
                    idLucrare to lvl.dbValue
                }
                authRepository.createEmployeeAccountAsAdmin(
                    fullName = fullName,
                    email = email,
                    phoneNumber = phoneNumber,
                    role = role.toDbValue(),
                    companyId = companyId,
                    password = password,
                    salary = salary,
                    skills = skills,
                )
                    .onSuccess { user ->
                        Log.d(TAG, "Cont angajat creat: ${user.fullName}")
                        _uiState.update { it.copy(isLoading = false, createdEmployeeName = user.fullName) }
                    }
                    .onFailure { e ->
                        Log.e(TAG, "Crearea contului a esuat", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "Crearea contului a esuat. Incearca din nou.",
                            )
                        }
                    }
            } catch (e: Throwable) {
                Log.e(TAG, "Crearea contului a esuat", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Crearea contului a esuat. Incearca din nou.",
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.update {
            InvitationCodeUiState(availableSkills = it.availableSkills)
        }
    }

    companion object {
        private const val TAG = "CreateEmployeeVM"
    }
}
