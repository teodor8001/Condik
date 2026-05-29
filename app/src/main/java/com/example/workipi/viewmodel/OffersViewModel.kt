package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.Project
import com.example.workipi.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OffersUiState(
    val offers: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OffersUiState())
    val uiState: StateFlow<OffersUiState> = _uiState.asStateFlow()

    init { loadOffers() }

    fun loadOffers() {
        val companyId = MockSession.currentUser?.idCompany
        if (companyId == null) {
            _uiState.update { it.copy(errorMessage = "Nu am putut identifica firma.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val offers = projectRepository.getOffersByCompanyId(companyId)
                _uiState.update { it.copy(isLoading = false, offers = offers) }
            } catch (e: Throwable) {
                Log.e(TAG, "Eroare la incarcarea ofertelor", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Nu am putut incarca ofertele.",
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "OffersVM"
    }
}
