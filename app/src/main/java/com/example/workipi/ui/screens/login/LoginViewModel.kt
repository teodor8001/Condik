package com.example.workipi.ui.screens.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.model.User
import com.example.workipi.repository.AuthRepository
import com.example.workipi.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signedInUser: User? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Introdu email-ul.") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Introdu parola.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signIn(state.email, state.password)
                .mapCatching { utilizator ->
                    sessionStore.open(utilizator, authRepository.getCurrentPermissions())
                    utilizator
                }
                .onSuccess { utilizator ->
                    Log.d(TAG, "signIn reusit: idUtilizator=${utilizator.idUser}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            signedInUser = utilizator,
                        )
                    }
                }
                .onFailure { e ->
                    runCatching { authRepository.signOut() }
                    sessionStore.clear()
                    Log.e(TAG, "signIn esuat", e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Autentificare esuata. Verifica email-ul si parola.",
                        )
                    }
                }
        }
    }

    fun consumeSignedInUser() {
        _uiState.update { it.copy(signedInUser = null) }
    }

    companion object {
        private const val TAG = "LoginVM"
    }
}
