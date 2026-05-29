package com.example.workipi.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.User
import com.example.workipi.data.model.UserRole
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.data.model.Lucrare
import com.example.workipi.viewmodel.ProjectDetailViewModel
import com.example.workipi.viewmodel.ProjectLeaderboardEntry
import com.example.workipi.viewmodel.ZoneLucrareEntry
import com.example.workipi.viewmodel.ZoneSection
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
    val context = LocalContext.current
    var showDeleteProject by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

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
                    IconButton(onClick = { showDeleteProject = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Sterge proiect",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
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
                val currentRole = MockSession.currentUser?.role
                val canSeeLeaderboard = currentRole == UserRole.ADMIN || currentRole == UserRole.PROJECT_MANAGER
                ProjectDetailContent(
                    project = state.project!!,
                    team = state.team,
                    zoneSections = state.zoneSections,
                    hasOnlyImplicitZone = state.hasOnlyImplicitZone,
                    firmaLucrari = state.firmaLucrari,
                    pontari = state.pontari,
                    leaderboard = state.leaderboard,
                    showLeaderboard = canSeeLeaderboard,
                    totalSurface = state.totalSurface,
                    completedSurface = state.completedSurface,
                    progress = state.progress,
                    salaryCostTotal = state.salaryCostTotal,
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
                    onAddLucrare = viewModel::addLucrareToZone,
                    onRemoveLucrare = viewModel::removeLucrareFromZone,
                    onDeleteZone = viewModel::deleteZone,
                    materiale = state.materiale,
                    materialeTotalCost = state.materialeTotalCost,
                    onAddMaterial = viewModel::addMaterial,
                    onEditMaterial = viewModel::updateMaterial,
                    onRemoveMaterial = viewModel::removeMaterial,
                    onAcceptOffer = {
                        viewModel.acceptOffer {
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    }

    if (showDeleteProject) {
        AlertDialog(
            onDismissRequest = { showDeleteProject = false },
            title = { Text("Sterge proiect") },
            text = {
                Text(
                    "Esti sigur ca vrei sa stergi proiectul \"${state.project?.title ?: ""}\"? " +
                        "Zonele si lucrarile asociate vor fi sterse."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteProject = false
                    viewModel.deleteProject { navController.popBackStack() }
                }) { Text("Sterge", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProject = false }) { Text("Anuleaza") }
            },
        )
    }
}

@Composable
private fun ProjectDetailContent(
    project: Project,
    team: List<User>,
    zoneSections: List<ZoneSection>,
    hasOnlyImplicitZone: Boolean,
    firmaLucrari: List<Lucrare>,
    pontari: List<com.example.workipi.viewmodel.ProjectPontareEntry>,
    leaderboard: List<ProjectLeaderboardEntry>,
    showLeaderboard: Boolean,
    totalSurface: Float,
    completedSurface: Float,
    progress: Float,
    salaryCostTotal: Float,
    ronFormatter: NumberFormat,
    contentPadding: PaddingValues,
    onPontareClick: (Long) -> Unit,
    onCheckInToggle: (Long) -> Unit,
    onAddZone: (String, Float) -> Unit,
    onAddLucrare: (Long, Long, Float) -> Unit,
    onRemoveLucrare: (Long, Long) -> Unit,
    onDeleteZone: (Long) -> Unit,
    materiale: List<com.example.workipi.data.model.Material>,
    materialeTotalCost: Float,
    onAddMaterial: (String, Float, String, Float) -> Unit,
    onEditMaterial: (Long, String, Float, String, Float) -> Unit,
    onRemoveMaterial: (Long) -> Unit,
    onAcceptOffer: () -> Unit,
) {
    val currentRole = MockSession.currentUser?.role
    val canEditMaterials = currentRole == UserRole.ADMIN || currentRole == UserRole.PROJECT_MANAGER
    @Composable
    fun AcceptOfferButton() {
        if (project.isOffer) {
            var showConfirm by remember { mutableStateOf(false) }
            Button(
                onClick = { showConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = "Accepta oferta",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text("Accepta oferta") },
                    text = { Text("Oferta va deveni proiect activ si va aparea in lista de Proiecte. Confirmi?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showConfirm = false
                            onAcceptOffer()
                        }) { Text("Accepta") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirm = false }) { Text("Anuleaza") }
                    },
                )
            }
        }
    }

    @Composable
    fun ZonesOrLucrari() {
        if (hasOnlyImplicitZone) {
            val implicit = zoneSections.firstOrNull()
            LucrariCard(
                title = "Lucrari",
                lucrari = implicit?.lucrari.orEmpty(),
                allowAdd = implicit != null,
                firmaLucrari = firmaLucrari,
                onAdd = { lucrareId, qty ->
                    implicit?.zone?.id?.let { onAddLucrare(it, lucrareId, qty) }
                },
                onRemove = { lucrareId ->
                    implicit?.zone?.id?.let { onRemoveLucrare(it, lucrareId) }
                },
            )
        } else {
            ZonesWithLucrariCard(
                zoneSections = zoneSections,
                firmaLucrari = firmaLucrari,
                onAddZone = onAddZone,
                onAddLucrare = onAddLucrare,
                onRemoveLucrare = onRemoveLucrare,
                onDeleteZone = onDeleteZone,
            )
        }
    }

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
                    AcceptOfferButton()
                    ProjectInfoCard(project, totalSurface, completedSurface, progress, salaryCostTotal, ronFormatter, hasOnlyImplicitZone)
                    ZonesOrLucrari()
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    TeamSection(team, ronFormatter, onPontareClick, onCheckInToggle)
                    MaterialeCard(
                        materiale = materiale,
                        totalCost = materialeTotalCost,
                        canEdit = canEditMaterials,
                        ronFormatter = ronFormatter,
                        onAdd = onAddMaterial,
                        onEdit = onEditMaterial,
                        onRemove = onRemoveMaterial,
                    )
                    if (showLeaderboard) ProjectLeaderboardCard(leaderboard)
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
                AcceptOfferButton()
                ProjectInfoCard(project, totalSurface, completedSurface, progress, salaryCostTotal, ronFormatter, hasOnlyImplicitZone)
                ZonesOrLucrari()
                TeamSection(team, ronFormatter, onPontareClick, onCheckInToggle)
                MaterialeCard(
                    materiale = materiale,
                    totalCost = materialeTotalCost,
                    canEdit = canEditMaterials,
                    ronFormatter = ronFormatter,
                    onAdd = onAddMaterial,
                    onEdit = onEditMaterial,
                    onRemove = onRemoveMaterial,
                )
                if (showLeaderboard) ProjectLeaderboardCard(leaderboard)
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
    salaryCostTotal: Float,
    ronFormatter: NumberFormat,
    hideSurface: Boolean = false,
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
                StatusBadge(status = computeStatus(progress, project.endDate, project.isOffer))
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
                if (!hideSurface) {
                    Text(
                        text = "${completedSurface.toInt()} / ${totalSurface.toInt()} mp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                label = "Costuri salarii (total proiect):",
                value = "${ronFormatter.format(salaryCostTotal)} RON",
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
private fun LucrariCard(
    title: String,
    lucrari: List<ZoneLucrareEntry>,
    allowAdd: Boolean,
    firmaLucrari: List<Lucrare>,
    onAdd: (Long, Float) -> Unit,
    onRemove: (Long) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val existingIds = lucrari.map { it.lucrareId }.toSet()

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
                    text = "$title (${lucrari.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (allowAdd) {
                    FilledTonalButton(
                        onClick = { showDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Adauga lucrare", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (lucrari.isEmpty()) {
                Text(
                    text = "Nicio lucrare adaugata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                lucrari.forEach { entry ->
                    LucrareRow(entry = entry, onRemove = { onRemove(entry.lucrareId) })
                    if (entry != lucrari.last()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddLucrareDialog(
            available = firmaLucrari.filter { it.id !in existingIds },
            onDismiss = { showDialog = false },
            onConfirm = { lucrareId, qty ->
                onAdd(lucrareId, qty)
                showDialog = false
            },
        )
    }
}

@Composable
private fun LucrareRow(entry: ZoneLucrareEntry, onRemove: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.lucrareName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${entry.totalQuantity.toInt()} ${entry.lucrareUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${(entry.progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Sterge lucrare",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        LinearProgressIndicator(
            progress = { entry.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )
        Text(
            text = "Executat: ${entry.completedQuantity.toInt()} ${entry.lucrareUnit}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ZonesWithLucrariCard(
    zoneSections: List<ZoneSection>,
    firmaLucrari: List<Lucrare>,
    onAddZone: (String, Float) -> Unit,
    onAddLucrare: (Long, Long, Float) -> Unit,
    onRemoveLucrare: (Long, Long) -> Unit,
    onDeleteZone: (Long) -> Unit,
) {
    var showAddZone by remember { mutableStateOf(false) }
    val visibleZones = zoneSections.filterNot { it.isImplicit }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Zone (${visibleZones.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                FilledTonalButton(
                    onClick = { showAddZone = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Adauga zona", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (visibleZones.isEmpty()) {
                Text(
                    text = "Proiectul nu are zone definite.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                visibleZones.forEach { section ->
                    ZoneBlock(
                        section = section,
                        firmaLucrari = firmaLucrari,
                        onAddLucrare = { lucrareId, qty -> onAddLucrare(section.zone.id, lucrareId, qty) },
                        onRemoveLucrare = { lucrareId -> onRemoveLucrare(section.zone.id, lucrareId) },
                        onDeleteZone = { onDeleteZone(section.zone.id) },
                    )
                    if (section != visibleZones.last()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }

    if (showAddZone) {
        AddZoneDialog(
            onDismiss = { showAddZone = false },
            onConfirm = { name, surface ->
                onAddZone(name, surface)
                showAddZone = false
            },
        )
    }
}

@Composable
private fun ZoneBlock(
    section: ZoneSection,
    firmaLucrari: List<Lucrare>,
    onAddLucrare: (Long, Float) -> Unit,
    onRemoveLucrare: (Long) -> Unit,
    onDeleteZone: () -> Unit,
) {
    var showAddLucrare by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val existingIds = section.lucrari.map { it.lucrareId }.toSet()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.zone.name ?: "Zona",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${section.zone.surface.toInt()} mp • ${section.lucrari.size} lucrari",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${(section.progress * 100).roundToInt()}%",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Sterge zona",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        LinearProgressIndicator(
            progress = { section.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )

        Column(
            modifier = Modifier.padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (section.lucrari.isEmpty()) {
                Text(
                    text = "Nicio lucrare adaugata.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                section.lucrari.forEach { entry ->
                    LucrareRow(entry = entry, onRemove = { onRemoveLucrare(entry.lucrareId) })
                }
            }

            FilledTonalButton(
                onClick = { showAddLucrare = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Adauga lucrare in zona", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showAddLucrare) {
        AddLucrareDialog(
            available = firmaLucrari.filter { it.id !in existingIds },
            onDismiss = { showAddLucrare = false },
            onConfirm = { lucrareId, qty ->
                onAddLucrare(lucrareId, qty)
                showAddLucrare = false
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Sterge zona") },
            text = {
                Text("Esti sigur ca vrei sa stergi zona \"${section.zone.name ?: "Zona"}\"? Lucrarile atasate vor fi sterse.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteZone()
                }) { Text("Sterge", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Anuleaza") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLucrareDialog(
    available: List<Lucrare>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Float) -> Unit,
) {
    var selected by remember { mutableStateOf<Lucrare?>(null) }
    var quantity by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val qtyParsed = quantity.toFloatOrNull()
    val isValid = selected != null && qtyParsed != null && qtyParsed > 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adauga lucrare") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (available.isEmpty()) {
                    Text(
                        text = "Nu mai sunt lucrari disponibile in firma. Adauga in Preturi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        OutlinedTextField(
                            value = selected?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Lucrare") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            available.forEach { lucrare ->
                                DropdownMenuItem(
                                    text = { Text("${lucrare.name} (${lucrare.unit})") },
                                    onClick = {
                                        selected = lucrare
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { v -> quantity = v.filter { it.isDigit() || it == '.' } },
                        label = { Text("Cantitate totala${selected?.unit?.let { " ($it)" } ?: ""}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected!!.id, qtyParsed!!) },
                enabled = isValid,
            ) { Text("Salveaza") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuleaza") }
        },
    )
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
private fun MaterialeCard(
    materiale: List<com.example.workipi.data.model.Material>,
    totalCost: Float,
    canEdit: Boolean,
    ronFormatter: NumberFormat,
    onAdd: (String, Float, String, Float) -> Unit,
    onEdit: (Long, String, Float, String, Float) -> Unit,
    onRemove: (Long) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMaterial by remember { mutableStateOf<com.example.workipi.data.model.Material?>(null) }
    var deletingId by remember { mutableStateOf<Long?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Materiale (${materiale.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (canEdit) {
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Adauga", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (materiale.isEmpty()) {
                Text(
                    text = "Niciun material adaugat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                materiale.forEach { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = m.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${m.quantity.toInt()} ${m.unit} × ${ronFormatter.format(m.unitCost)} RON",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${ronFormatter.format(m.totalCost)} RON",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (canEdit) {
                            IconButton(
                                onClick = { editingMaterial = m },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Editeaza",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            IconButton(
                                onClick = { deletingId = m.id },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Sterge",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                    if (m != materiale.last()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Total materiale",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${ronFormatter.format(totalCost)} RON",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        MaterialDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, qty, unit, cost ->
                onAdd(name, qty, unit, cost)
                showAddDialog = false
            },
        )
    }
    editingMaterial?.let { m ->
        MaterialDialog(
            initial = m,
            onDismiss = { editingMaterial = null },
            onConfirm = { name, qty, unit, cost ->
                onEdit(m.id, name, qty, unit, cost)
                editingMaterial = null
            },
        )
    }
    deletingId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("Sterge material") },
            text = { Text("Esti sigur ca vrei sa stergi acest material?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(id)
                    deletingId = null
                }) { Text("Sterge", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) { Text("Anuleaza") }
            },
        )
    }
}

@Composable
private fun MaterialDialog(
    initial: com.example.workipi.data.model.Material?,
    onDismiss: () -> Unit,
    onConfirm: (String, Float, String, Float) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var quantity by remember { mutableStateOf(initial?.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var unitCost by remember { mutableStateOf(initial?.unitCost?.toString() ?: "") }
    val qtyParsed = quantity.toFloatOrNull()
    val costParsed = unitCost.toFloatOrNull()
    val isValid = name.isNotBlank() && unit.isNotBlank() &&
        qtyParsed != null && qtyParsed > 0f &&
        costParsed != null && costParsed >= 0f
    val total = if (qtyParsed != null && costParsed != null) qtyParsed * costParsed else 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Adauga material" else "Editeaza material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Denumire") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { v -> quantity = v.filter { it.isDigit() || it == '.' } },
                        label = { Text("Cantitate") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("U.M.") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    )
                }
                OutlinedTextField(
                    value = unitCost,
                    onValueChange = { v -> unitCost = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("Cost / unitate (RON)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    ),
                    shape = RoundedCornerShape(10.dp),
                )
                Text(
                    text = "Total: ${total.toInt()} RON",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, qtyParsed!!, unit, costParsed!!) },
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
                                text = entry.history.workDate?.toString() ?: "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "${entry.lucrareName} • ${entry.zoneName} • ${entry.history.quantity.toInt()} ${entry.lucrareUnit}",
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

@Composable
private fun ProjectLeaderboardCard(entries: List<ProjectLeaderboardEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Clasament proiect",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Doar admin / inginer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (entries.isEmpty()) {
                Text(
                    text = "Nu sunt inca puncte acumulate pe acest proiect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    val position = index + 1
                    val medal = when (position) {
                        1 -> "🥇"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> "#$position"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = medal,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = entry.userName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${entry.points} pts",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (entry != entries.last()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}
