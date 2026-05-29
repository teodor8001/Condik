package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.model.InvitationCodeInsert
import com.example.workipi.data.model.InvitationCodeLucrareInsert
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.SkillLevel
import com.example.workipi.data.model.UserRole
import com.example.workipi.data.model.toDbValue
import com.example.workipi.repository.AuthRepository
import com.example.workipi.repository.InvitationCodeLucrareRepository
import com.example.workipi.repository.InvitationCodeRepository
import com.example.workipi.repository.SkillRepository
import com.example.workipi.util.InvitationCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

data class SkillSelection(
    val lucrare: Lucrare,
    val level: SkillLevel,
)

data class InvitationCodeUiState(
    val isLoading: Boolean = false,
    val generatedCode: String? = null,
    val errorMessage: String? = null,
    val availableSkills: List<Lucrare> = emptyList(),
    val selectedSkills: Map<Long, SkillLevel> = emptyMap(),
)

@HiltViewModel
class InvitationCodeViewModel @Inject constructor(
    private val invitationCodeRepository: InvitationCodeRepository,
    private val authRepository: AuthRepository,
    private val skillRepository: SkillRepository,
    private val invitationCodeLucrareRepository: InvitationCodeLucrareRepository,
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

    fun generateInvitationCode(
        fullName: String,
        email: String,
        phoneNumber: String,
        role: UserRole,
        salary: Float?,
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val code = InvitationCodeGenerator.generate()
                val companyId = authRepository.getCompanyIdFromAuthUser()
                val expirationDate = Clock.System.now() + 24.hours
                val insert = InvitationCodeInsert(
                    code = code,
                    companyId = companyId,
                    role = role.toDbValue(),
                    email = email,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    expirationDate = expirationDate,
                    salary = salary,
                )

                val createdCode = invitationCodeRepository.generateInvitationCode(insert)

                val skillRows = _uiState.value.selectedSkills.map { (idLucrare, lvl) ->
                    InvitationCodeLucrareInsert(
                        codeId = createdCode.id,
                        idLucrare = idLucrare,
                        skillLevel = lvl.dbValue,
                    )
                }
                invitationCodeLucrareRepository.assignSkillsToCode(skillRows)
                    .onFailure { e -> Log.e(TAG, "Skills nu s-au atasat la cod (continuam)", e) }

                Log.d(TAG, "Cod invitatie generat: $code")
                _uiState.update { it.copy(isLoading = false, generatedCode = code) }
            } catch (e: Throwable) {
                Log.e(TAG, "Generare cod invitatie esuata", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Generarea codului a esuat. Incearca din nou.",
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
        private const val TAG = "InvitationCodeVM"
    }
}
