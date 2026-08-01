package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.viewmodel.ProjectWithProgress
import com.example.workipi.viewmodel.ProjectsViewModel
import kotlin.math.roundToInt

@Composable
private fun SantierOverviewPlaceholder(
    navController: NavController,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadProjects() }
    val averageProgress = state.projects
        .map { it.progress }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?.toFloat()
        ?: 0f

    OverviewPage(
        title = "Șantier",
        subtitle = "Activitatea curentă din toate proiectele accesibile",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard("Proiecte active", state.projects.size.toString(), Modifier.weight(1f))
            MetricCard("Progres mediu", "${(averageProgress * 100).roundToInt()}%", Modifier.weight(1f))
        }

        Text("Fluxul zilei", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModuleCard("Prezență", "Cine este astăzi pe șantier", Icons.Filled.People, Modifier.weight(1f))
            ModuleCard("Pontări", "Ore și cantități executate", Icons.Filled.Assignment, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModuleCard("Calitate", "Corecturi și verificări", Icons.Filled.FactCheck, Modifier.weight(1f))
            ModuleCard("Fotografii", "Dovezi și observații zilnice", Icons.Filled.PhotoCamera, Modifier.weight(1f))
        }

        ProjectSection(
            projects = state.projects,
            isLoading = state.isLoading,
            errorMessage = state.errorMessage,
            emptyMessage = "Nu există proiecte active accesibile.",
            onProjectClick = { navController.navigate(Screen.ProjectDetail.createRoute(it)) },
        )
    }
}

@Composable
fun ResurseScreen(
    navController: NavController,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadProjects() }

    OverviewPage(
        title = "Resurse",
        subtitle = "Materiale, unelte și echipamente conectate cu proiectele",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModuleCard(
                title = "Materiale",
                description = "Stoc, necesar, consum și cost pe proiect",
                icon = Icons.Filled.Inventory2,
                modifier = Modifier.weight(1f),
            )
            ModuleCard(
                title = "Unelte și echipamente",
                description = "Inventar, responsabil și disponibilitate",
                icon = Icons.Filled.Handyman,
                modifier = Modifier.weight(1f),
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Construction, contentDescription = null)
                Column {
                    Text("Resurse pe proiect", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Deschide un proiect pentru materialele existente. Inventarul central de unelte urmează în etapa Resurse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        ProjectSection(
            projects = state.projects,
            isLoading = state.isLoading,
            errorMessage = state.errorMessage,
            emptyMessage = "Nu există proiecte accesibile pentru resurse.",
            onProjectClick = { navController.navigate(Screen.ProjectDetail.createRoute(it)) },
        )
    }
}

@Composable
private fun OverviewPage(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val openDrawer = LocalOpenDrawer.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (openDrawer != null) {
                IconButton(onClick = openDrawer) {
                    Icon(Icons.Filled.Menu, contentDescription = "Deschide meniu")
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectSection(
    projects: List<ProjectWithProgress>,
    isLoading: Boolean,
    errorMessage: String?,
    emptyMessage: String,
    onProjectClick: (Long) -> Unit,
) {
    Text("Proiecte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    when {
        isLoading && projects.isEmpty() -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        errorMessage != null && projects.isEmpty() -> Text(errorMessage, color = MaterialTheme.colorScheme.error)
        projects.isEmpty() -> Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> projects.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onProjectClick(item.project.projectId) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.project.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                item.project.adress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${(item.progress * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
