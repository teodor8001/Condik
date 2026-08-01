package com.example.workipi.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.InvitationCode
import com.example.workipi.data.model.User
import com.example.workipi.repository.InvitationCodeRepository
import com.example.workipi.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
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
    @ApplicationContext private val context: Context,
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
            // Reset automat al check-in-urilor la inceputul zilei de lucru (4 AM ora Romaniei).
            maybeResetCheckIns(companyId)
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

    fun setCheckedIn(userId: Long, value: Boolean) {
        viewModelScope.launch {
            userRepository.setCheckedIn(userId, value)
                .onSuccess {
                    _uiState.update { st ->
                        st.copy(employees = st.employees.map { if (it.idUser == userId) it.copy(isCheckedIn = value) else it })
                    }
                }
                .onFailure { e -> Log.e(TAG, "Check-in esuat", e) }
        }
    }

    /**
     * Daca a trecut un nou inceput de zi de lucru (4 AM ora Romaniei) de la ultimul reset,
     * deselecteaza toate check-in-urile, ca sa fie pregatite pentru ziua urmatoare.
     */
    private suspend fun maybeResetCheckIns(companyId: Long) {
        val tz = TimeZone.of("Europe/Bucharest")
        val now = Clock.System.now().toLocalDateTime(tz)
        // Inainte de 4 AM apartinem inca zilei de lucru precedente.
        val workDay = if (now.hour < 4) now.date.minus(1, DateTimeUnit.DAY) else now.date
        val key = workDay.toString()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_RESET, null) != key) {
            userRepository.resetAllCheckIns(companyId)
                .onFailure { e -> Log.e(TAG, "Reset check-in esuat", e) }
            prefs.edit().putString(KEY_LAST_RESET, key).apply()
        }
    }

    companion object {
        private const val TAG = "AngajatiVM"
        private const val PREFS = "checkin_prefs"
        private const val KEY_LAST_RESET = "last_reset_workday"
    }
}
