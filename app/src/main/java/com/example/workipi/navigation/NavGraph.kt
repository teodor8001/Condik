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
fun NavGraph(navController: NavHostController) {
    AppNavigationDrawer(navController = navController) {
        NavHost(
            navController    = navController,
            startDestination = Screen.Login.route
        ) {
            composable(Screen.Login.route)       { LoginScreen(navController) }
            composable(Screen.Home.route)        { HomeScreen(navController) }
            composable(Screen.Proiecte.route)    { ProiecteScreen(navController) }
            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                ProjectDetailScreen(
                    navController = navController,
                    projectId     = backStackEntry.arguments?.getString("projectId")
                )
            }
            composable(Screen.Pontare.route)     { PontareScreen(navController) }
            composable(Screen.Calitate.route)    { CalitateScreen(navController) }
            composable(Screen.Angajati.route)    { AngajatiScreen(navController) }
            composable(
                route = Screen.EmployeeDetail.route,
                arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
            ) { backStackEntry ->
                EmployeeDetailScreen(
                    navController = navController,
                    employeeId    = backStackEntry.arguments?.getString("employeeId")
                )
            }
            composable(Screen.Leaderboard.route) { LeaderboardScreen(navController) }
            composable(Screen.Preturi.route)     { PreturiScreen(navController) }
            composable(Screen.Settings.route)    { SettingsScreen(navController) }
        }
    }
}