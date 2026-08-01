package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.repository.AuthRepository
import com.example.workipi.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val done: Boolean = false,
)

/**
 * Prima logare a unui angajat creat de admin: i se cere sa-si schimbe parola initiala. Dupa schimbare,
 * contul nu mai e "in asteptare" (necesita_schimbare_parola = false).
 */
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit() {
        val state = _uiState.value
        val error = when {
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
            authRepository.changeOwnPassword(state.password)
                .onSuccess {
                    sessionStore.markPasswordChanged()
                    _uiState.update { it.copy(isLoading = false, done = true) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Schimbarea parolei a esuat", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Schimbarea parolei a esuat. Incearca din nou.",
                        )
                    }
                }
        }
    }

    fun consumeDone() = _uiState.update { it.copy(done = false) }

    companion object {
        private const val TAG = "ChangePasswordVM"
    }
}
