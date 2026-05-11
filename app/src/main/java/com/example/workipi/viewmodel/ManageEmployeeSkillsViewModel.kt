package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.SkillLevel
import com.example.workipi.data.model.UtilizatorLucrareInsert
import com.example.workipi.data.model.toSkillLevel
import com.example.workipi.repository.LucrareRepository
import com.example.workipi.repository.UtilizatorLucrareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageEmployeeSkillsUiState(
    val available: List<Lucrare> = emptyList(),
    val current: Map<Long, SkillLevel> = emptyMap(),       // editat de user
    val initial: Map<Long, SkillLevel> = emptyMap(),       // ce era in DB la load
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class ManageEmployeeSkillsViewModel @Inject constructor(
    private val utilizatorLucrareRepository: UtilizatorLucrareRepository,
    private val lucrareRepository: LucrareRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageEmployeeSkillsUiState())
    val uiState: StateFlow<ManageEmployeeSkillsUiState> = _uiState.asStateFlow()

    fun load(userId: Long) {
        val companyId = MockSession.currentUser?.idCompany
        if (companyId == null) {
            _uiState.update { it.copy(errorMessage = "Nu am putut identifica firma.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val available = lucrareRepository.getSkillsForCompany(companyId).getOrNull()
            val userSkills = utilizatorLucrareRepository.getSkillsForUser(userId).getOrNull()
            if (available == null || userSkills == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Nu am putut incarca datele.",
                    )
                }
                return@launch
            }
            val initial = userSkills.associate { it.idLucrare to it.skillLevel.toSkillLevel() }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    available = available,
                    current = initial,
                    initial = initial,
                )
            }
        }
    }

    fun toggleSkill(idLucrare: Long) {
        _uiState.update { state ->
            val newCurrent = state.current.toMutableMap()
            if (idLucrare in newCurrent) newCurrent.remove(idLucrare)
            else newCurrent[idLucrare] = SkillLevel.JUNIOR
            state.copy(current = newCurrent, errorMessage = null)
        }
    }

    fun setLevel(idLucrare: Long, level: SkillLevel) {
        _uiState.update { state ->
            if (idLucrare !in state.current) state
            else state.copy(current = state.current + (idLucrare to level))
        }
    }

    fun save(userId: Long) {
        val state = _uiState.value
        val toAdd = state.current.keys - state.initial.keys
        val toRemove = state.initial.keys - state.current.keys
        val toUpdate = state.current.keys.intersect(state.initial.keys).filter {
            state.current[it] != state.initial[it]
        }

        if (toAdd.isEmpty() && toRemove.isEmpty() && toUpdate.isEmpty()) {
            _uiState.update { it.copy(saved = true) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                if (toAdd.isNotEmpty()) {
                    val rows = toAdd.map {
                        UtilizatorLucrareInsert(
                            userId = userId,
                            idLucrare = it,
                            skillLevel = state.current[it]!!.dbValue,
                        )
                    }
                    utilizatorLucrareRepository.assignSkills(rows).getOrThrow()
                }
                toRemove.forEach { id ->
                    utilizatorLucrareRepository.removeSkill(userId, id).getOrThrow()
                }
                toUpdate.forEach { id ->
                    utilizatorLucrareRepository
                        .updateLevel(userId, id, state.current[id]!!.dbValue)
                        .getOrThrow()
                }

                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Throwable) {
                Log.e(TAG, "Eroare la salvarea skills-urilor", e)
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
        private const val TAG = "ManageEmpSkillsVM"
    }
}
