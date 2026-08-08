package com.example.workipi.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.workipi.ui.components.AppNavigationDrawer
import com.example.workipi.data.model.AppPermission
import com.example.workipi.session.SessionState
import com.example.workipi.ui.screens.*
import com.example.workipi.ui.session.LocalSessionState

@Composable
private fun RequirePermission(
    permission: AppPermission,
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    if (LocalSessionState.current.hasPermission(permission)) content()
    else AccessDeniedScreen(navController)
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
    sessionState: SessionState,
) {
    CompositionLocalProvider(LocalSessionState provides sessionState) {
      AppNavigationDrawer(navController = navController) {
       NavHost(
            navController    = navController,
            startDestination = startDestination
        ) {
            composable(Screen.Login.route)            { LoginScreen(navController) }
            composable(Screen.CreateCompany.route)    { CreateCompanyScreen(navController) }
            composable(Screen.ActivateAccount.route)  { ActivateAccountScreen(navController) }
            composable(Screen.ChangePassword.route)   { ChangePasswordScreen(navController) }
            composable(Screen.Home.route) {
                RequirePermission(AppPermission.DASHBOARD_VIEW, navController) {
                    HomeScreen(navController)
                }
            }
            composable(Screen.Proiecte.route) {
                RequirePermission(AppPermission.PROJECTS_VIEW, navController) {
                    ProjectsScreen(navController)
                }
            }
            composable(Screen.Santier.route) {
                RequirePermission(AppPermission.SITE_VIEW, navController) {
                    SantierScreen(navController)
                }
            }
            composable(Screen.Resurse.route) {
                RequirePermission(AppPermission.RESOURCES_VIEW, navController) {
                    ResurseScreen(navController)
                }
            }
            composable(
                route = Screen.AddProject.route,
                arguments = listOf(navArgument("offer") { type = NavType.BoolType; defaultValue = false })
            ) { backStackEntry ->
                val isOffer = backStackEntry.arguments?.getBoolean("offer") ?: false
                val permission = if (isOffer) AppPermission.OFFERS_MANAGE else AppPermission.PROJECTS_CREATE
                RequirePermission(permission, navController) {
                    AddProjectScreen(
                        navController = navController,
                        isOffer = isOffer,
                    )
                }
            }
            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                RequirePermission(AppPermission.PROJECTS_VIEW, navController) {
                    ProjectDetailScreen(
                        navController = navController,
                        projectId     = backStackEntry.arguments?.getLong("projectId") ?: 0L,
                    )
                }
            }
            composable(
                route = Screen.AssignEmployees.route,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) { backStackEntry ->
                RequirePermission(AppPermission.PROJECTS_MANAGE, navController) {
                    AssignEmployeesScreen(
                        navController = navController,
                        projectId     = backStackEntry.arguments?.getLong("projectId") ?: 0L,
                    )
                }
            }
            composable(
                route = Screen.PontareEntry.route,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.LongType },
                    navArgument("userId") { type = NavType.LongType },
                ),
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
                val session = LocalSessionState.current
                val isOwnEntry = session.user?.id == userId &&
                    session.hasPermission(AppPermission.TIME_ENTRIES_CREATE)
                if (isOwnEntry || session.hasPermission(AppPermission.TIME_ENTRIES_REVIEW)) {
                    PontareEntryScreen(
                        navController = navController,
                        projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L,
                        userId = userId,
                    )
                } else {
                    AccessDeniedScreen(navController)
                }
            }
            composable(Screen.Calitate.route) {
                RequirePermission(AppPermission.SITE_VIEW, navController) {
                    CalitateScreen(navController)
                }
            }
            composable(Screen.Angajati.route) {
                RequirePermission(AppPermission.TEAM_VIEW, navController) {
                    AngajatiScreen(navController)
                }
            }
            composable(Screen.AddEmployee.route) {
                RequirePermission(AppPermission.TEAM_MANAGE, navController) {
                    AddEmployeeScreen(navController)
                }
            }
            composable(
                route = Screen.EmployeeDetail.route,
                arguments = listOf(navArgument("employeeId") { type = NavType.LongType })
            ) { backStackEntry ->
                RequirePermission(AppPermission.TEAM_VIEW, navController) {
                    EmployeeDetailScreen(
                        navController = navController,
                        employeeId    = backStackEntry.arguments?.getLong("employeeId") ?: 0L,
                    )
                }
            }
            composable(
                route = Screen.ManageEmployeeSkills.route,
                arguments = listOf(navArgument("employeeId") { type = NavType.LongType })
            ) { backStackEntry ->
                RequirePermission(AppPermission.TEAM_MANAGE, navController) {
                    ManageEmployeeSkillsScreen(
                        navController = navController,
                        employeeId    = backStackEntry.arguments?.getLong("employeeId") ?: 0L,
                    )
                }
            }
            composable(Screen.Leaderboard.route) {
                RequirePermission(AppPermission.PERFORMANCE_VIEW, navController) {
                    LeaderboardScreen(navController)
                }
            }
            composable(Screen.Firma.route) {
                RequirePermission(AppPermission.ADMINISTRATION_VIEW, navController) {
                    FirmaScreen(navController)
                }
            }
            composable(Screen.Ofertare.route) {
                RequirePermission(AppPermission.OFFERS_VIEW, navController) {
                    OfertareScreen(navController)
                }
            }
            composable(Screen.Settings.route) {
                RequirePermission(AppPermission.SETTINGS_VIEW, navController) {
                    SettingsScreen(navController)
                }
            }
       }
      }
    }
}
