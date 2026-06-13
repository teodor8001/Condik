package com.example.workipi.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.workipi.ui.components.AppNavigationDrawer
import com.example.workipi.ui.screens.*

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
) {
    AppNavigationDrawer(navController = navController) {
        NavHost(
            navController    = navController,
            startDestination = startDestination
        ) {
            composable(Screen.Login.route)            { LoginScreen(navController) }
            composable(Screen.CreateCompany.route)    { CreateCompanyScreen(navController) }
            composable(Screen.ActivateAccount.route)  { ActivateAccountScreen(navController) }
            composable(Screen.Home.route)          { HomeScreen(navController) }
            composable(Screen.Proiecte.route)    { ProjectsScreen(navController) }
            composable(Screen.AddProject.route)  { AddProjectScreen(navController) }
            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                ProjectDetailScreen(
                    navController = navController,
                    projectId     = backStackEntry.arguments?.getLong("projectId") ?: 0L,
                )
            }
            composable(
                route = Screen.AssignEmployees.route,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                AssignEmployeesScreen(
                    navController = navController,
                    projectId     = backStackEntry.arguments?.getLong("projectId") ?: 0L,
                )
            }
            composable(
                route = Screen.PontareEntry.route,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.LongType },
                    navArgument("userId") { type = NavType.LongType },
                ),
            ) { backStackEntry ->
                PontareEntryScreen(
                    navController = navController,
                    projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L,
                    userId = backStackEntry.arguments?.getLong("userId") ?: 0L,
                )
            }
            composable(Screen.Pontare.route)     { PontareScreen(navController) }
            composable(Screen.Calitate.route)    { CalitateScreen(navController) }
            composable(Screen.Angajati.route)    { AngajatiScreen(navController) }
            composable(Screen.AddEmployee.route) { AddEmployeeScreen(navController) }
            composable(
                route = Screen.EmployeeDetail.route,
                arguments = listOf(navArgument("employeeId") { type = NavType.LongType })
            ) { backStackEntry ->
                EmployeeDetailScreen(
                    navController = navController,
                    employeeId    = backStackEntry.arguments?.getLong("employeeId") ?: 0L,
                )
            }
            composable(
                route = Screen.ManageEmployeeSkills.route,
                arguments = listOf(navArgument("employeeId") { type = NavType.LongType })
            ) { backStackEntry ->
                ManageEmployeeSkillsScreen(
                    navController = navController,
                    employeeId    = backStackEntry.arguments?.getLong("employeeId") ?: 0L,
                )
            }
            composable(Screen.Leaderboard.route) { LeaderboardScreen(navController) }
            composable(Screen.Preturi.route)     { PreturiScreen(navController) }
            composable(Screen.Ofertare.route)    { OfertareScreen(navController) }
            composable(Screen.Settings.route)    { SettingsScreen(navController) }
        }
    }
}