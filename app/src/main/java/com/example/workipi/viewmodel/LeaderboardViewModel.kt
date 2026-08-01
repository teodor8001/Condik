package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.session.SessionStore
import com.example.workipi.data.model.User
import com.example.workipi.data.model.UserRole
import com.example.workipi.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val totalEmployees: Int = 0,
    val visible: List<User> = emptyList(),
    val isRestricted: Boolean = false,
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    fun load() {
        val currentUser = sessionStore.state.value.user
        val companyId = currentUser?.companyId
        if (companyId == null) {
            _uiState.update { it.copy(errorMessage = "Nu am putut identifica firma.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            userRepository.getEmployeesByCompanyId(companyId)
                .onSuccess { employees ->
                    // Topul este al angajatilor — fara admini.
                    val sorted = employees
                        .filter { it.role?.equals("admin", ignoreCase = true) != true }
                        .sortedByDescending { it.points ?: 0.0 }
                    val isAngajat = currentUser.role == UserRole.ANGAJAT
                    val visible = if (isAngajat) {
                        val half = ceil(sorted.size / 2.0).toInt().coerceAtLeast(1)
                        sorted.take(half)
                    } else sorted
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalEmployees = sorted.size,
                            visible = visible,
                            isRestricted = isAngajat,
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la incarcarea topului", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Nu am putut incarca topul.",
                        )
                    }
                }
        }
    }

    companion object {
        private const val TAG = "LeaderboardVM"
    }
}
