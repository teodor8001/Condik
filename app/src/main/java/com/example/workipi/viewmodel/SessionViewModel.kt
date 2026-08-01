package com.example.workipi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.toUser
import com.example.workipi.navigation.Screen
import com.example.workipi.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decide la pornire daca exista o sesiune salvata (login persistent) si gestioneaza logout-ul.
 * Scop activitate — aceeasi instanta in MainActivity (gate) si in drawer (logout).
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    sealed interface StartupState {
        data object Loading : StartupState
        data class Ready(val startRoute: String) : StartupState
    }

    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = runCatching { authRepository.restoreSession() }.getOrNull()
            _state.value = if (user != null) {
                MockSession.currentUser = user.toUser()
                // Daca angajatul inca nu si-a schimbat parola initiala, il ducem direct la schimbare.
                val route = if (user.needsPasswordChange) Screen.ChangePassword.route else Screen.Home.route
                StartupState.Ready(route)
            } else {
                StartupState.Ready(Screen.Login.route)
            }
        }
    }

    /** Iese din cont: sterge sesiunea Supabase si sesiunea locala, apoi anunta UI-ul. */
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
            MockSession.currentUser = null
            onComplete()
        }
    }
}
