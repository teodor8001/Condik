package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.User
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.viewmodel.ProjectDetailViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    navController: NavController,
    projectId: Long,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) { viewModel.load(projectId) }
    // Reincarca cand revii din AssignEmployees
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load(projectId) }

    val ronFormatter = NumberFormat.getNumberInstance(Locale("ro", "RO")).apply {
        maximumFractionDigits = 0
    }
    val openDrawer = LocalOpenDrawer.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.project?.title ?: "Proiect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Inapoi",
                        )
                    }
                },
                actions = {
                    if (openDrawer != null) {
                        IconButton(onClick = { openDrawer() }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Deschide meniu")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.AssignEmployees.createRoute(projectId))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Asigneaza angajati",
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.isLoading && state.project == null -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.project == null -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.errorMessage ?: "Proiectul nu a fost gasit.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            else -> {
                ProjectDetailContent(
                    project = state.project!!,
                    team = state.team,
                    zones = state.zones,
                    pontari = state.pontari,
                    totalSurface = state.totalSurface,
                    completedSurface = state.completedSurface,
                    progress = state.progress,
                    ronFormatter = ronFormatter,
                    contentPadding = padding,
                    onPontareClick = { userId ->
                        navController.navigate(
                            Screen.PontareEntry.createRoute(projectId, userId)
                        )
                    },
                    onCheckInToggle = viewModel::toggleCheckIn,
                    onAddZone = { name, surface ->
                        viewModel.addZone(projectId, name, surface)
                    },
                )
            }
        }
    }
}

@Composable
private fun ProjectDetailContent(
    project: Project,
    team: List<User>,
    zones: List<com.example.workipi.data.model.Zone>,
    pontari: List<com.example.workipi.viewmodel.ProjectPontareEntry>,
    totalSurface: Float,
    completedSurface: Float,
    progress: Float,
    ronFormatter: NumberFormat,
    contentPadding: PaddingValues,
    onPontareClick: (Long) -> Unit,
    onCheckInToggle: (Long) -> Unit,
    onAddZone: (String, Float) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isTabletLandscape = maxWidth > 600.dp
        if (isTabletLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ProjectInfoCard(project, totalSurface, completedSurface, progress, ronFormatter)
                    ZonesCard(zones, onAddZone)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    TeamSection(team, ronFormatter, onPontareClick, onCheckInToggle)
                    PontariCard(pontari)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ProjectInfoCard(project, totalSurface, completedSurface, progress, ronFormatter)
                ZonesCard(zones, onAddZone)
                TeamSection(team, ronFormatter, onPontareClick, onCheckInToggle)
                PontariCard(pontari)
            }
        }
    }
}

@Composable
private fun ProjectInfoCard(
    project: Project,
    totalSurface: Float,
    completedSurface: Float,
    progress: Float,
    ronFormatter: NumberFormat,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sumar proiect",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                StatusBadge(status = computeStatus(progress, project.endDate))
            }

            Text(
                text = project.adress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Progres",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                )
                Text(
                    text = "${completedSurface.toInt()} / ${totalSurface.toInt()} mp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            InfoRow(
                icon = { Icon(Icons.Filled.CalendarToday, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                label = "Perioada:",
                value = "${formatDate(project.startDate)}  →  ${formatDate(project.endDate)}",
            )

            InfoRow(
                icon = { Icon(Icons.Filled.Payments, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                label = "Buget:",
                value = project.budget?.let { "${ronFormatter.format(it)} RON" } ?: "—",
            )

            InfoRow(
                icon = { Icon(Icons.Filled.Payments, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                label = "Costuri salarii:",
                value = "${ronFormatter.format(project.totalSalaryPerMonth)} RON / luna",
            )
        }
    }
}

@Composable
private fun InfoRow(icon: @Composable () -> Unit, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun TeamSection(
    team: List<User>,
    ronFormatter: NumberFormat,
    onPontareClick: (Long) -> Unit,
    onCheckInToggle: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Echipa (${team.size} angajati)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (team.isEmpty()) {
                Text(
                    text = "Nicio persoana asignata. Apasa butonul + ca sa adaugi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                team.forEach { user ->
                    TeamRow(
                        user = user,
                        ronFormatter = ronFormatter,
                        onPontare = { onPontareClick(user.idUser) },
                        onCheckIn = { onCheckInToggle(user.idUser) },
                    )
                    if (user != team.last()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamRow(
    user: User,
    ronFormatter: NumberFormat,
    onPontare: () -> Unit,
    onCheckIn: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = user.role?.replaceFirstChar { it.uppercase() } ?: "Angajat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = user.salary?.let { "${ronFormatter.format(it)} RON" } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 4.dp),
        )
        IconButton(onClick = onCheckIn, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (user.isCheckedIn) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (user.isCheckedIn) "Check-out" else "Check-in",
                tint = if (user.isCheckedIn)
                    androidx.compose.ui.graphics.Color(0xFF2E7D32)
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onPontare, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "Pontare",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ZonesCard(
    zones: List<com.example.workipi.data.model.Zone>,
    onAddZone: (String, Float) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Zone (${zones.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                FilledTonalButton(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Adauga zona", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (zones.isEmpty()) {
                Text(
                    text = "Proiectul nu are zone definite.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                zones.forEach { zone ->
                    val total = zone.surface
                    val done = zone.surfaceCompleted ?: 0f
                    val progress = if (total <= 0f) 0f else (done / total).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = zone.name ?: "Zona",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "${(progress * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        )
                        Text(
                            text = "${done.toInt()} / ${total.toInt()} mp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (zone != zones.last()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddZoneDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, surface ->
                onAddZone(name, surface)
                showDialog = false
            },
        )
    }
}

@Composable
private fun AddZoneDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Float) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var surface by remember { mutableStateOf("") }
    val parsedSurface = surface.toFloatOrNull()
    val isValid = name.isNotBlank() && parsedSurface != null && parsedSurface > 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adauga zona") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nume zona") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
                OutlinedTextField(
                    value = surface,
                    onValueChange = { v -> surface = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("Suprafata (mp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    shape = RoundedCornerShape(10.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, parsedSurface ?: 0f) },
                enabled = isValid,
            ) { Text("Salveaza") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuleaza") }
        },
    )
}

@Composable
private fun PontariCard(pontari: List<com.example.workipi.viewmodel.ProjectPontareEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Istoric pontari (${pontari.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (pontari.isEmpty()) {
                Text(
                    text = "Nicio pontare inregistrata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                pontari.forEach { entry ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = entry.userName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = entry.pontare.workDate?.toString() ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${entry.lucrareName} • ${entry.zoneName} • ${entry.pontare.quantity.toInt()} ${entry.lucrareUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (entry != pontari.last()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}
