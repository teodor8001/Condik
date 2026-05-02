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
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.UserRole
import com.example.workipi.navigation.Screen
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
    val icon: ImageVector
)

private val adminItems = listOf(
    NavItem(Screen.Home,        "Acasa",      Icons.Filled.Home),
    NavItem(Screen.Proiecte,    "Proiecte",   Icons.Filled.Business),
    NavItem(Screen.Pontare,     "Pontare",    Icons.Filled.Timer),
    NavItem(Screen.Calitate,    "Calitate",   Icons.Filled.VerifiedUser),
    NavItem(Screen.Angajati,    "Angajati",   Icons.Filled.Group),
    NavItem(Screen.Leaderboard, "Topuri",     Icons.Filled.EmojiEvents),
    NavItem(Screen.Preturi,     "Preturi",    Icons.Filled.Payments),
)

private val angajatItems = listOf(
    NavItem(Screen.Home,        "Acasa",      Icons.Filled.Home),
    NavItem(Screen.Leaderboard, "Topuri",     Icons.Filled.EmojiEvents),
)

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
    val user  = MockSession.currentUser
    val items = when (user?.role) {
        UserRole.ANGAJAT -> angajatItems
        else             -> adminItems
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 24.dp)
    ) {
        // ---- Header: logo ----
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "WorkIPI",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Management santier",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
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
                        onItemClick()
                        if (currentRoute != item.screen.route) {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
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
                                UserRole.ADMIN           -> "Administrator"
                                UserRole.PROJECT_MANAGER -> "Inginer"
                                UserRole.ANGAJAT         -> "Angajat"
                                UserRole.CLIENT          -> "Client"
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
                        text = { Text("Setari") },
                        leadingIcon = {
                            Icon(Icons.Filled.Settings, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onCloseDrawer()
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Deconectare") },
                        leadingIcon = {
                            Icon(Icons.Filled.Logout, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            MockSession.currentUser = null
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
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
    val authRoutes     = setOf(Screen.Login.route, Screen.CreateAccount.route)
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
                    modifier = Modifier.width(260.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    DrawerContent(
                        navController = navController,
                        currentRoute  = currentRoute
                    )
                }
            }
        ) {
            // statusBarsPadding o singura data, la nivel de wrapper
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                content()
            }
        }
    } else {
        // Telefon: drawer modal (slide din stanga)
        val drawerState = rememberDrawerState(DrawerValue.Closed)

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
                            // Inchide drawer-ul dupa navigare
                        }
                    )
                }
            }
        ) {
            // statusBarsPadding o singura data, la nivel de wrapper
            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                content()
            }
        }
    }
}
