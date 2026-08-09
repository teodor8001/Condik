package com.example.workipi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.workipi.data.model.AppPermission
import com.example.workipi.data.model.UserRole
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.session.LocalSessionState
import com.example.workipi.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// CompositionLocal — ofera acces la "deschide drawer" din orice screen copil.
// Null pe tableta (drawer permanent, buton hamburger nu e necesar).
// ---------------------------------------------------------------------------
val LocalOpenDrawer = compositionLocalOf<(() -> Unit)?> { null }

// ---------------------------------------------------------------------------
// Model item meniu
// ---------------------------------------------------------------------------
private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val permission: AppPermission,
)

private val navigationItems = listOf(
    NavItem(Screen.Home,        "Acasă",        Icons.Filled.Home, AppPermission.DASHBOARD_VIEW),
    NavItem(Screen.Proiecte,    "Proiecte",     Icons.Filled.Business, AppPermission.PROJECTS_VIEW),
    NavItem(Screen.Santier,     "Șantier",      Icons.Filled.Construction, AppPermission.SITE_VIEW),
    NavItem(Screen.Angajati,    "Echipă",       Icons.Filled.Group, AppPermission.TEAM_VIEW),
    NavItem(Screen.Leaderboard, "Performanță",  Icons.Filled.EmojiEvents, AppPermission.PERFORMANCE_VIEW),
    NavItem(Screen.Resurse,     "Resurse",      Icons.Filled.Inventory2, AppPermission.RESOURCES_VIEW),
    NavItem(Screen.Ofertare,    "Ofertare",     Icons.Filled.Description, AppPermission.OFFERS_VIEW),
    NavItem(Screen.Firma,       "Administrare", Icons.Filled.Domain, AppPermission.ADMINISTRATION_VIEW),
)

// Ecrane de editare/adaugare: la navigare in afara lor (din meniu) intrebam intai
// daca userul vrea sa renunte, ca sa nu piarda ce a introdus.
private val editRoutes = setOf(
    Screen.AddProject.route,
    Screen.AddEmployee.route,
    Screen.ManageEmployeeSkills.route,
    Screen.AssignEmployees.route,
    Screen.PontareEntry.route,
)

/**
 * Permite unui ecran de editare sa dezactiveze temporar confirmarea de la navigare
 * (ex. dupa ce s-a generat codul de invitatie, editarea s-a terminat -> nu mai intrebam).
 */
object NavEditGuard {
    var skipConfirm by mutableStateOf(false)
}

// ---------------------------------------------------------------------------
// Continutul vizual al drawer-ului (header + itemi + footer)
// ---------------------------------------------------------------------------
@Composable
private fun DrawerContent(
    navController: NavController,
    currentRoute: String?,
    onItemClick: () -> Unit = {},
    onCloseDrawer: () -> Unit = {}
) {
    val session = LocalSessionState.current
    val user = session.user
    val items = navigationItems.filter { session.hasPermission(it.permission) }
    val sessionViewModel: SessionViewModel = hiltViewModel()
    var pendingNav by remember { mutableStateOf<(() -> Unit)?>(null) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(272.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(vertical = 18.dp)
    ) {
        // ---- Header: logo ----
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Text("C", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(text = "CONDIK", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Management șantier", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.35.sp)
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(12.dp))

        // ---- Itemi navigare ----
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                if (item.screen == Screen.Firma) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ADMINISTRARE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                val selected = currentRoute == item.screen.route
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selected = selected,
                    onClick = {
                        if (currentRoute != item.screen.route) {
                            val doNavigate = {
                                onItemClick()
                                navController.navigate(item.screen.route) {
                                    // Curata back stack-ul pana la Home, ca sa nu ramana
                                    // ecrane de detaliu (ex. ProjectDetail) deasupra destinatiei.
                                    popUpTo(Screen.Home.route)
                                    launchSingleTop = true
                                }
                            }
                            // Daca esti pe un ecran de editare (si editarea nu s-a terminat),
                            // confirma intai renuntarea.
                            if (currentRoute in editRoutes && !NavEditGuard.skipConfirm) {
                                pendingNav = doNavigate
                            } else {
                                doNavigate()
                            }
                        }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        selectedIconColor      = MaterialTheme.colorScheme.primary,
                        selectedTextColor      = MaterialTheme.colorScheme.primary,
                        unselectedIconColor    = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        if (session.hasPermission(AppPermission.SETTINGS_VIEW)) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Settings, contentDescription = "Setări") },
                label = { Text("Setări") },
                selected = currentRoute == Screen.Settings.route,
                onClick = {
                    if (currentRoute != Screen.Settings.route) {
                        val doNavigate = {
                            onItemClick()
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        }
                        if (currentRoute in editRoutes && !NavEditGuard.skipConfirm) {
                            pendingNav = doNavigate
                        } else {
                            doNavigate()
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ---- Footer: user info ----
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (user != null) {
            var menuExpanded by remember { mutableStateOf(false) }

            Box {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { menuExpanded = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name
                            .split(" ")
                            .take(2)
                            .joinToString("") { it.first().uppercase() },
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = when (user.role) {
                                UserRole.ADMIN      -> "Administrator"
                                UserRole.MANAGER    -> "Manager"
                                UserRole.INGINER    -> "Inginer"
                                UserRole.SEF_ECHIPA -> "Sef de echipa"
                                UserRole.ANGAJAT    -> "Angajat"
                                UserRole.CLIENT     -> "Client"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Optiuni cont",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Deconectare") },
                        leadingIcon = {
                            Icon(Icons.Filled.Logout, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onCloseDrawer()
                            sessionViewModel.logout {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            }
        }

        pendingNav?.let { proceed ->
            ConfirmDialog(
                title = "Renunti la modificari?",
                message = "Daca pleci de aici, ce ai introdus se va pierde.",
                onConfirm = { pendingNav = null; proceed() },
                onDismiss = { pendingNav = null },
                confirmLabel = "Da, renunt",
                dismissLabel = "Nu",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Wrapper principal — alege Permanent (tableta) sau Modal (telefon)
// ---------------------------------------------------------------------------
@Composable
fun AppNavigationDrawer(
    navController: NavController,
    content: @Composable () -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    val authRoutes     = setOf(
        Screen.Login.route,
        Screen.CreateCompany.route,
        Screen.ActivateAccount.route,
    )
    val showDrawer     = currentRoute != null && currentRoute !in authRoutes

    if (!showDrawer) {
        content()
        return
    }

    // Detectare dimensiune ecran fara BoxWithConstraints (mai fiabila)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isTablet      = screenWidthDp > 600

    if (isTablet) {
        // Tableta: drawer permanent vizibil in stanga, fara hamburger
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier.width(272.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                ) {
                    DrawerContent(
                        navController = navController,
                        currentRoute  = currentRoute
                    )
                }
            }
        ) {
            // statusBarsPadding o singura data, la nivel de wrapper
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                content()
            }
        }
    } else {
        // Telefon: drawer modal (slide din stanga)
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val drawerScope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    DrawerContent(
                        navController = navController,
                        currentRoute  = currentRoute,
                        onItemClick   = {
                            drawerScope.launch { drawerState.close() }
                        },
                        onCloseDrawer = {
                            drawerScope.launch { drawerState.close() }
                        },
                    )
                }
            }
        ) {
            // statusBarsPadding o singura data, la nivel de wrapper
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                CompositionLocalProvider(
                    LocalOpenDrawer provides {
                        drawerScope.launch { drawerState.open() }
                    }
                ) {
                    content()
                }
            }
        }
    }
}
