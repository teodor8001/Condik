package com.example.workipi.session

import com.example.workipi.data.model.AppPermission
import com.example.workipi.data.model.User
import com.example.workipi.data.model.UserRole
import com.example.workipi.data.model.userRoleFromDbValue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SessionUser(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val companyId: Long?,
    val needsPasswordChange: Boolean,
)

data class SessionState(
    val user: SessionUser? = null,
    val permissions: Set<AppPermission> = emptySet(),
) {
    val isAuthenticated: Boolean get() = user != null

    fun hasPermission(permission: AppPermission): Boolean = permission in permissions
}

@Singleton
class SessionStore @Inject constructor() {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun open(profile: User, permissions: Set<AppPermission>) {
        _state.value = SessionState(
            user = SessionUser(
                id = profile.idUser,
                name = profile.fullName,
                email = profile.email,
                phone = profile.phoneNumber.toString(),
                role = userRoleFromDbValue(profile.role),
                companyId = profile.idCompany,
                needsPasswordChange = profile.needsPasswordChange,
            ),
            permissions = permissions,
        )
    }

    fun markPasswordChanged() {
        _state.update { current ->
            current.copy(user = current.user?.copy(needsPasswordChange = false))
        }
    }

    fun clear() {
        _state.value = SessionState()
    }
}
