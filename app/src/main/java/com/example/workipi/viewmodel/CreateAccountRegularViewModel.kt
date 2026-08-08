package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.model.InvitationCode
import com.example.workipi.data.model.User
import com.example.workipi.repository.AuthRepository
import com.example.workipi.repository.InvitationCodeRepository
import com.example.workipi.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class CreateRegularAccountUiState(
    val invitationCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isValidCode: Boolean? = null,
    val createdUser: User? = null,
    val passwordVisible: Boolean = false,
)

@HiltViewModel
class CreateAccountRegularViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val codeRepository: InvitationCodeRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateRegularAccountUiState())
    val uiState: StateFlow<CreateRegularAccountUiState> = _uiState.asStateFlow()

    fun onInvitationCodeChange(value: String) {
        _uiState.update { it.copy(invitationCode = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible)}
    }

    suspend fun getCodeIfValid(code: String): Result<InvitationCode> = runCatching {
        val invitationCode = codeRepository
            .getCodeByName(code)

        if (invitationCode == null) {
            error("Invitatia este invalida")
        }
        val isInvitationValid = invitationCode.let {
            !(it.isUsed || it.expirationDate < Clock.System.now())
        }

        if (isInvitationValid) {
            invitationCode
        } else {
            error("Invitatia a expirat sau a fost folosita")
        }
    }
    fun submitRegularAccount() {
        Log.d(::submitRegularAccount.toString(), "s-a facut submit-ul")
        val state = _uiState.value
        val error = when {
            state.password.isBlank() -> "Introdu parola"
            state.confirmPassword.isBlank() -> "Nu ai confirmat parola"
            else -> null
        }

        error?.let { errorMessage ->
            _uiState.update {
                it.copy(errorMessage = errorMessage)
            }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val invitationCode: InvitationCode? = getCodeIfValid(state.invitationCode)
                .onSuccess {
                    Log.d(TAG, "Codul este valid")
                }
                .onFailure {
                    Log.e(TAG, "Codul nu este valid")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Codul nu este valid"
                        )
                    }
                }
                .getOrNull()

            if (invitationCode == null) {
                return@launch
            }

            authRepository.signUpEmployee(
                invitationCode = invitationCode.code,
                email = invitationCode.email,
                password = state.password,
            )
                .mapCatching { utilizator ->
                    sessionStore.open(utilizator, authRepository.getCurrentPermissions())
                    utilizator
                }
                .onSuccess { utilizator ->
                    Log.d(TAG, "Userul ${invitationCode.fullName} este creat cu success")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            createdUser = utilizator,
                        )
                    }
                }
                .onFailure { error ->
                    runCatching { authRepository.signOut() }
                    sessionStore.clear()
                    Log.d(TAG, "Userul nu s-a putut crea cu succes")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Userul nu s-a putut crea cu success"
                        )
                    }
                }
        }
    }

    fun consumeCreatedUser() {
        _uiState.update { it.copy(createdUser = null) }
    }
    companion object {
        private const val TAG = "CreateAccountRegularViewModel"
    }
}

