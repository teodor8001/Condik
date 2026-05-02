package com.example.workipi.navigation

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object CreateAccount  : Screen("create_account")
    object Home           : Screen("home")
    object Proiecte       : Screen("proiecte")
    object ProjectDetail  : Screen("project/{projectId}") {
        fun createRoute(projectId: String) = "project/$projectId"
    }
    object Pontare        : Screen("pontare")
    object Calitate       : Screen("calitate")
    object Angajati       : Screen("angajati")
    object EmployeeDetail : Screen("employee/{employeeId}") {
        fun createRoute(employeeId: String) = "employee/$employeeId"
    }
    object Leaderboard    : Screen("leaderboard")
    object Preturi        : Screen("preturi")
    object Settings       : Screen("settings")
}