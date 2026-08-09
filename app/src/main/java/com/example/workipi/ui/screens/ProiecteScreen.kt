package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.text.drawText
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.example.workipi.data.model.ProjectStatus
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.viewmodel.ProjectWithProgress
import com.example.workipi.viewmodel.ProjectsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

private enum class ProjectFilter { ACTIVE, INCHEIATE, TOATE }
private enum class GanttView { DAY, WEEK, MONTH }

// Coloanele sortabile ale tabelului, in aceeasi ordine ca labelurile din TableHeader.
private enum class ProjectSort { NAME, PROGRESS, SURFACE, MP_PER_DAY, REVISIONS, RISK }
private val sortColumns = listOf(
    ProjectSort.NAME, ProjectSort.PROGRESS, ProjectSort.SURFACE,
    ProjectSort.MP_PER_DAY, ProjectSort.REVISIONS, ProjectSort.RISK,
)

@Composable
fun ProjectsScreen(
    navController: NavController,
    viewModel: ProjectsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val openDrawer = LocalOpenDrawer.current
    var filter by remember { mutableStateOf(ProjectFilter.ACTIVE) }
    var projectToDelete by remember { mutableStateOf<ProjectWithProgress?>(null) }
    var sortColumn by remember { mutableStateOf<ProjectSort?>(null) }
    var sortAsc by remember { mutableStateOf(true) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadProjects() }

    val filtered = state.projects.filter { item ->
        val status = computeStatus(item.progress, item.project.endDate, item.project.isOffer)
        when (filter) {
            ProjectFilter.ACTIVE    -> status == ProjectStatus.ACTIV || status == ProjectStatus.INTARZIAT
            ProjectFilter.INCHEIATE -> status == ProjectStatus.FINALIZAT
            ProjectFilter.TOATE     -> true
        }
    }

    val sorted = remember(filtered, sortColumn, sortAsc) {
        val base = when (sortColumn) {
            null                    -> filtered
            ProjectSort.NAME        -> filtered.sortedBy { it.project.title.lowercase() }
            ProjectSort.PROGRESS    -> filtered.sortedBy { it.progress }
            ProjectSort.SURFACE     -> filtered.sortedBy { it.completedSurface }
            ProjectSort.MP_PER_DAY  -> filtered.sortedBy { it.mpPerDay }
            ProjectSort.REVISIONS   -> filtered.sortedBy { it.revisionsToDo }
            ProjectSort.RISK        -> filtered.sortedBy { riskRank(it) }
        }
        if (sortColumn != null && !sortAsc) base.reversed() else base
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
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
                    )
                    Text(
                        text = "${state.projects.size} proiecte",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            PortfolioInsights(items = state.projects)

            GanttChartCard(items = state.projects)

            ProjectsTableCard(
                items = sorted,
                filter = filter,
                onFilterChange = { filter = it },
                onRowClick = { id ->
                    navController.navigate(Screen.ProjectDetail.createRoute(id))
                },
                onDelete = { projectToDelete = it },
                sortColumn = sortColumn,
                sortAsc = sortAsc,
                onSort = { col ->
                    if (sortColumn == col) sortAsc = !sortAsc
                    else { sortColumn = col; sortAsc = true }
                },
            )

            if (state.isLoading && state.projects.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.errorMessage != null && state.projects.isEmpty()) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate(Screen.AddProject.createRoute(offer = false)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Adauga proiect")
        }
    }

    projectToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Esti sigur?") },
            text = { Text("Proiectul \"${target.project.title}\" va fi sters definitiv.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(target.project.projectId)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) { Text("Da") }
            },
            dismissButton = {
                Button(
                    onClick = { projectToDelete = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) { Text("Nu") }
            },
        )
    }
}

@Composable
private fun PortfolioInsights(items: List<ProjectWithProgress>) {
    val active = items.count {
        val status = computeStatus(it.progress, it.project.endDate, it.project.isOffer)
        status == ProjectStatus.ACTIV || status == ProjectStatus.INTARZIAT
    }
    val delayed = items.count { riskRank(it) >= 4 }
    val completed = items.count { it.progress >= 1f }
    val totalSurface = items.sumOf { it.totalSurface.toDouble() }
    val completedSurface = items.sumOf { it.completedSurface.toDouble() }
    val portfolioProgress = if (totalSurface > 0) (completedSurface / totalSurface * 100).roundToInt() else 0

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PortfolioMetric("Proiecte active", active.toString(), "în execuție", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
        PortfolioMetric("În risc", delayed.toString(), "termen depășit / estimat", Modifier.weight(1f), MaterialTheme.colorScheme.error)
        PortfolioMetric("Finalizate", completed.toString(), "din portofoliu", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
        PortfolioMetric("Progres portofoliu", "$portfolioProgress%", "suprafață realizată", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PortfolioMetric(
    label: String,
    value: String,
    helper: String,
    modifier: Modifier,
    accent: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(width = 26.dp, height = 4.dp).clip(RoundedCornerShape(99.dp)).background(accent))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(helper, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun GanttChartCard(items: List<ProjectWithProgress>) {
    var view by remember { mutableStateOf(GanttView.MONTH) }
    var windowDayOffset by remember { mutableIntStateOf(0) }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val visibleMonths = when (view) {
        GanttView.DAY -> 1
        GanttView.WEEK -> 3
        GanttView.MONTH -> 6
    }
    val shiftMonths = when (view) {
        GanttView.DAY, GanttView.WEEK -> 1
        GanttView.MONTH -> 3
    }
    val initialStart = today.firstDayOfMonth().plusMonths(-(visibleMonths / 3))
    val windowStart = initialStart.plusDays(windowDayOffset)
    val windowEnd = windowStart.plusMonths(visibleMonths).plusDays(-1)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "Portofoliu în timp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Plan contractual, progres realizat și estimarea la ritmul curent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                GanttViewChips(view) {
                    view = it
                    windowDayOffset = 0
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        val destination = windowStart.plusMonths(-shiftMonths)
                        windowDayOffset += destination.toEpochDays() - windowStart.toEpochDays()
                    }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Perioada anterioară")
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Fereastra afișată", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${monthShort(windowStart.monthNumber)} ${windowStart.year} — ${monthShort(windowEnd.monthNumber)} ${windowEnd.year}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = {
                        val destination = windowStart.plusMonths(shiftMonths)
                        windowDayOffset += destination.toEpochDays() - windowStart.toEpochDays()
                    }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Perioada următoare")
                    }
                    VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    TextButton(onClick = { windowDayOffset = 0 }) { Text("Astăzi") }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                GanttLegend()
                Text(
                    text = "Glisează graficul spre stânga sau dreapta",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Nu sunt proiecte de afisat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                GanttCanvas(
                    items = items,
                    view = view,
                    windowStart = windowStart,
                    windowEnd = windowEnd,
                    onPanDays = { days -> windowDayOffset += days },
                )
            }
        }
    }
}

@Composable
private fun GanttViewChips(current: GanttView, onChange: (GanttView) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        GanttView.entries.forEach { v ->
            val selected = current == v
            Text(
                text = when (v) {
                    GanttView.DAY -> "Zi"
                    GanttView.WEEK -> "Săptămână"
                    GanttView.MONTH -> "Lună"
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onChange(v) }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GanttLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), "Plan")
        LegendDot(MaterialTheme.colorScheme.primary, "Realizat")
        LegendDot(MaterialTheme.colorScheme.secondary, "Estimare bună")
        LegendDot(MaterialTheme.colorScheme.error, "Estimare întârziată")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GanttCanvas(
    items: List<ProjectWithProgress>,
    view: GanttView,
    windowStart: LocalDate,
    windowEnd: LocalDate,
    onPanDays: (Int) -> Unit,
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    data class Row(
        val label: String,
        val start: LocalDate,
        val plannedEnd: LocalDate,
        val estimatedEnd: LocalDate?,
        val progress: Float,
        val noPace: Boolean,
    )
    val rows = items.map { item ->
        val rate = item.mpPerDay
        val remaining = (item.totalSurface - item.completedSurface).coerceAtLeast(0f)
        val estimated = if (item.progress >= 1f || rate <= 0f || item.totalSurface <= 0f) null
        else today.plusDays((remaining / rate).toInt())
        Row(
            label = item.project.title,
            start = item.project.startDate.toLocalDateTime(TimeZone.currentSystemDefault()).date,
            plannedEnd = item.project.endDate,
            estimatedEnd = estimated,
            progress = item.progress,
            noPace = item.progress < 1f && rate <= 0f,
        )
    }.sortedBy { it.start }

    val totalDays = (windowEnd.toEpochDays() - windowStart.toEpochDays()).coerceAtLeast(1)
    val renderStart = windowStart.plusDays(-totalDays)
    val renderEnd = windowEnd.plusDays(totalDays)
    val labelColWidth = 190.dp
    val axisHeight = 38.dp
    val rowHeight = 48.dp
    val barHeight = 12.dp
    val chartHeight = axisHeight + (rowHeight.value * rows.size).dp
    val currentOnPanDays by rememberUpdatedState(onPanDays)

    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val dayGridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
    val todayColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val axisColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    val laneColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
    val monthStripeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.035f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val axisLabelStyle = MaterialTheme.typography.labelSmall.copy(color = mutedColor, fontWeight = FontWeight.SemiBold)
    val tinyStyle = androidx.compose.ui.text.TextStyle(fontSize = 9.sp, color = mutedColor, fontWeight = FontWeight.Medium)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .clip(RoundedCornerShape(15.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.52f), RoundedCornerShape(15.dp))
            .pointerInput(view) {
                var pendingDays = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        if (size.width > 0) {
                            pendingDays += (-dragAmount / size.width) * totalDays
                            val wholeDays = pendingDays.toInt()
                            if (wholeDays != 0) {
                                currentOnPanDays(wholeDays)
                                pendingDays -= wholeDays
                            }
                        }
                    },
                    onDragEnd = { pendingDays = 0f },
                    onDragCancel = { pendingDays = 0f },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .width(labelColWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(axisHeight)
                    .background(axisColor)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("PROIECT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = mutedColor)
            }
            rows.forEachIndexed { index, r ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .background(if (index % 2 == 1) laneColor else Color.Transparent)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = r.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (r.noPace) "${(r.progress * 100).roundToInt()}% · fără ritm" else "${(r.progress * 100).roundToInt()}% realizat",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (r.noPace) mutedColor else primaryColor,
                    )
                }
            }
        }

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(surfaceColor)
        ) {
            val w = size.width
            val h = size.height
            val axisH = axisHeight.toPx()
            val plotTop = axisH
            val plotBottom = h
            val dayPx = w / totalDays
            val labelEvery = when {
                dayPx >= 14f -> 1
                dayPx >= 7f  -> 2
                dayPx >= 4f  -> 5
                dayPx >= 2f  -> 10
                else         -> 0 // prea dens — nu afisez zile, doar lunile
            }

            fun xFor(date: LocalDate): Float {
                val d = (date.toEpochDays() - windowStart.toEpochDays()).toFloat()
                return d / totalDays * w
            }

            fun clipped(date: LocalDate): LocalDate = when {
                date < renderStart -> renderStart
                date > renderEnd -> renderEnd
                else -> date
            }

            fun intersects(start: LocalDate, end: LocalDate): Boolean = start <= renderEnd && end >= renderStart

            drawRect(color = axisColor, topLeft = androidx.compose.ui.geometry.Offset.Zero, size = androidx.compose.ui.geometry.Size(w, axisH))

            rows.indices.forEach { index ->
                if (index % 2 == 1) {
                    drawRect(
                        color = laneColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, plotTop + index * rowHeight.toPx()),
                        size = androidx.compose.ui.geometry.Size(w, rowHeight.toPx()),
                    )
                }
            }

            var stripeMonth = renderStart.firstDayOfMonth()
            var stripeIndex = 0
            while (stripeMonth <= renderEnd) {
                val nextMonth = stripeMonth.plusMonths(1)
                if (stripeIndex % 2 == 0) {
                    drawRect(
                        color = monthStripeColor,
                        topLeft = androidx.compose.ui.geometry.Offset(xFor(stripeMonth), plotTop),
                        size = androidx.compose.ui.geometry.Size(xFor(nextMonth) - xFor(stripeMonth), plotBottom - plotTop),
                    )
                }
                stripeMonth = nextMonth
                stripeIndex++
            }

            when (view) {
                GanttView.DAY -> {
                    var d = renderStart
                    var index = 0
                    while (d <= renderEnd) {
                        val x = xFor(d)
                        drawLine(
                            color = dayGridColor,
                            start = androidx.compose.ui.geometry.Offset(x, plotTop),
                            end = androidx.compose.ui.geometry.Offset(x, plotBottom),
                            strokeWidth = 1f,
                        )
                        if (labelEvery > 0 && index % labelEvery == 0) {
                            val layout = textMeasurer.measure(
                                text = androidx.compose.ui.text.AnnotatedString(d.dayOfMonth.toString()),
                                style = tinyStyle,
                            )
                            drawText(
                                textLayoutResult = layout,
                                topLeft = androidx.compose.ui.geometry.Offset(x + 4f, 11f),
                            )
                        }
                        d = d.plusDays(1)
                        index++
                    }
                }
                GanttView.WEEK -> {
                    var weekIndex = 1
                    var d = renderStart
                    while (d <= renderEnd) {
                        val x = xFor(d)
                        drawLine(
                            color = dayGridColor,
                            start = androidx.compose.ui.geometry.Offset(x, plotTop),
                            end = androidx.compose.ui.geometry.Offset(x, plotBottom),
                            strokeWidth = 1f,
                        )
                        val layout = textMeasurer.measure(
                            text = androidx.compose.ui.text.AnnotatedString("S$weekIndex"),
                            style = tinyStyle,
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = androidx.compose.ui.geometry.Offset(x + 4f, 11f),
                        )
                        weekIndex++
                        d = d.plusDays(7)
                    }
                }
                GanttView.MONTH -> {
                    Unit
                }
            }

            var cur = renderStart.firstDayOfMonth()
            while (cur <= renderEnd) {
                val x = xFor(cur)
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(x, plotTop),
                    end = androidx.compose.ui.geometry.Offset(x, plotBottom),
                    strokeWidth = 1.2f,
                )
                if (view == GanttView.MONTH) {
                    val layout = textMeasurer.measure(
                        text = androidx.compose.ui.text.AnnotatedString("${monthShort(cur.monthNumber)} ${cur.year}"),
                        style = axisLabelStyle,
                    )
                    drawText(textLayoutResult = layout, topLeft = androidx.compose.ui.geometry.Offset(x + 8f, 10f))
                }
                cur = cur.plusMonths(1)
            }

            if (today in renderStart..renderEnd) {
                val xToday = xFor(today)
                drawRect(
                    color = todayColor,
                    topLeft = androidx.compose.ui.geometry.Offset(xToday - 3f, plotTop),
                    size = androidx.compose.ui.geometry.Size(6f, plotBottom - plotTop),
                )
                val todayLayout = textMeasurer.measure(androidx.compose.ui.text.AnnotatedString("AZI"), axisLabelStyle.copy(color = primaryColor, fontWeight = FontWeight.Bold))
                drawText(todayLayout, topLeft = androidx.compose.ui.geometry.Offset(xToday + 5f, 10f))
            }

            val barPx = barHeight.toPx()
            val rowPx = rowHeight.toPx()
            rows.forEachIndexed { i, r ->
                val yCenter = plotTop + i * rowPx + rowPx / 2f
                val yTop = yCenter - barPx / 2f
                if (intersects(r.start, r.plannedEnd)) {
                    val xStart = xFor(clipped(r.start))
                    val xPlanned = xFor(clipped(r.plannedEnd))
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.22f),
                        topLeft = androidx.compose.ui.geometry.Offset(xStart, yTop),
                        size = androidx.compose.ui.geometry.Size((xPlanned - xStart).coerceAtLeast(2f), barPx),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barPx / 2f, barPx / 2f),
                    )

                    val totalPlanDays = (r.plannedEnd.toEpochDays() - r.start.toEpochDays()).coerceAtLeast(1)
                    val completedDate = r.start.plusDays((totalPlanDays * r.progress.coerceIn(0f, 1f)).roundToInt())
                    if (intersects(r.start, completedDate)) {
                        val xProgressEnd = xFor(clipped(completedDate))
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = androidx.compose.ui.geometry.Offset(xStart, yTop),
                            size = androidx.compose.ui.geometry.Size((xProgressEnd - xStart).coerceAtLeast(2f), barPx),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barPx / 2f, barPx / 2f),
                        )
                    }
                    if (r.plannedEnd in renderStart..renderEnd) {
                        val plannedX = xFor(r.plannedEnd)
                        drawLine(color = primaryColor.copy(alpha = 0.75f), start = androidx.compose.ui.geometry.Offset(plannedX, yTop - 5f), end = androidx.compose.ui.geometry.Offset(plannedX, yTop + barPx + 5f), strokeWidth = 2f)
                    }
                }

                val est = r.estimatedEnd
                if (est != null && est != r.plannedEnd) {
                    val forecastColor = if (est > r.plannedEnd) errorColor else secondaryColor
                    if (intersects(r.plannedEnd, est)) {
                        val plannedX = xFor(clipped(r.plannedEnd))
                        val estimateX = xFor(clipped(est))
                        drawLine(
                            color = forecastColor,
                            start = androidx.compose.ui.geometry.Offset(plannedX, yCenter),
                            end = androidx.compose.ui.geometry.Offset(estimateX, yCenter),
                            strokeWidth = 3f,
                        )
                        if (est in renderStart..renderEnd) {
                            drawCircle(color = forecastColor, radius = 5f, center = androidx.compose.ui.geometry.Offset(estimateX, yCenter))
                        } else {
                            drawCircle(color = forecastColor, radius = 4f, center = androidx.compose.ui.geometry.Offset(if (est > renderEnd) w - 4f else 4f, yCenter))
                        }
                    }
                }
            }
        }
    }
}

private fun LocalDate.plusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(this.toEpochDays() + days)

private fun LocalDate.firstDayOfMonth(): LocalDate = LocalDate(year, monthNumber, 1)

private fun LocalDate.plusMonths(months: Int): LocalDate {
    val totalMonths = this.year * 12 + (this.monthNumber - 1) + months
    val newYear = totalMonths / 12
    val newMonth = totalMonths % 12 + 1
    val dom = minOf(this.dayOfMonth, lastDayOfMonth(newYear, newMonth))
    return LocalDate(newYear, newMonth, dom)
}

private fun lastDayOfMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 28
}

private fun monthShort(m: Int): String = listOf(
    "IAN", "FEB", "MAR", "APR", "MAI", "IUN", "IUL", "AUG", "SEP", "OCT", "NOI", "DEC"
)[m - 1]

@Composable
private fun CalendarPlaceholderCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Calendar proiecte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "View by day / weeks / months",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Grafic Gantt — in curand",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectsTableCard(
    items: List<ProjectWithProgress>,
    filter: ProjectFilter,
    onFilterChange: (ProjectFilter) -> Unit,
    onRowClick: (Long) -> Unit,
    onDelete: (ProjectWithProgress) -> Unit,
    sortColumn: ProjectSort?,
    sortAsc: Boolean,
    onSort: (ProjectSort) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "Lista proiecte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Deschide un proiect pentru detalii și control operațional.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilterChips(filter, onFilterChange)
            }

            Column(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState()),
            ) {
                TableHeader(sortColumn = sortColumn, sortAsc = sortAsc, onSort = onSort)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Niciun proiect in aceasta categorie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items.forEach { item ->
                        TableRow(
                            item = item,
                            onClick = { onRowClick(item.project.projectId) },
                            onDelete = { onDelete(item) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChips(filter: ProjectFilter, onChange: (ProjectFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ProjectFilter.entries.forEach { f ->
            FilterChip(
                selected = filter == f,
                onClick = { onChange(f) },
                label = {
                    Text(
                        text = when (f) {
                            ProjectFilter.ACTIVE -> "ACTIVE"
                            ProjectFilter.INCHEIATE -> "INCHEIATE"
                            ProjectFilter.TOATE -> "TOATE"
                        },
                        fontSize = 11.sp,
                    )
                },
            )
        }
    }
}

// Latimi fixe pe coloane (in dp) — totalizeaza ~840dp; pe ecran mic scroll orizontal
private val colWidths = listOf(180.dp, 120.dp, 140.dp, 110.dp, 110.dp, 180.dp)
private val deleteColWidth = 56.dp

@Composable
private fun TableHeader(
    sortColumn: ProjectSort?,
    sortAsc: Boolean,
    onSort: (ProjectSort) -> Unit,
) {
    val labels = listOf(
        "Nume proiect", "Procent progres", "Mp realizati / total",
        "Medie Mp/zi", "Nr. revizii", "Riscuri"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val col = sortColumns[i]
            Row(
                modifier = Modifier
                    .width(colWidths[i])
                    .clickable { onSort(col) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (sortColumn == col) {
                    Icon(
                        imageVector = if (sortAsc) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = if (sortAsc) "crescator" else "descrescator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(14.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(deleteColWidth))
    }
}

@Composable
private fun TableRow(item: ProjectWithProgress, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(colWidths[0]), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = item.project.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            StatusBadge(computeStatus(item.progress, item.project.endDate, item.project.isOffer))
        }
        Column(modifier = Modifier.width(colWidths[1]), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(text = "${(item.progress * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        Text(
            text = "${item.completedSurface.toInt()} / ${item.totalSurface.toInt()} mp",
            modifier = Modifier.width(colWidths[2]),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "${item.mpPerDay.toInt()} mp/zi",
            modifier = Modifier.width(colWidths[3]),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (item.revisionsToDo == 0) "—" else item.revisionsToDo.toString(),
            modifier = Modifier.width(colWidths[4]),
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(modifier = Modifier.width(colWidths[5])) {
            RiskBadge(item)
        }
        Box(
            modifier = Modifier.width(deleteColWidth),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Sterge proiect",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun RiskBadge(item: ProjectWithProgress) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val start = item.project.startDate.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val rate = item.mpPerDay
    val remaining = (item.totalSurface - item.completedSurface).coerceAtLeast(0f)
    val estimatedEnd = if (item.progress >= 1f || rate <= 0f || item.totalSurface <= 0f) null
        else today.plusDays((remaining / rate).toInt())

    val (label, bg, fg) = when {
        start > today -> Triple("Start santier in curand", Color(0xFFE0E0E0), Color(0xFF424242))
        item.progress >= 1f -> Triple("Finalizat", Color(0xFFE3F2FD), Color(0xFF1565C0))
        item.project.endDate < today -> Triple("Termen finalizare intarziat", Color(0xFFFFEBEE), Color(0xFFC62828))
        estimatedEnd == null -> Triple("Fara date suficiente", Color(0xFFE0E0E0), Color(0xFF424242))
        estimatedEnd > item.project.endDate -> Triple("Termen finalizare intarziat", Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Triple("Termen finalizare mai rapid", Color(0xFFE8F5E9), Color(0xFF2E7D32))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

// Rang de risc pentru sortare (mai mare = mai riscant) — aceeasi logica ca RiskBadge.
private fun riskRank(item: ProjectWithProgress): Int {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val start = item.project.startDate.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val rate = item.mpPerDay
    val remaining = (item.totalSurface - item.completedSurface).coerceAtLeast(0f)
    val estimatedEnd = if (item.progress >= 1f || rate <= 0f || item.totalSurface <= 0f) null
        else today.plusDays((remaining / rate).toInt())
    return when {
        start > today                        -> 1
        item.progress >= 1f                  -> 0
        item.project.endDate < today         -> 4
        estimatedEnd == null                 -> 2
        estimatedEnd > item.project.endDate  -> 4
        else                                 -> 3
    }
}

internal fun computeStatus(progress: Float, endDate: LocalDate, isOffer: Boolean = false): ProjectStatus {
    if (isOffer) return ProjectStatus.OFERTA
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
        ProjectStatus.OFERTA    -> Triple("Oferta",    Color(0xFFFFF3E0), Color(0xFFE65100))
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
