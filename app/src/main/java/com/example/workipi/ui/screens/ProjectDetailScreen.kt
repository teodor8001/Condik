package com.example.workipi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import com.example.workipi.data.model.Lucrare
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.ConfirmDialog
import com.example.workipi.ui.components.TimeNavLineChart
import com.example.workipi.ui.theme.generalUiComponents.InformationCard
import com.example.workipi.viewmodel.ChartSeries
import com.example.workipi.viewmodel.LucrareEntryItem
import com.example.workipi.viewmodel.MixLucrareItem
import com.example.workipi.viewmodel.PontareRowItem
import com.example.workipi.viewmodel.ProjectDetailScreenUi
import com.example.workipi.viewmodel.ProjectDetailViewModel
import com.example.workipi.viewmodel.TeamMemberItem
import com.example.workipi.viewmodel.ZoneItem
import com.example.workipi.viewmodel.ZonePickItem
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class DetailDialog { MIX, TEAM, PONTARE, ZONE }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectDetailScreen(
    navController: NavController,
    projectId: Long,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(projectId) { viewModel.load(projectId) }
    // Reincarca la revenirea pe ecran (ex. dupa ce am asignat angajati noi echipei).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load(projectId) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            userScrollEnabled = true,
        ) { page ->
            when (page) {
                0 -> DetailPage(
                    state = state,
                    projectId = projectId,
                    viewModel = viewModel,
                    onReload = { viewModel.load(projectId) },
                    onGoNext = { scope.launch { pagerState.animateScrollToPage(1) } },
                    onAssignTeam = { navController.navigate(Screen.AssignEmployees.createRoute(projectId)) },
                )
                else -> ReportsPage(
                    pontari = state.pontari,
                    onGoBack = { scope.launch { pagerState.animateScrollToPage(0) } },
                )
            }
        }

        // Buton de inchidere (X) — inapoi la lista de proiecte.
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Inapoi la proiecte",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun DetailPage(
    state: ProjectDetailScreenUi,
    projectId: Long,
    viewModel: ProjectDetailViewModel,
    onReload: () -> Unit,
    onGoNext: () -> Unit,
    onAssignTeam: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                viewModel = viewModel,
                onReload = onReload,
                onAssignTeam = onAssignTeam,
            )
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FilledTonalIconButton(onClick = onGoNext) {
                Icon(Icons.Filled.ExpandMore, contentDescription = "Vezi raportarile recente")
            }
        }
    }
}

@Composable
private fun ReportsPage(
    pontari: List<PontareRowItem>,
    onGoBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(SCREEN_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FilledTonalIconButton(onClick = onGoBack) {
                Icon(Icons.Filled.ExpandLess, contentDescription = "Inapoi la detalii")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CORNER_SHAPE.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION.dp),
        ) {
            Column(
                modifier = Modifier.padding(SCREEN_PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(LARGER_SPACING.dp),
            ) {
                Text(
                    text = "Raportari recente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Ultimele intrari din teren",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (pontari.isEmpty()) {
                    EmptyHint("Nicio pontare inca.")
                } else {
                    ReportsTable(pontari)
                }
            }
        }
    }
}

private enum class ReportSort { ANGAJAT, ORE, LUCRARE, CANTITATE, CALITATE, DATA, ZONA }

@Composable
private fun ReportsTable(pontari: List<PontareRowItem>) {
    var sortCol by remember { mutableStateOf<ReportSort?>(null) }
    var asc by remember { mutableStateOf(true) }
    val sorted = remember(pontari, sortCol, asc) {
        val base = when (sortCol) {
            null -> pontari
            ReportSort.ANGAJAT -> pontari.sortedBy { it.employeeName.lowercase() }
            ReportSort.ORE -> pontari.sortedBy { it.hours }
            ReportSort.LUCRARE -> pontari.sortedBy { it.lucrareName.lowercase() }
            ReportSort.CANTITATE -> pontari.sortedBy { it.quantity }
            ReportSort.CALITATE -> pontari.sortedBy { it.quality }
            ReportSort.DATA -> pontari.sortedBy { it.dateSort }
            ReportSort.ZONA -> pontari.sortedBy { it.zoneName.lowercase() }
        }
        if (sortCol != null && !asc) base.reversed() else base
    }
    val onSort: (ReportSort) -> Unit = { c -> if (sortCol == c) asc = !asc else { sortCol = c; asc = true } }

    Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            SortHeaderCell("Angajat", Modifier.width(160.dp), sortCol == ReportSort.ANGAJAT, asc) { onSort(ReportSort.ANGAJAT) }
            SortHeaderCell("Ore", Modifier.width(70.dp), sortCol == ReportSort.ORE, asc) { onSort(ReportSort.ORE) }
            SortHeaderCell("Lucrare", Modifier.width(130.dp), sortCol == ReportSort.LUCRARE, asc) { onSort(ReportSort.LUCRARE) }
            SortHeaderCell("Cantitate", Modifier.width(110.dp), sortCol == ReportSort.CANTITATE, asc) { onSort(ReportSort.CANTITATE) }
            SortHeaderCell("Calitate", Modifier.width(110.dp), sortCol == ReportSort.CALITATE, asc) { onSort(ReportSort.CALITATE) }
            SortHeaderCell("Data", Modifier.width(80.dp), sortCol == ReportSort.DATA, asc) { onSort(ReportSort.DATA) }
            SortHeaderCell("Zona", Modifier.width(110.dp), sortCol == ReportSort.ZONA, asc) { onSort(ReportSort.ZONA) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        sorted.forEachIndexed { index, p ->
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(p.employeeName, Modifier.width(160.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${p.hours.roundToInt()} ore", Modifier.width(70.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.lucrareName, Modifier.width(130.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${p.quantity.roundToInt()} ${p.unit}", Modifier.width(110.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Calitate ${p.quality.roundToInt()}/5", Modifier.width(110.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.date, Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.zoneName, Modifier.width(110.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (index < sorted.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
        }
    }
}

@Composable
private fun DetailPanel(
    state: ProjectDetailScreenUi,
    projectId: Long,
    viewModel: ProjectDetailViewModel,
    onReload: () -> Unit,
    onAssignTeam: () -> Unit,
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
                    series = state.graphSeries,
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
        DetailDialog.MIX -> MixLucrariPopup(
            state = state,
            viewModel = viewModel,
            onDismiss = { openDialog = null },
        )
        DetailDialog.TEAM -> ListPopupDialog(
            title = "Echipa curenta",
            subtitle = "${state.team.size} angajati • ${state.teamSalaryTotal.roundToInt()} RON salarii / luna",
            onDismiss = { openDialog = null },
            onAdd = { openDialog = null; onAssignTeam() },
        ) {
            if (state.team.isEmpty()) {
                EmptyHint("Niciun angajat alocat acestui proiect")
            } else {
                TeamTable(state.team)
            }
        }
        DetailDialog.ZONE -> ZonePopup(
            zones = state.zoneItems,
            viewModel = viewModel,
            onDismiss = { openDialog = null },
        )
        DetailDialog.PONTARE -> PontariPopup(
            pontari = state.pontari,
            team = state.team,
            projectId = projectId,
            onDismiss = { openDialog = null },
            onReload = onReload,
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
                .widthIn(max = 460.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
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
                            onQualityChange = viewModel::onQualityChange,
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

    if (state.duplicateWarning) {
        ConfirmDialog(
            title = "Pontare duplicata",
            message = "Exista deja o pontare pentru acest angajat, aceasta lucrare, in aceasta zi. Vrei sa o adaugi oricum?",
            onConfirm = { selectedUserId?.let { viewModel.confirmSubmitAnyway(it) } },
            onDismiss = { viewModel.dismissDuplicateWarning() },
            confirmLabel = "Adauga oricum",
            dismissLabel = "Anuleaza",
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
    series: List<ChartSeries>,
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
            verticalArrangement = Arrangement.spacedBy(LARGER_SPACING.dp),
        ) {
            Text(
                text = "Top 4 lucrari (cantitate / timp)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TimeNavLineChart(
                series = series,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
    onAdd: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
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

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    if (onAdd != null) {
                        IconButton(onClick = onAdd) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Adauga",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
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
}

// ---------------- Zone popup: lista + adauga / editeaza / sterge ----------------

private sealed interface ZoneMode {
    data object List : ZoneMode
    data object Add : ZoneMode
    data class Edit(val zone: ZoneItem) : ZoneMode
}

@Composable
private fun ZonePopup(
    zones: List<ZoneItem>,
    viewModel: ProjectDetailViewModel,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf<ZoneMode>(ZoneMode.List) }
    var toDelete by remember { mutableStateOf<ZoneItem?>(null) }
    when (val m = mode) {
        ZoneMode.List -> {
            ListPopupDialog(
                title = "Zone",
                subtitle = "progres pe fiecare zona",
                onDismiss = onDismiss,
                onAdd = { mode = ZoneMode.Add },
            ) {
                if (zones.isEmpty()) {
                    EmptyHint("Proiectul nu are zone. Apasa + ca sa adaugi una.")
                } else {
                    zones.forEach { zone ->
                        ZoneManageRow(
                            zone = zone,
                            onEdit = { mode = ZoneMode.Edit(zone) },
                            onDelete = { toDelete = zone },
                        )
                    }
                }
            }
            toDelete?.let { zone ->
                ConfirmDialog(
                    title = "Esti sigur?",
                    message = "Zona \"${zone.name}\" si lucrarile ei vor fi sterse.",
                    onConfirm = { viewModel.deleteZone(zone.id); toDelete = null },
                    onDismiss = { toDelete = null },
                )
            }
        }
        ZoneMode.Add -> ZoneFormDialog(
            title = "Adauga zona",
            initialName = "",
            initialSurface = null,
            showSurface = false,
            onDismiss = onDismiss,
            onBack = { mode = ZoneMode.List },
            onSave = { name, _ -> viewModel.addZone(name); mode = ZoneMode.List },
        )
        is ZoneMode.Edit -> ZoneFormDialog(
            title = "Editeaza zona",
            initialName = m.zone.name,
            initialSurface = m.zone.totalQuantity.toFloat(),
            showSurface = true,
            onDismiss = onDismiss,
            onBack = { mode = ZoneMode.List },
            onSave = { name, surface ->
                viewModel.updateZone(m.zone.id, name, surface ?: 0f); mode = ZoneMode.List
            },
        )
    }
}

@Composable
private fun ZoneManageRow(zone: ZoneItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    ItemRow {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = zone.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${zone.percent}% • ${zone.completedQuantity.roundToInt()} / ${zone.totalQuantity.roundToInt()} mp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Editeaza", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Sterge", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ZoneFormDialog(
    title: String,
    initialName: String,
    initialSurface: Float?,
    showSurface: Boolean,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onSave: (String, Float?) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var surface by remember { mutableStateOf(initialSurface?.let { it.roundToInt().toString() } ?: "") }
    FormDialog(title = title, onDismiss = onDismiss) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nume zona") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
        )
        if (showSurface) {
            OutlinedTextField(
                value = surface,
                onValueChange = { surface = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Suprafata (mp)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp),
            )
        }
        FormActions(onBack = onBack, onSave = { onSave(name, surface.toFloatOrNull()) }, saveEnabled = name.isNotBlank())
    }
}

// ---------------- Mix lucrari popup: lista + adauga lucrare pe zona ----------------

private sealed interface LucrareMode {
    data object List : LucrareMode
    data object Add : LucrareMode
    data class Edit(val entry: LucrareEntryItem) : LucrareMode
}

@Composable
private fun MixLucrariPopup(
    state: ProjectDetailScreenUi,
    viewModel: ProjectDetailViewModel,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf<LucrareMode>(LucrareMode.List) }
    var toDelete by remember { mutableStateOf<LucrareEntryItem?>(null) }
    when (val m = mode) {
        LucrareMode.List -> {
            ListPopupDialog(
                title = "Mix lucrari",
                subtitle = "lucrari pe zone",
                onDismiss = onDismiss,
                onAdd = { mode = LucrareMode.Add },
            ) {
                if (state.lucrariEntries.isEmpty()) {
                    EmptyHint("Nicio lucrare alocata. Apasa + ca sa adaugi.")
                } else {
                    state.lucrariEntries.forEach { entry ->
                        LucrareManageRow(
                            entry = entry,
                            onEdit = { mode = LucrareMode.Edit(entry) },
                            onDelete = { toDelete = entry },
                        )
                    }
                }
            }
            toDelete?.let { entry ->
                ConfirmDialog(
                    title = "Esti sigur?",
                    message = "Lucrarea \"${entry.lucrareName}\" din ${entry.zoneName} va fi stearsa.",
                    onConfirm = {
                        viewModel.deleteLucrare(entry.zoneId, entry.lucrareId, entry.quantity.toFloat())
                        toDelete = null
                    },
                    onDismiss = { toDelete = null },
                )
            }
        }
        LucrareMode.Add -> LucrareFormDialog(
            zones = state.zonePickers,
            skills = state.availableSkills,
            onDismiss = onDismiss,
            onBack = { mode = LucrareMode.List },
            onAddExisting = { zoneId, lucrareId, qty -> viewModel.addLucrare(zoneId, lucrareId, qty); mode = LucrareMode.List },
            onAddNew = { zoneId, n, u, p, qty -> viewModel.addNewLucrare(zoneId, n, u, p, qty); mode = LucrareMode.List },
        )
        is LucrareMode.Edit -> LucrareQuantityDialog(
            entry = m.entry,
            onDismiss = onDismiss,
            onBack = { mode = LucrareMode.List },
            onSave = { newQty ->
                viewModel.updateLucrare(m.entry.zoneId, m.entry.lucrareId, m.entry.quantity.toFloat(), newQty)
                mode = LucrareMode.List
            },
        )
    }
}

@Composable
private fun LucrareManageRow(entry: LucrareEntryItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    ItemRow {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.lucrareName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${entry.zoneName} • ${entry.quantity.roundToInt()} ${entry.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Editeaza", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Sterge", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun LucrareQuantityDialog(
    entry: LucrareEntryItem,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onSave: (Float) -> Unit,
) {
    var quantity by remember { mutableStateOf(entry.quantity.roundToInt().toString()) }
    FormDialog(title = "Editeaza ${entry.lucrareName}", onDismiss = onDismiss) {
        Text(
            text = "Zona: ${entry.zoneName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Cantitate (${entry.unit})") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(10.dp),
        )
        val qty = quantity.toFloatOrNull()
        FormActions(onBack = onBack, onSave = { onSave(qty!!) }, saveEnabled = qty != null && qty > 0f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LucrareFormDialog(
    zones: List<ZonePickItem>,
    skills: List<Lucrare>,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onAddExisting: (zoneId: Long?, lucrareId: Long, quantity: Float) -> Unit,
    onAddNew: (zoneId: Long?, name: String, unit: String, price: Float, quantity: Float) -> Unit,
) {
    var selectedZoneId by remember { mutableStateOf(zones.firstOrNull()?.id) }
    var zoneMenu by remember { mutableStateOf(false) }
    var selectedSkillId by remember { mutableStateOf<Long?>(null) }
    var skillMenu by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newUnit by remember { mutableStateOf("mp") }
    var newPrice by remember { mutableStateOf("") }

    val creatingNew = selectedSkillId == NEW_SKILL_ID
    val selectedSkill = skills.firstOrNull { it.id == selectedSkillId }
    val unitLabel = if (creatingNew) newUnit else selectedSkill?.unit ?: ""

    FormDialog(title = "Adauga lucrare", onDismiss = onDismiss) {
        if (zones.isNotEmpty()) {
            ExposedDropdownMenuBox(expanded = zoneMenu, onExpandedChange = { zoneMenu = it }) {
                OutlinedTextField(
                    value = zones.firstOrNull { it.id == selectedZoneId }?.name ?: "Alege zona",
                    onValueChange = {}, readOnly = true, label = { Text("Zona") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(10.dp),
                )
                ExposedDropdownMenu(expanded = zoneMenu, onDismissRequest = { zoneMenu = false }) {
                    zones.forEach { z ->
                        DropdownMenuItem(text = { Text(z.name) }, onClick = { selectedZoneId = z.id; zoneMenu = false })
                    }
                }
            }
        }

        ExposedDropdownMenuBox(expanded = skillMenu, onExpandedChange = { skillMenu = it }) {
            val lucrareLabel = when {
                creatingNew -> "Lucrare noua"
                selectedSkill != null -> "${selectedSkill.name} (${selectedSkill.unit})"
                else -> "Alege lucrare"
            }
            OutlinedTextField(
                value = lucrareLabel, onValueChange = {}, readOnly = true, label = { Text("Lucrare") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(10.dp),
            )
            ExposedDropdownMenu(expanded = skillMenu, onDismissRequest = { skillMenu = false }) {
                skills.forEach { s ->
                    DropdownMenuItem(text = { Text("${s.name} (${s.unit})") }, onClick = { selectedSkillId = s.id; skillMenu = false })
                }
                DropdownMenuItem(text = { Text("+ Creeaza lucrare noua") }, onClick = { selectedSkillId = NEW_SKILL_ID; skillMenu = false })
            }
        }

        if (creatingNew) {
            OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Denumire lucrare") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = newUnit, onValueChange = { newUnit = it }, label = { Text("Unitate (mp, mc, buc...)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = newPrice, onValueChange = { newPrice = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Pret / unitate (RON)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp))
        }

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Cantitate" + if (unitLabel.isNotBlank()) " ($unitLabel)" else "") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(10.dp),
        )

        val qty = quantity.toFloatOrNull()
        val canSave = qty != null && qty > 0f &&
            ((creatingNew && newName.isNotBlank()) || (!creatingNew && selectedSkill != null))
        FormActions(
            onBack = onBack,
            onSave = {
                if (creatingNew) onAddNew(selectedZoneId, newName, newUnit, newPrice.toFloatOrNull() ?: 0f, qty!!)
                else onAddExisting(selectedZoneId, selectedSkill!!.id, qty!!)
            },
            saveEnabled = canSave,
        )
    }
}

// ---------------- Pontari popup: lista pontarilor + adauga pontare ----------------

@Composable
private fun PontariPopup(
    pontari: List<PontareRowItem>,
    team: List<TeamMemberItem>,
    projectId: Long,
    onDismiss: () -> Unit,
    onReload: () -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    if (!creating) {
        ListPopupDialog(
            title = "Pontari",
            subtitle = "${pontari.size} inregistrari",
            onDismiss = { confirmClose = true },
            onAdd = { creating = true },
        ) {
            if (pontari.isEmpty()) {
                EmptyHint("Nicio pontare. Apasa + ca sa adaugi una.")
            } else {
                PontariTable(pontari)
            }
        }
    } else {
        PontarePopup(
            team = team,
            projectId = projectId,
            onDismiss = { confirmClose = true },
            onSaved = { onReload(); creating = false },
        )
    }

    if (confirmClose) {
        ConfirmDialog(
            title = "Esti sigur ca vrei sa inchizi?",
            message = "Ce ai introdus se va pierde.",
            onConfirm = {
                confirmClose = false
                if (creating) creating = false else onDismiss()
            },
            onDismiss = { confirmClose = false },
            confirmLabel = "Da, inchide",
            dismissLabel = "Nu",
        )
    }
}

private enum class PontareSort { ANGAJAT, LUCRARE, ZI, CANTITATE }

@Composable
private fun PontariTable(pontari: List<PontareRowItem>) {
    var sortCol by remember { mutableStateOf<PontareSort?>(null) }
    var asc by remember { mutableStateOf(true) }
    val sorted = remember(pontari, sortCol, asc) {
        val base = when (sortCol) {
            null -> pontari
            PontareSort.ANGAJAT -> pontari.sortedBy { it.employeeName.lowercase() }
            PontareSort.LUCRARE -> pontari.sortedBy { it.lucrareName.lowercase() }
            PontareSort.ZI -> pontari.sortedBy { it.dateSort }
            PontareSort.CANTITATE -> pontari.sortedBy { it.quantity }
        }
        if (sortCol != null && !asc) base.reversed() else base
    }
    val onSort: (PontareSort) -> Unit = { c ->
        if (sortCol == c) asc = !asc else { sortCol = c; asc = true }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            SortHeaderCell("Angajat", Modifier.weight(1.4f), sortCol == PontareSort.ANGAJAT, asc) { onSort(PontareSort.ANGAJAT) }
            SortHeaderCell("Lucrare", Modifier.weight(1f), sortCol == PontareSort.LUCRARE, asc) { onSort(PontareSort.LUCRARE) }
            SortHeaderCell("Zi", Modifier.weight(1f), sortCol == PontareSort.ZI, asc) { onSort(PontareSort.ZI) }
            SortHeaderCell("Cant.", Modifier.weight(0.8f), sortCol == PontareSort.CANTITATE, asc) { onSort(PontareSort.CANTITATE) }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        sorted.forEachIndexed { index, p ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(p.employeeName, Modifier.weight(1.4f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(p.lucrareName, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(p.date, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${p.quantity.roundToInt()} ${p.unit}", Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (index < sorted.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
        }
    }
}

@Composable
private fun SortHeaderCell(text: String, modifier: Modifier, active: Boolean, asc: Boolean, onClick: () -> Unit) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (active) {
            Icon(
                imageVector = if (asc) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 2.dp).size(12.dp),
            )
        }
    }
}

// ---------------- Helpers comune pentru formularele din popup ----------------

@Composable
private fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                content()
            }
        }
    }
}

@Composable
private fun FormActions(onBack: () -> Unit, onSave: () -> Unit, saveEnabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Inapoi") }
        Button(
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) { Text("Salveaza") }
    }
}

private const val NEW_SKILL_ID = -1L

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

