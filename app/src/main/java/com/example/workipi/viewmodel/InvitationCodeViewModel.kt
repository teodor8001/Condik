package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.model.InvitationCodeInsert
import com.example.workipi.data.model.UserRole
import com.example.workipi.data.model.toDbValue
import com.example.workipi.repository.AuthRepository
import com.example.workipi.repository.InvitationCodeRepository
import com.example.workipi.repository.UserRepository
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

data class InvitationCodeUiState(
    val isLoading: Boolean = false,
    val generatedCode: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class InvitationCodeViewModel @Inject constructor(
    private val invitationCodeRepository: InvitationCodeRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvitationCodeUiState())
    val uiState: StateFlow<InvitationCodeUiState> = _uiState.asStateFlow()

    fun generateInvitationCode(fullName: String, email: String, phoneNumber: String, role: UserRole) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val code = InvitationCodeGenerator.generate()
                val companyId = authRepository.getCompanyIdFromAuthUser()
                val expirationDate = Clock.System.now() + 24.hours
                val invitationCodeInsert = InvitationCodeInsert(
                    code = code,
                    companyId = companyId,
                    role = role.toDbValue(),
                    email = email,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    expirationDate = expirationDate,
                )

                invitationCodeRepository.generateInvitationCode(invitationCodeInsert)

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
        _uiState.value = InvitationCodeUiState()
    }

    companion object {
        private const val TAG = "InvitationCodeVM"
    }
}