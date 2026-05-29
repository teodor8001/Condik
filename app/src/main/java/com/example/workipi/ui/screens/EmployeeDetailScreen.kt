package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.example.workipi.data.model.User
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.viewmodel.EmployeeDetailViewModel
import com.example.workipi.viewmodel.EmployeeSkill
import com.example.workipi.viewmodel.PontareEntry

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    navController: NavController,
    employeeId: Long,
    viewModel: EmployeeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val openDrawer = LocalOpenDrawer.current

    LaunchedEffect(employeeId) { viewModel.load(employeeId) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load(employeeId) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.consumeDeleted()
            navController.popBackStack()
        }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.employee?.fullName ?: "Angajat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Inapoi"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = state.employee != null && !state.isDeleting,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Elimina angajat",
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            state.isLoading && state.employee == null -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.employee == null -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.errorMessage ?: "Angajatul nu a fost gasit.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            else -> EmployeeContent(
                employee = state.employee!!,
                skills = state.skills,
                pontari = state.pontari,
                contentPadding = innerPadding,
                onEditSkills = {
                    navController.navigate(
                        Screen.ManageEmployeeSkills.createRoute(employeeId)
                    )
                },
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Elimina angajat") },
            text = {
                Text(
                    "Esti sigur ca vrei sa elimini pe ${state.employee?.fullName ?: "acest angajat"}? " +
                            "Aceasta actiune va sterge si istoricul pontarilor lui. Ireversibil."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.removeEmployee(employeeId)
                    },
                    enabled = !state.isDeleting,
                ) {
                    Text("Elimina", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Anuleaza") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployeeContent(
    employee: User,
    skills: List<EmployeeSkill>,
    pontari: List<PontareEntry>,
    contentPadding: PaddingValues,
    onEditSkills: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = employee.fullName
                            .split(" ")
                            .take(2)
                            .joinToString("") { it.firstOrNull()?.uppercase() ?: "" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Text(
                    text = employee.fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = employee.role?.replaceFirstChar { it.uppercase() } ?: "Angajat",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

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
                Text(
                    text = "Informatii contact",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                InfoRow(icon = Icons.Filled.Phone, label = "Telefon", value = employee.phoneNumber.toString())
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                InfoRow(icon = Icons.Filled.Email, label = "Email", value = employee.email)
                if (employee.salary != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    InfoRow(
                        icon = Icons.Filled.AttachMoney,
                        label = "Salariu",
                        value = "${employee.salary.toInt()} RON / luna",
                    )
                }
                if (employee.points != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    InfoRow(
                        icon = Icons.Filled.Star,
                        label = "Punctaj",
                        value = "${employee.points.toInt()} pts",
                    )
                }
            }
        }

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
                        text = "Competente (${skills.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    TextButton(onClick = onEditSkills) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Editeaza")
                    }
                }
                if (skills.isEmpty()) {
                    Text(
                        text = "Niciun skill atasat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        skills.forEach { SkillChip(it) }
                    }
                }
            }
        }

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
                        EmployeePontareRow(entry)
                        if (entry != pontari.last()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeePontareRow(entry: PontareEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                text = "${entry.history.workDate ?: "—"} • ${entry.history.quantity.toInt()} ${entry.lucrareUnit} • ${entry.history.hours?.toInt() ?: 0}h",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "+${entry.earnedPoints} pts",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun SkillChip(skill: EmployeeSkill) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = "${skill.lucrare.name} • ${skill.level}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
