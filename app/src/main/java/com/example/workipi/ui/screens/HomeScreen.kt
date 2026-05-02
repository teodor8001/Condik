package com.example.workipi.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.UserRole
import com.example.workipi.ui.screens.home.AdminHomeScreen
import com.example.workipi.ui.screens.home.EmployeeHomeScreen

@Composable
fun HomeScreen(navController: NavController) {
    val user = MockSession.currentUser ?: return

    when (user.role) {
        UserRole.ADMIN,
        UserRole.PROJECT_MANAGER -> AdminHomeScreen(navController)
        UserRole.ANGAJAT,
        UserRole.CLIENT          -> EmployeeHomeScreen(navController)
    }
}