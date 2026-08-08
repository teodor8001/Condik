package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.session.SessionStore
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.LucrareInsert
import com.example.workipi.repository.SkillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreturiUiState(
    val skills: List<Lucrare> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class PreturiViewModel @Inject constructor(
    private val skillRepository: SkillRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreturiUiState())
    val uiState: StateFlow<PreturiUiState> = _uiState.asStateFlow()

    init {
        loadSkills()
    }

    fun loadSkills() {
        val companyId = sessionStore.state.value.user?.companyId
        if (companyId == null) {
            _uiState.update { it.copy(errorMessage = "Nu am putut identifica firma. Reautentifica-te.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            skillRepository.getSkillsForCompany(companyId)
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, skills = list.sortedBy { s -> s.name }) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la incarcarea lucrarilor", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Nu am putut incarca lucrarile.",
                        )
                    }
                }
        }
    }

    fun createSkill(name: String, unit: String, price: Float, points: Long) {
        val companyId = sessionStore.state.value.user?.companyId ?: return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            skillRepository.createSkill(
                LucrareInsert(
                    name = name.trim(),
                    unit = unit,
                    price = price,
                    points = points,
                    idFirma = companyId,
                )
            )
                .onSuccess { created ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            skills = (state.skills + created).sortedBy { it.name },
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la creare lucrare", e)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = e.message ?: "Crearea a esuat. Incearca din nou.",
                        )
                    }
                }
        }
    }

    fun updateSkill(id: Long, name: String, unit: String, price: Float, points: Long) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            skillRepository.updateSkill(id, name.trim(), unit, price, points)
                .onSuccess {
                    _uiState.update { state ->
                        val updated = state.skills.map { s ->
                            if (s.id == id) s.copy(name = name.trim(), unit = unit, price = price, points = points)
                            else s
                        }.sortedBy { it.name }
                        state.copy(isSaving = false, skills = updated)
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la update lucrare", e)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = e.message ?: "Salvarea a esuat. Incearca din nou.",
                        )
                    }
                }
        }
    }

    fun deleteSkill(id: Long) {
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            skillRepository.deleteSkill(id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            skills = state.skills.filterNot { it.id == id },
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la stergere lucrare", e)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = e.message ?: "Stergerea a esuat.",
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val TAG = "PreturiVM"
    }
}
