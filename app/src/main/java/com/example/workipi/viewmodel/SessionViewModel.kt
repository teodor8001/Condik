package com.example.workipi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.navigation.Screen
import com.example.workipi.repository.AuthRepository
import com.example.workipi.session.SessionState
import com.example.workipi.session.SessionStore
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
    private val sessionStore: SessionStore,
) : ViewModel() {

    sealed interface StartupState {
        data object Loading : StartupState
        data class Ready(val startRoute: String) : StartupState
    }

    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()
    val sessionState: StateFlow<SessionState> = sessionStore.state

    init {
        viewModelScope.launch {
            val restored = runCatching {
                val user = authRepository.restoreSession() ?: return@runCatching null
                val permissions = authRepository.getCurrentPermissions()
                sessionStore.open(user, permissions)
                user
            }
            val user = restored.getOrNull()
            if (restored.isFailure) {
                runCatching { authRepository.signOut() }
                sessionStore.clear()
            }
            val route = when {
                user == null -> Screen.Login.route
                user.needsPasswordChange -> Screen.ChangePassword.route
                else -> Screen.Home.route
            }
            _state.value = StartupState.Ready(route)
        }
    }

    /** Iese din cont: sterge sesiunea Supabase si sesiunea locala, apoi anunta UI-ul. */
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
            sessionStore.clear()
            onComplete()
        }
    }
}
