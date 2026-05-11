package com.example.workipi.navigation

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object CreateAccount  : Screen("create_account")
    object Home           : Screen("home")
    object Proiecte       : Screen("proiecte")
    object AddProject     : Screen("add_project")
    object ProjectDetail  : Screen("project/{projectId}") {
        fun createRoute(projectId: Long) = "project/$projectId"
    }
    object AssignEmployees : Screen("project/{projectId}/assign") {
        fun createRoute(projectId: Long) = "project/$projectId/assign"
    }
    object PontareEntry : Screen("project/{projectId}/pontare/{userId}") {
        fun createRoute(projectId: Long, userId: Long) = "project/$projectId/pontare/$userId"
    }
    object Pontare        : Screen("pontare")
    object Calitate       : Screen("calitate")
    object Angajati       : Screen("angajati")
    object AddEmployee    : Screen("add_employee")
    object EmployeeDetail : Screen("employee/{employeeId}") {
        fun createRoute(employeeId: Long) = "employee/$employeeId"
    }
    object ManageEmployeeSkills : Screen("employee/{employeeId}/skills") {
        fun createRoute(employeeId: Long) = "employee/$employeeId/skills"
    }
    object Leaderboard    : Screen("leaderboard")
    object Preturi        : Screen("preturi")
    object Settings       : Screen("settings")
}