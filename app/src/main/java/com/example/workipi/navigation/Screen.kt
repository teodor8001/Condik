package com.example.workipi.navigation

sealed class Screen(val route: String) {
    object Login           : Screen("login")
    object CreateCompany   : Screen("create_company")
    object ActivateAccount : Screen("activate_account")
    object Home           : Screen("home")
    object Proiecte       : Screen("proiecte")
    object AddProject     : Screen("add_project?offer={offer}") {
        fun createRoute(offer: Boolean = false) = "add_project?offer=$offer"
    }
    object ProjectDetail  : Screen("project/{projectId}") {
        fun createRoute(projectId: Long) = "project/$projectId"
    }
    object AssignEmployees : Screen("project/{projectId}/assign") {
        fun createRoute(projectId: Long) = "project/$projectId/assign"
    }
    object PontareEntry : Screen("project/{projectId}/pontare/{userId}") {
        fun createRoute(projectId: Long, userId: Long) = "project/$projectId/pontare/$userId"
    }
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
    object Firma          : Screen("firma")
    object Ofertare       : Screen("ofertare")
    object Settings       : Screen("settings")
}