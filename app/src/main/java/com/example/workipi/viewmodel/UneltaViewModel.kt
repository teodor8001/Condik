package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.Unealta
import com.example.workipi.data.model.UnealtaInsert
import com.example.workipi.repository.UneltaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UneltaUiState(
    val tools: List<Unealta> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class UneltaViewModel @Inject constructor(
    private val repository: UneltaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UneltaUiState())
    val uiState: StateFlow<UneltaUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        val companyId = MockSession.currentUser?.idCompany ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getByCompany(companyId)
                .onSuccess { list -> _uiState.update { it.copy(isLoading = false, tools = list.sortedBy { t -> t.name }) } }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la incarcarea uneltelor", e)
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Nu am putut incarca uneltele.") }
                }
        }
    }

    fun addTool(name: String, totalQuantity: Int) {
        val companyId = MockSession.currentUser?.idCompany ?: return
        if (name.isBlank() || totalQuantity <= 0) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            repository.add(
                UnealtaInsert(
                    companyId = companyId,
                    name = name.trim(),
                    totalQuantity = totalQuantity,
                    availableQuantity = totalQuantity,
                )
            )
                .onSuccess { _uiState.update { it.copy(isSaving = false) }; load() }
                .onFailure { e ->
                    Log.e(TAG, "Adaugare unealta esuata", e)
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Adaugarea a esuat.") }
                }
        }
    }

    fun deleteTool(id: Long) {
        viewModelScope.launch {
            repository.remove(id)
                .onSuccess { load() }
                .onFailure { e -> Log.e(TAG, "Stergere unealta esuata", e) }
        }
    }

    companion object {
        private const val TAG = "UneltaVM"
    }
}
