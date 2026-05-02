package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.model.User
import com.example.workipi.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateAccountUiState(
    val denumireFirma: String = "",
    val numePrenume: String = "",
    val email: String = "",
    val telefon: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdUser: User? = null,
)

@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAccountUiState())
    val uiState: StateFlow<CreateAccountUiState> = _uiState.asStateFlow()

    fun onDenumireFirmaChange(value: String) =
        _uiState.update { it.copy(denumireFirma = value, errorMessage = null) }

    fun onNumePrenumeChange(value: String) =
        _uiState.update { it.copy(numePrenume = value, errorMessage = null) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onTelefonChange(value: String) =
        _uiState.update { it.copy(telefon = value.filter { c -> c.isDigit() }, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }

    fun togglePasswordVisibility() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit() {
        val state = _uiState.value
        val error = when {
            state.denumireFirma.isBlank() -> "Introdu denumirea firmei."
            state.numePrenume.isBlank() -> "Introdu numele si prenumele."
            state.email.isBlank() -> "Introdu email-ul."
            state.telefon.isBlank() -> "Introdu numarul de telefon."
            state.telefon.toLongOrNull() == null -> "Numarul de telefon trebuie sa contina doar cifre."
            state.password.length < 6 -> "Parola trebuie sa aiba cel putin 6 caractere."
            state.password != state.confirmPassword -> "Parolele nu coincid."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signUpAdmin(
                companyName = state.denumireFirma,
                fullName = state.numePrenume,
                email = state.email,
                phoneNumber = state.telefon,
                password = state.password,
            )
                .onSuccess { utilizator ->
                    Log.d(TAG, "signUpAdmin reusit: idUtilizator=${utilizator.idUser}")
                    _uiState.update { it.copy(isLoading = false, createdUser = utilizator) }
                }
                .onFailure { e ->
                    Log.e(TAG, "signUpAdmin esuat", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Crearea contului a esuat. Incearca din nou.",
                        )
                    }
                }
        }
    }

    fun consumeCreatedUser() {
        _uiState.update { it.copy(createdUser = null) }
    }

    companion object {
        private const val TAG = "CreateAccountVM"
    }
}