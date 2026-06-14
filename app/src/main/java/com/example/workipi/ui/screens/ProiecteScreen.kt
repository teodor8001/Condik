package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.text.drawText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
private fun GanttChartCard(items: List<ProjectWithProgress>) {
    var view by remember { mutableStateOf(GanttView.DAY) }
    Card(
        modifier = Modifier
            .fillMaxWidth(),
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
                    text = "Calendar proiecte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GanttLegend()
                    Spacer(Modifier.width(4.dp))
                    GanttViewChips(view) { view = it }
                }
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
                GanttCanvas(items = items, view = view)
            }
        }
    }
}

@Composable
private fun GanttViewChips(current: GanttView, onChange: (GanttView) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GanttView.entries.forEach { v ->
            FilterChip(
                selected = current == v,
                onClick = { onChange(v) },
                label = {
                    Text(
                        text = when (v) {
                            GanttView.DAY -> "Zi"
                            GanttView.WEEK -> "Saptamana"
                            GanttView.MONTH -> "Luna"
                        },
                        fontSize = 11.sp,
                    )
                },
            )
        }
    }
}

@Composable
private fun GanttLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(Color.Black, "Planificat")
        LegendDot(Color(0xFF2E7D32), "Mai rapid")
        LegendDot(Color(0xFFC62828), "Intarziat")
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
private fun GanttCanvas(items: List<ProjectWithProgress>, view: GanttView) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // Calculeaza estimatedEnd per proiect; null daca nu se poate (rata 0 sau finalizat)
    data class Row(
        val label: String,
        val start: LocalDate,
        val plannedEnd: LocalDate,
        val estimatedEnd: LocalDate?,
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
        )
    }.sortedBy { it.start }

    val minDate = rows.minOf { it.start }
    val maxDate = rows.maxOf { maxOf(it.plannedEnd, it.estimatedEnd ?: it.plannedEnd) }
    val totalDays = (maxDate.toEpochDays() - minDate.toEpochDays()).coerceAtLeast(1)

    val labelColWidth = 110.dp
    val rowHeight = 30.dp
    val barHeight = 10.dp
    val chartHeight = (rowHeight.value * rows.size + 52f).dp // +52 pentru axa zilelor + lunilor

    val labelColor = MaterialTheme.colorScheme.onBackground
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val dayGridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val todayColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val tinyStyle = androidx.compose.ui.text.TextStyle(fontSize = 8.sp, color = mutedColor)

    Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
        // Coloana cu numele proiectelor
        Column(modifier = Modifier.width(labelColWidth)) {
            rows.forEach { r ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = r.label,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Canvas-ul cu bare + axa
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val w = size.width
            val h = size.height
            val axisH = 52f
            val plotH = h - axisH
            val dayPx = w / totalDays
            val labelEvery = when {
                dayPx >= 14f -> 1
                dayPx >= 7f  -> 2
                dayPx >= 4f  -> 5
                dayPx >= 2f  -> 10
                else         -> 0 // prea dens — nu afisez zile, doar lunile
            }

            fun xFor(date: LocalDate): Float {
                val d = (date.toEpochDays() - minDate.toEpochDays()).toFloat()
                return d / totalDays * w
            }

            when (view) {
                GanttView.DAY -> {
                    // Linii verticale subtiri per zi + numar zi
                    var d = minDate
                    while (d <= maxDate) {
                        val x = xFor(d)
                        drawLine(
                            color = dayGridColor,
                            start = androidx.compose.ui.geometry.Offset(x, 0f),
                            end = androidx.compose.ui.geometry.Offset(x, plotH),
                            strokeWidth = 1.5f,
                        )
                        if (labelEvery > 0 && (d.dayOfMonth - 1) % labelEvery == 0) {
                            val layout = textMeasurer.measure(
                                text = androidx.compose.ui.text.AnnotatedString(d.dayOfMonth.toString()),
                                style = tinyStyle,
                            )
                            drawText(
                                textLayoutResult = layout,
                                topLeft = androidx.compose.ui.geometry.Offset(x + 1f, plotH + 2f),
                            )
                        }
                        d = d.plusDays(1)
                    }
                }
                GanttView.WEEK -> {
                    // Linii verticale + eticheta W1, W2 ... la fiecare 7 zile pornind de la minDate
                    var weekIndex = 1
                    var d = minDate
                    while (d <= maxDate) {
                        val x = xFor(d)
                        drawLine(
                            color = dayGridColor,
                            start = androidx.compose.ui.geometry.Offset(x, 0f),
                            end = androidx.compose.ui.geometry.Offset(x, plotH),
                            strokeWidth = 1.5f,
                        )
                        val layout = textMeasurer.measure(
                            text = androidx.compose.ui.text.AnnotatedString("W$weekIndex"),
                            style = tinyStyle,
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = androidx.compose.ui.geometry.Offset(x + 2f, plotH + 2f),
                        )
                        weekIndex++
                        d = d.plusDays(7)
                    }
                }
                GanttView.MONTH -> {
                    // Doar separatori la inceput de luna — eticheta lunii e desenata mai jos
                }
            }

            // Grid vertical mai vizibil + eticheta luna la inceput de luna (mereu vizibil)
            var cur = LocalDate(minDate.year, minDate.month, 1)
            if (cur < minDate) cur = cur.plusMonths(1)
            while (cur <= maxDate) {
                val x = xFor(cur)
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, plotH),
                    strokeWidth = if (view == GanttView.MONTH) 1.5f else 1.2f,
                )
                val monthLabelY = if (view == GanttView.MONTH) plotH + 2f else plotH + 18f
                val layout = textMeasurer.measure(
                    text = androidx.compose.ui.text.AnnotatedString(monthShort(cur.monthNumber)),
                    style = labelStyle.copy(color = mutedColor, fontWeight = FontWeight.SemiBold),
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = androidx.compose.ui.geometry.Offset(x + 4f, monthLabelY),
                )
                cur = cur.plusMonths(1)
            }

            // Linia verticala pentru ziua de azi (daca e in range)
            if (today in minDate..maxDate) {
                val xToday = xFor(today)
                drawRect(
                    color = todayColor,
                    topLeft = androidx.compose.ui.geometry.Offset(xToday - 4f, 0f),
                    size = androidx.compose.ui.geometry.Size(8f, plotH),
                )
            }

            // Barele
            val barPx = barHeight.toPx()
            val rowPx = rowHeight.toPx()
            rows.forEachIndexed { i, r ->
                val yCenter = i * rowPx + rowPx / 2f
                val yTop = yCenter - barPx / 2f
                val xStart = xFor(r.start)
                val xPlanned = xFor(r.plannedEnd)

                // Linia neagra: start -> plannedEnd
                drawRect(
                    color = Color.Black,
                    topLeft = androidx.compose.ui.geometry.Offset(xStart, yTop),
                    size = androidx.compose.ui.geometry.Size((xPlanned - xStart).coerceAtLeast(1f), barPx),
                )

                // Coada
                val est = r.estimatedEnd
                if (est != null && est != r.plannedEnd) {
                    val xEst = xFor(est)
                    if (est > r.plannedEnd) {
                        // Rosu: plannedEnd -> estimatedEnd (extindere la dreapta)
                        drawRect(
                            color = Color(0xFFC62828),
                            topLeft = androidx.compose.ui.geometry.Offset(xPlanned, yTop),
                            size = androidx.compose.ui.geometry.Size((xEst - xPlanned), barPx),
                        )
                    } else {
                        // Verde: estimatedEnd -> plannedEnd suprapus peste segmentul final negru
                        drawRect(
                            color = Color(0xFF2E7D32),
                            topLeft = androidx.compose.ui.geometry.Offset(xEst, yTop),
                            size = androidx.compose.ui.geometry.Size((xPlanned - xEst), barPx),
                        )
                    }
                }
            }
        }
    }
}

private fun LocalDate.plusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(this.toEpochDays() + days)

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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Lista proiecte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
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
    Row(modifier = Modifier.padding(vertical = 10.dp)) {
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
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.project.title,
            modifier = Modifier.width(colWidths[0]),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${(item.progress * 100).roundToInt()}%",
            modifier = Modifier.width(colWidths[1]),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "${item.completedSurface.toInt()} / ${item.totalSurface.toInt()} mp",
            modifier = Modifier.width(colWidths[2]),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "${item.mpPerDay.toInt()} mp",
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
