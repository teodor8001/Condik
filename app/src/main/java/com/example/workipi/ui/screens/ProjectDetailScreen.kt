package com.example.workipi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workipi.viewmodel.HistoryViewModel
import com.example.workipi.ui.theme.CARD_SPACING
import com.example.workipi.ui.theme.CORNER_SHAPE
import com.example.workipi.ui.theme.ELEVATION
import com.example.workipi.ui.theme.LARGER_SPACING
import com.example.workipi.ui.theme.MIDDLE_SECTION_HEIGHT
import com.example.workipi.ui.theme.NORMAL_SPACING
import com.example.workipi.ui.theme.PADDING
import com.example.workipi.ui.theme.PROGRESS_BAR_HEIGHT
import com.example.workipi.ui.theme.SCREEN_PADDING
import com.example.workipi.ui.theme.generalUiComponents.InformationCard
import com.example.workipi.viewmodel.AverageWorkGraphic
import com.example.workipi.viewmodel.MixLucrareItem
import com.example.workipi.viewmodel.ProjectDetailScreenUi
import com.example.workipi.viewmodel.ProjectDetailViewModel
import com.example.workipi.viewmodel.TeamMemberItem
import com.example.workipi.viewmodel.ZoneItem
import kotlin.math.roundToInt

private enum class DetailDialog { MIX, TEAM, PONTARE, ZONE }

@Composable
fun ProjectDetailScreen(
    navController: NavController,
    projectId: Long,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) { viewModel.load(projectId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SCREEN_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
        ) {
            Text(
                text = "Detalii proiect",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            val error = state.error
            if (error != null) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
            } else {
                DetailPanel(
                    state = state,
                    projectId = projectId,
                    onReload = { viewModel.load(projectId) },
                )
            }
        }
    }
}

@Composable
private fun DetailPanel(
    state: ProjectDetailScreenUi,
    projectId: Long,
    onReload: () -> Unit,
) {
    var openDialog by remember { mutableStateOf<DetailDialog?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CORNER_SHAPE.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION.dp),
    ) {
        Column(
            modifier = Modifier.padding(SCREEN_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
        ) {
            ProjectHeader(state)

            // Rand 1 — termene
            Row(horizontalArrangement = Arrangement.spacedBy(CARD_SPACING.dp)) {
                InformationCard(
                    modifier = Modifier.weight(1f),
                    title = "Termen finalizare",
                    value = state.endDate,
                )
                InformationCard(
                    modifier = Modifier.weight(1f),
                    title = "Estimare finalizare",
                    value = state.estimatedEndDate,
                )
            }

            // Rand 2 — indicatori
            Row(horizontalArrangement = Arrangement.spacedBy(CARD_SPACING.dp)) {
                InformationCard(
                    modifier = Modifier.weight(1f),
                    title = "Mp realizati / total",
                    value = "${state.finishedQuantity.roundToInt()} / ${state.totalQuantity.roundToInt()} mp",
                )
                InformationCard(
                    modifier = Modifier.weight(1f),
                    title = "Pontari",
                    value = state.pontariCount.toString(),
                    onClick = { openDialog = DetailDialog.PONTARE },
                )
                InformationCard(
                    modifier = Modifier.weight(1f),
                    title = "Nr. revizii de facut",
                    value = "—",
                )
                InformationCard(
                    modifier = Modifier.weight(1f),
                    title = "Zone",
                    value = state.zoneItems.size.toString(),
                    onClick = { openDialog = DetailDialog.ZONE },
                )
            }

            // Rand 3 — grafic (jumatate) + mix lucrari & echipa (cealalta jumatate, deschid popup la click)
            Row(
                modifier = Modifier.height(MIDDLE_SECTION_HEIGHT.dp),
                horizontalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
            ) {
                WorkGraphCard(
                    points = state.graphPoints,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                ClickableSectionCard(
                    title = "Mix lucrari",
                    subtitle = "suprafete principale",
                    onClick = { openDialog = DetailDialog.MIX },
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                ) {
                    if (state.mixLucrari.isEmpty()) {
                        EmptyHint("Nicio lucrare alocata")
                    } else {
                        state.mixLucrari.take(2).forEach { MixLucrareRow(it) }
                    }
                }
                ClickableSectionCard(
                    title = "Echipa curenta",
                    onClick = { openDialog = DetailDialog.TEAM },
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight(),
                ) {
                    Text(
                        text = "${state.team.size} angajati",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${state.teamSalaryTotal.roundToInt()} RON salarii / luna",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Rand 4 — financiar
            Row(horizontalArrangement = Arrangement.spacedBy(CARD_SPACING.dp)) {
                InformationCard(
                    modifier = Modifier.weight(1f),
                    title = "Profit anticipat",
                    value = "${state.possibleGains.roundToInt()} RON",
                )
                InformationCard(
                    modifier = Modifier.weight(1f),
                    title = "Buget ofertat",
                    value = "${state.budget.roundToInt()} RON",
                )
                InformationCard(
                    modifier = Modifier.weight(1f),
                    title = "Costuri proiect",
                    value = "${state.projectCosts.roundToInt()} RON",
                )
            }
        }
    }

    when (openDialog) {
        DetailDialog.MIX -> ListPopupDialog(
            title = "Mix lucrari",
            subtitle = "suprafete principale",
            onDismiss = { openDialog = null },
        ) {
            if (state.mixLucrari.isEmpty()) {
                EmptyHint("Nicio lucrare alocata acestui proiect")
            } else {
                state.mixLucrari.forEach { MixLucrareRow(it) }
            }
        }
        DetailDialog.TEAM -> ListPopupDialog(
            title = "Echipa curenta",
            subtitle = "${state.team.size} angajati • ${state.teamSalaryTotal.roundToInt()} RON salarii / luna",
            onDismiss = { openDialog = null },
        ) {
            if (state.team.isEmpty()) {
                EmptyHint("Niciun angajat alocat acestui proiect")
            } else {
                TeamTable(state.team)
            }
        }
        DetailDialog.ZONE -> ListPopupDialog(
            title = "Zone",
            subtitle = "progres pe fiecare zona",
            onDismiss = { openDialog = null },
        ) {
            if (state.zoneItems.isEmpty()) {
                EmptyHint("Proiectul nu are zone definite")
            } else {
                state.zoneItems.forEach { ZoneRow(it) }
            }
        }
        DetailDialog.PONTARE -> PontarePopup(
            team = state.team,
            projectId = projectId,
            onDismiss = { openDialog = null },
            onSaved = onReload,
        )
        null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PontarePopup(
    team: List<TeamMemberItem>,
    projectId: Long,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var selectedUserId by remember { mutableStateOf(team.firstOrNull()?.userId) }
    var employeeMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) { viewModel.load(projectId) }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            onSaved()
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Pontare",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (team.isEmpty()) {
                        EmptyHint("Aloca intai angajati proiectului ca sa poti ponta.")
                    } else {
                        val selectedName = team.firstOrNull { it.userId == selectedUserId }?.name
                            ?: "Alege angajat"
                        ExposedDropdownMenuBox(
                            expanded = employeeMenu,
                            onExpandedChange = { employeeMenu = it },
                        ) {
                            OutlinedTextField(
                                value = selectedName,
                                onValueChange = {},
                                readOnly = true,
                                textStyle = MaterialTheme.typography.bodyMedium,
                                label = { Text("Angajat") },
                                trailingIcon = {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(10.dp),
                            )
                            ExposedDropdownMenu(
                                expanded = employeeMenu,
                                onDismissRequest = { employeeMenu = false },
                            ) {
                                team.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text(member.name) },
                                        onClick = {
                                            selectedUserId = member.userId
                                            employeeMenu = false
                                        },
                                    )
                                }
                            }
                        }

                        PontareFormBody(
                            state = state,
                            onSelectZone = viewModel::selectZone,
                            onSelectSkill = viewModel::selectSkill,
                            onQuantityChange = viewModel::onQuantityChange,
                            onHoursChange = viewModel::onHoursChange,
                            onPickDate = { showDatePicker = true },
                            onSubmit = { selectedUserId?.let { viewModel.submit(it) } },
                            focusManager = focusManager,
                            submitEnabled = selectedUserId != null,
                            compact = true,
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Inchide",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        PontareDatePicker(
            initialMillis = state.workDateMillis,
            onConfirm = { viewModel.onWorkDateChange(it) },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun ProjectHeader(state: ProjectDetailScreenUi) {
    Column(verticalArrangement = Arrangement.spacedBy(LARGER_SPACING.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(NORMAL_SPACING.dp)) {
                Text(
                    text = state.name.ifBlank { "Denumire" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = state.address.ifBlank { "Adresa" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(state.status)
        }
        ProgressBar(state.progressPercent)
    }
}

@Composable
private fun ProgressBar(percent: Int) {
    val fraction = (percent / 100f).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PROGRESS_BAR_HEIGHT.dp)
            .clip(RoundedCornerShape(LARGER_SPACING.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = "Procent progres  $percent%",
            modifier = Modifier.padding(start = PADDING.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun WorkGraphCard(
    points: List<AverageWorkGraphic>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(CORNER_SHAPE.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PADDING.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LARGER_SPACING.dp),
        ) {
            Text(
                text = "Grafic mp realizati / zi",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (points.size < 2) {
                    Text(
                        text = "Date insuficiente pentru grafic",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    WorkChart(
                        points = points,
                        lineColor = MaterialTheme.colorScheme.primary,
                        gridColor = GridPurple,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Text(
                text = "Afiseaza media mp realizati / zi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun WorkChart(
    points: List<AverageWorkGraphic>,
    lineColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxQuantity = points.maxOf { it.quantity }.coerceAtLeast(1.0)
    Canvas(modifier = modifier) {
        val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
        fun offsetAt(i: Int): Offset {
            val x = i * stepX
            val y = size.height - (points[i].quantity / maxQuantity * size.height).toFloat()
            return Offset(x, y)
        }

        // gridlines verticale per zi
        points.indices.forEach { i ->
            val x = i * stepX
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2f,
            )
        }

        // linia mediei
        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = offsetAt(i),
                end = offsetAt(i + 1),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Card cu titlu + subtitlu + continut, intreg clickabil (deschide popup-ul cu lista completa). */
@Composable
private fun ClickableSectionCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(CORNER_SHAPE.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(LARGER_SPACING.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

/** Popup centrat cu lista completa. Se inchide din X, tap in afara sau back. */
@Composable
private fun ListPopupDialog(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    content()
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Inchide",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneRow(item: ZoneItem) {
    ItemRow {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${item.percent}% finalizat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${item.completedQuantity.roundToInt()} / ${item.totalQuantity.roundToInt()} mp",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MixLucrareRow(item: MixLucrareItem) {
    ItemRow {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "lucrare in proiect",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = item.totalQuantity.asQuantityRo(item.unit),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TeamTable(team: List<TeamMemberItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            TeamHeaderCell("Nume complet", Modifier.weight(1.5f))
            TeamHeaderCell("Salariu", Modifier.weight(1f))
            TeamHeaderCell("Medie mp/zi", Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        team.forEachIndexed { index, member ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = member.name,
                    modifier = Modifier.weight(1.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${member.salary.roundToInt()} RON",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${member.mpPerDay.roundToInt()} mp/zi",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (index < team.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
        }
    }
}

@Composable
private fun TeamHeaderCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ItemRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Formateaza o cantitate cu separator de mii (RO): 1600 -> "1.600 mp". */
private fun Double.asQuantityRo(unit: String): String {
    val grouped = roundToInt().toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
    return "$grouped $unit"
}

private val GridPurple = Color(0xFF9575CD)
