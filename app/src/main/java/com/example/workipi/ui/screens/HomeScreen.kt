package com.example.workipi.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.workipi.data.model.UserRole
import com.example.workipi.ui.screens.home.AdminHomeScreen
import com.example.workipi.ui.screens.home.EmployeeHomeScreen
import com.example.workipi.ui.session.LocalSessionState

@Composable
fun HomeScreen(navController: NavController) {
    val user = LocalSessionState.current.user ?: return

    when (user.role) {
        UserRole.ADMIN,
        UserRole.MANAGER,
        UserRole.INGINER -> AdminHomeScreen(navController)
        UserRole.SEF_ECHIPA,
        UserRole.ANGAJAT,
        UserRole.CLIENT          -> EmployeeHomeScreen(navController)
    }
}
