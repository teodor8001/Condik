package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.ProjectStatus
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.viewmodel.ProjectWithProgress
import com.example.workipi.viewmodel.ProjectsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProjectsScreen(
    navController: NavController,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val openDrawer = LocalOpenDrawer.current

    // Refresh la revenirea pe ecran (dupa Add Project)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadProjects() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isTabletLandscape = maxWidth > 600.dp
        val columns = if (isTabletLandscape) 2 else 1
        val padding = if (isTabletLandscape) 32.dp else 20.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padding)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (openDrawer != null) {
                    IconButton(onClick = { openDrawer() }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Deschide meniu",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Proiecte",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${state.projects.size} proiecte",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                state.isLoading && state.projects.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.errorMessage != null && state.projects.isEmpty() -> {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                state.projects.isEmpty() -> {
                    Text(
                        text = "Nu ai inca proiecte. Apasa + sa adaugi unul.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.projects) { item ->
                            ProjectCard(
                                item = item,
                                onClick = {
                                    navController.navigate(
                                        Screen.ProjectDetail.createRoute(item.project.projectId)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate(Screen.AddProject.route) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Adauga proiect")
        }
    }
}

@Composable
private fun ProjectCard(item: ProjectWithProgress, onClick: () -> Unit) {
    val project = item.project
    val ronFormatter = NumberFormat.getNumberInstance(Locale("ro", "RO")).apply {
        maximumFractionDigits = 0
    }
    val progress = item.progress

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatusBadge(status = computeStatus(progress, project.endDate))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${formatDate(project.startDate)}  →  ${formatDate(project.endDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progres",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Buget:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = project.budget?.let { "${ronFormatter.format(it)} RON" } ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

internal fun computeStatus(progress: Float, endDate: LocalDate): ProjectStatus {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return when {
        progress >= 1.0f -> ProjectStatus.FINALIZAT
        endDate < today -> ProjectStatus.INTARZIAT
        else -> ProjectStatus.ACTIV
    }
}

@Composable
internal fun StatusBadge(status: ProjectStatus) {
    val (label, bgColor, textColor) = when (status) {
        ProjectStatus.ACTIV     -> Triple("Activ",     Color(0xFFE8F5E9), Color(0xFF2E7D32))
        ProjectStatus.INTARZIAT -> Triple("Intarziat", Color(0xFFFFEBEE), Color(0xFFC62828))
        ProjectStatus.FINALIZAT -> Triple("Finalizat", Color(0xFFE3F2FD), Color(0xFF1565C0))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

internal fun formatDate(instant: kotlinx.datetime.Instant): String {
    val ld = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${ld.dayOfMonth.toString().padStart(2, '0')}.${ld.monthNumber.toString().padStart(2, '0')}.${ld.year}"
}

internal fun formatDate(date: kotlinx.datetime.LocalDate): String =
    "${date.dayOfMonth.toString().padStart(2, '0')}.${date.monthNumber.toString().padStart(2, '0')}.${date.year}"
