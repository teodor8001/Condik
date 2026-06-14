package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.InvitationCode
import com.example.workipi.data.model.User
import com.example.workipi.repository.InvitationCodeRepository
import com.example.workipi.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

data class AngajatiUiState(
    val employees: List<User> = emptyList(),
    val pendingInvites: List<InvitationCode> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class AngajatiViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val invitationCodeRepository: InvitationCodeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AngajatiUiState())
    val uiState: StateFlow<AngajatiUiState> = _uiState.asStateFlow()

    init {
        loadEmployees()
    }

    fun loadEmployees() {
        val companyId = MockSession.currentUser?.idCompany
        if (companyId == null) {
            _uiState.update { it.copy(errorMessage = "Nu am putut identifica firma. Reautentifica-te.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val employeesResult = userRepository.getEmployeesByCompanyId(companyId)
            // Invitatii inca neactivati — apar in lista chiar daca nu si-au folosit codul.
            val pendingInvites = invitationCodeRepository.getUnusedByCompany(companyId)
                .getOrDefault(emptyList())
                .filter { it.expirationDate > Clock.System.now() }

            employeesResult
                .onSuccess { list ->
                    // Lista de angajati nu include adminii.
                    val employees = list.filter { it.role?.equals("admin", ignoreCase = true) != true }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            employees = employees,
                            pendingInvites = pendingInvites,
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Eroare la incarcarea angajatilor", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Nu am putut incarca angajatii.",
                        )
                    }
                }
        }
    }

    companion object {
        private const val TAG = "AngajatiVM"
    }
}
