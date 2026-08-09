package com.example.workipi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.workipi.data.model.Material
import com.example.workipi.data.model.Unealta
import com.example.workipi.data.model.AppPermission
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.ConfirmDialog
import com.example.workipi.ui.session.LocalSessionState
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

private enum class DetailDialog { PONTARE, PONTARI_LIST, ZONE }
private enum class ProjectDetailTab(val label: String) {
    SUMAR("Sumar"),
    LUCRARI("Lucrări"),
    ECHIPA("Echipă și activitate"),
    RESURSE("Resurse și costuri"),
}
private enum class WorksDialog { ADD_ZONE, ADD_WORK }
private enum class ResourcesDialog { ADD_MATERIAL }

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

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.name.ifBlank { "Workspace proiect" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (state.address.isNotBlank()) {
                    Text(
                        text = state.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.Close, contentDescription = "Înapoi la proiecte")
            }
        }

        DetailPage(
            modifier = Modifier.weight(1f),
            state = state,
            projectId = projectId,
            viewModel = viewModel,
            onReload = { viewModel.load(projectId) },
            onAssignTeam = { navController.navigate(Screen.AssignEmployees.createRoute(projectId)) },
        )

    }
}

@Composable
private fun DetailPage(
    modifier: Modifier = Modifier,
    state: ProjectDetailScreenUi,
    projectId: Long,
    viewModel: ProjectDetailViewModel,
    onReload: () -> Unit,
    onAssignTeam: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { ProjectDetailTab.entries.size }

    Column(
        modifier = Modifier
            .then(modifier)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(SCREEN_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
    ) {
        val error = state.error
        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
        } else {
            ProjectDetailTabs(
                selected = ProjectDetailTab.entries[pagerState.currentPage],
                onSelect = { tab ->
                    scope.launch {
                        pagerState.animateScrollToPage(tab.ordinal)
                    }
                },
            )
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                when (ProjectDetailTab.entries[page]) {
                    ProjectDetailTab.SUMAR -> Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
                    ) {
                        DetailPanel(state, projectId, viewModel, onReload, onAssignTeam)
                    }
                    ProjectDetailTab.LUCRARI -> Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    ) {
                        WorksDetailPage(state, viewModel)
                    }
                    ProjectDetailTab.ECHIPA -> Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    ) {
                        TeamActivityPage(state = state, onEditTeam = onAssignTeam)
                    }
                    ProjectDetailTab.RESURSE -> Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    ) {
                        ResourcesCostsPage(state = state, viewModel = viewModel)
                    }
                }
            }
        }

    }
}

@Composable
private fun ProjectDetailTabs(
    selected: ProjectDetailTab,
    onSelect: (ProjectDetailTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProjectDetailTab.entries.forEach { tab ->
            val active = tab == selected
            TextButton(
                onClick = { onSelect(tab) },
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) { Text(tab.label, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ProjectDetailHero(state: ProjectDetailScreenUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.name.ifBlank { "Proiect" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(state.address.ifBlank { "Adresă necompletată" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(state.status)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { (state.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(99.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text("${state.progressPercent}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroDetail("Termen", state.endDate, Modifier.weight(1f))
                HeroDetail("Estimare", state.estimatedEndDate, Modifier.weight(1f))
                HeroDetail("Realizat", "${state.finishedQuantity.roundToInt()} / ${state.totalQuantity.roundToInt()} mp", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroDetail(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WorksDetailPage(
    state: ProjectDetailScreenUi,
    viewModel: ProjectDetailViewModel,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<WorksDialog?>(null) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Lucrări", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Suprafețe, progres și organizare pe zone.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WorksMetric(
                    modifier = Modifier.weight(1f),
                    label = "Suprafață realizată",
                    value = "${state.finishedQuantity.roundToInt()} / ${state.totalQuantity.roundToInt()} mp",
                    helper = "${state.progressPercent}% finalizat",
                    progress = state.progressPercent / 100f,
                )
                val efficiency = if (state.companyCompletedProjectsPace > 0) (state.projectPace / state.companyCompletedProjectsPace * 100).roundToInt() else null
                WorksMetric(
                    modifier = Modifier.weight(1f),
                    label = "Eficiență proiect",
                    value = efficiency?.let { "$it%" } ?: "—",
                    helper = if (efficiency != null) "${state.projectPace.roundToInt()} mp/zi față de ${state.companyCompletedProjectsPace.roundToInt()} mp/zi, media firmei" else "Media firmei va apărea după primele proiecte încheiate.",
                    positive = efficiency?.let { it >= 100 } == true,
                )
            }
            Row(modifier = Modifier.fillMaxWidth().heightIn(min = 430.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                WorksListCard(state.mixLucrari, onAddWork = { dialog = WorksDialog.ADD_WORK }, modifier = Modifier.weight(1f).fillMaxHeight())
                ZonesCard(
                    zones = state.zoneItems,
                    entries = state.lucrariEntries,
                    onAddZone = { dialog = WorksDialog.ADD_ZONE },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
    when (dialog) {
        WorksDialog.ADD_ZONE -> ZoneFormDialog(
            title = "Adaugă zonă",
            initialName = "",
            initialSurface = null,
            showSurface = false,
            onDismiss = { dialog = null },
            onBack = { dialog = null },
            onSave = { name, _ -> viewModel.addZone(name); dialog = null },
        )
        WorksDialog.ADD_WORK -> LucrareFormDialog(
            zones = state.zonePickers,
            skills = state.availableSkills,
            onDismiss = { dialog = null },
            onBack = { dialog = null },
            onAddExisting = { zoneId, workId, quantity -> viewModel.addLucrare(zoneId, workId, quantity); dialog = null },
            onAddNew = { zoneId, name, unit, price, quantity -> viewModel.addNewLucrare(zoneId, name, unit, price, quantity); dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun WorksMetric(
    label: String,
    value: String,
    helper: String,
    modifier: Modifier = Modifier,
    positive: Boolean = false,
    progress: Float? = null,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (positive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
            Text(helper, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            progress?.let {
                LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp).clip(RoundedCornerShape(99.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
private fun WorksListCard(works: List<MixLucrareItem>, onAddWork: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Lista lucrărilor", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onAddWork, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text("+ Lucrare") }
            }
            Text("Cantitatea totală și progresul fiecărei lucrări.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (works.isEmpty()) EmptyHint("Nu sunt încă lucrări alocate zonelor acestui proiect.")
            else works.forEach { work -> WorkAggregateRow(work) }
        }
    }
}

@Composable
private fun WorkAggregateRow(work: MixLucrareItem) {
    val progress = if (work.totalQuantity > 0) (work.completedQuantity / work.totalQuantity).coerceIn(0.0, 1.0) else 0.0
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(work.name, fontWeight = FontWeight.Bold)
            Text("${work.completedQuantity.roundToInt()} / ${work.totalQuantity.roundToInt()} ${work.unit}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinearProgressIndicator(progress = { progress.toFloat() }, modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(99.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surface)
            Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${work.pacePerDay.roundToInt()} mp/zi", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ZonesCard(
    zones: List<ZoneItem>,
    entries: List<LucrareEntryItem>,
    onAddZone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val worksByZoneId = entries.groupBy { it.zoneId }
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Zone", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = onAddZone, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text("+ Zonă") }
            }
            Text("Lucrările alocate fiecărei zone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (zones.isEmpty()) EmptyHint("Nu există încă zone. Adaugă prima zonă pentru proiect.")
            else zones.forEach { zone ->
                ZoneWorksRow(zone = zone, works = worksByZoneId[zone.id].orEmpty())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZoneWorksRow(zone: ZoneItem, works: List<LucrareEntryItem>) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(13.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(zone.name, fontWeight = FontWeight.Bold)
            Text("${works.size} ${if (works.size == 1) "lucrare" else "lucrări"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (works.isEmpty()) {
            Text(
                "Zonă pregătită. Adaugă o lucrare ca să setezi suprafața.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                works.forEach { work ->
                    val percent = if (work.quantity > 0) (work.completedQuantity / work.quantity * 100).roundToInt().coerceIn(0, 100) else 0
                    AssistChip(onClick = {}, label = { Text("${work.lucrareName} · ${work.completedQuantity.roundToInt()}/${work.quantity.roundToInt()} ${work.unit} · $percent%", maxLines = 1) }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant))
                }
            }
        }
    }
}

@Composable
private fun TeamActivityPage(
    state: ProjectDetailScreenUi,
    onEditTeam: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentCount = state.team.count { it.isPresent }
    val absentCount = (state.team.size - presentCount).coerceAtLeast(0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Echipă și activitate", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Angajații alocați proiectului și situația operațională a zilei.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 390.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Card(
                    modifier = Modifier.weight(2.1f).fillMaxHeight(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Echipa proiectului", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${state.team.size} ${if (state.team.size == 1) "angajat alocat" else "angajați alocați"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(onClick = onEditTeam) { Text("Editează echipa") }
                        }
                        if (state.team.isEmpty()) {
                            EmptyHint("Niciun angajat nu este alocat încă proiectului.")
                        } else {
                            TeamActivityTable(state.team)
                        }
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Activitatea zilei", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Prezența este calculată pentru ziua operațională 03:00–02:59.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TeamActivityStat("Prezenți", "$presentCount/${state.team.size}", Modifier.weight(1f), positive = true)
                            TeamActivityStat("Pontări", state.pontariCount.toString(), Modifier.weight(1f))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        Text("Situație curentă", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        if (state.team.isEmpty()) {
                            Text("Adaugă angajați în echipă pentru a urmări prezența.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            TeamPresenceSummary("Prezenți", presentCount, MaterialTheme.colorScheme.secondary)
                            TeamPresenceSummary("Absenți / neconfirmați", absentCount, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamActivityStat(label: String, value: String, modifier: Modifier = Modifier, positive: Boolean = false) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (positive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TeamPresenceSummary(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun TeamActivityTable(team: List<TeamMemberItem>) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeamHeaderCell("Nume angajat", Modifier.weight(1.5f))
            TeamHeaderCell("Rol în proiect", Modifier.weight(1.05f))
            TeamHeaderCell("Ritm personal", Modifier.weight(0.9f))
            TeamHeaderCell("Prezență", Modifier.weight(0.85f))
        }
        team.forEachIndexed { index, member ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(member.name, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(member.role, modifier = Modifier.weight(1.05f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${member.mpPerDay.roundToInt()} mp/zi", modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                TeamPresenceBadge(member.isPresent, Modifier.weight(0.85f))
            }
            if (index < team.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
    }
}

@Composable
private fun TeamPresenceBadge(isPresent: Boolean, modifier: Modifier = Modifier) {
    val color = if (isPresent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    val background = if (isPresent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Text(
        text = if (isPresent) "Prezent" else "Absent",
        modifier = modifier.widthIn(min = 66.dp).clip(RoundedCornerShape(99.dp)).background(background).padding(horizontal = 9.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
private fun ResourcesCostsPage(
    state: ProjectDetailScreenUi,
    viewModel: ProjectDetailViewModel,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<ResourcesDialog?>(null) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Resurse și costuri", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Materialele proiectului, inventarul firmei și situația financiară estimată.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 460.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MaterialsResourceCard(
                    materials = state.materials,
                    onAdd = { dialog = ResourcesDialog.ADD_MATERIAL },
                    modifier = Modifier.weight(1.05f).fillMaxHeight(),
                )
                ToolsResourceCard(
                    tools = state.tools,
                    unavailable = state.toolsUnavailable,
                    modifier = Modifier.weight(1.05f).fillMaxHeight(),
                )
                ProjectCostsCard(
                    state = state,
                    modifier = Modifier.weight(1.1f).fillMaxHeight(),
                )
            }
        }
    }
    when (dialog) {
        ResourcesDialog.ADD_MATERIAL -> MaterialFormDialog(
            onDismiss = { dialog = null },
            onSave = { name, quantity, unit, cost ->
                viewModel.addMaterial(name, quantity, unit, cost)
                dialog = null
            },
        )
        null -> Unit
    }
}

@Composable
private fun MaterialsResourceCard(materials: List<Material>, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Materiale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Necesarul salvat pentru proiect.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onAdd, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) { Text("+ Material") }
            }
            if (materials.isEmpty()) {
                EmptyHint("Nu există materiale pe proiect. Adaugă primul material.")
            } else {
                materials.forEachIndexed { index, material ->
                    MaterialResourceRow(material)
                    if (index < materials.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
private fun MaterialResourceRow(material: Material) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(material.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(material.totalCost.toDouble().asRon(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${material.quantity.roundToInt()} ${material.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${material.unitCost.toDouble().asRon()} / ${material.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToolsResourceCard(tools: List<Unealta>, unavailable: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Unelte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (unavailable) "Modulul de inventar nu este configurat." else "Inventarul disponibil al firmei.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (unavailable) "MOCK" else "FIRMĂ", modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 7.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (unavailable) "Când adăugăm inventarul, aici vom afișa disponibilitatea lui." else "Alocarea unei unelte pe proiect nu este încă modelată în bază.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (unavailable) {
                EmptyHint("Tabelul de unelte lipsește din baza de date curentă.")
            } else if (tools.isEmpty()) {
                EmptyHint("Nu sunt unelte înregistrate pentru firmă.")
            } else {
                tools.forEachIndexed { index, tool ->
                    ToolResourceRow(tool)
                    if (index < tools.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
private fun ToolResourceRow(tool: Unealta) {
    val usedShare = if (tool.totalQuantity > 0) tool.inUse.toFloat() / tool.totalQuantity else 0f
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(tool.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        LinearProgressIndicator(
            progress = { usedShare },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(99.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${tool.inUse} în uz / ${tool.totalQuantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            Text("${tool.availableQuantity} disponibile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProjectCostsCard(state: ProjectDetailScreenUi, modifier: Modifier = Modifier) {
    val salaryCost = (state.projectCosts - state.materialCosts).coerceAtLeast(0.0)
    val budget = state.budget.coerceAtLeast(0.0)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Costuri proiect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Estimare din salariile echipei și materialele proiectului.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CostMetric("Valoare contract", state.budget.asRon(), Modifier.weight(1f))
                CostMetric("Cost estimat", state.projectCosts.asRon(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CostMetric("Salarii estimate", salaryCost.asRon(), Modifier.weight(1f))
                CostMetric("Profit estimat", state.possibleGains.asRon(), Modifier.weight(1f), positive = state.possibleGains >= 0)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Text("Structura costului estimat", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            CostBar("Salarii", salaryCost, budget)
            CostBar("Materiale", state.materialCosts, budget)
            CostBar("Cost total", state.projectCosts, budget)
            CostBar("Profit estimat", state.possibleGains.coerceAtLeast(0.0), budget, positive = true)
        }
    }
}

@Composable
private fun CostMetric(label: String, value: String, modifier: Modifier = Modifier, positive: Boolean = false) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (positive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CostBar(label: String, value: Double, budget: Double, positive: Boolean = false) {
    val share = if (budget > 0.0) (value / budget).toFloat().coerceIn(0f, 1f) else 0f
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(label, modifier = Modifier.width(88.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
        LinearProgressIndicator(
            progress = { share },
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(99.dp)),
            color = if (positive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(value.asRon(), modifier = Modifier.width(92.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MaterialFormDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: Float, unit: String, unitCost: Float) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("buc") }
    var unitCost by remember { mutableStateOf("") }
    val parsedQuantity = quantity.toFloatOrNull()
    val parsedCost = unitCost.toFloatOrNull()
    FormDialog(title = "Adaugă material", onDismiss = onDismiss) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Denumire material") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Cantitate") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp))
            OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unitate") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
        }
        OutlinedTextField(value = unitCost, onValueChange = { unitCost = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Cost / unitate (RON)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp))
        FormActions(
            onBack = onDismiss,
            onSave = { onSave(name, parsedQuantity!!, unit, parsedCost!!) },
            saveEnabled = name.isNotBlank() && parsedQuantity != null && parsedQuantity > 0f && parsedCost != null && parsedCost >= 0f,
        )
    }
}

@Composable
private fun ProjectWorksPage(state: ProjectDetailScreenUi) {
    WorkspaceListPage(title = "Lucrări", subtitle = "Lucrările planificate pe zone și cantitățile lor") {
        if (state.lucrariEntries.isEmpty()) {
            EmptyHint("Nicio lucrare alocată proiectului.")
        } else {
            state.lucrariEntries.forEach { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(item.lucrareName, fontWeight = FontWeight.SemiBold)
                            Text(item.zoneName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${item.quantity.roundToInt()} ${item.unit}", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectZonesPage(state: ProjectDetailScreenUi) {
    WorkspaceListPage(title = "Zone și subzone", subtitle = "Progresul fizic al fiecărei zone") {
        if (state.zoneItems.isEmpty()) {
            EmptyHint("Nicio zonă configurată.")
        } else {
            state.zoneItems.forEach { zone ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(zone.name, fontWeight = FontWeight.SemiBold)
                            Text("${zone.percent}%", color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(progress = { zone.percent / 100f }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "${zone.completedQuantity.roundToInt()} / ${zone.totalQuantity.roundToInt()} mp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectTeamPage(state: ProjectDetailScreenUi, onAssignTeam: () -> Unit) {
    WorkspaceListPage(title = "Echipă", subtitle = "Membrii alocați proiectului") {
        Button(onClick = onAssignTeam) { Text("Gestionează echipa") }
        if (state.team.isEmpty()) EmptyHint("Niciun membru alocat.") else TeamTable(state.team)
    }
}

@Composable
private fun ProjectCostsPage(state: ProjectDetailScreenUi) {
    if (!LocalSessionState.current.hasPermission(AppPermission.FINANCIALS_VIEW)) {
        WorkspacePlaceholderPage("Costuri", "Nu ai permisiunea de a vedea informațiile financiare ale proiectului.")
        return
    }
    WorkspaceListPage(title = "Costuri", subtitle = "Situația financiară curentă a proiectului") {
        Row(horizontalArrangement = Arrangement.spacedBy(CARD_SPACING.dp)) {
            InformationCard(Modifier.weight(1f), "Buget ofertat", "${state.budget.roundToInt()} RON")
            InformationCard(Modifier.weight(1f), "Costuri proiect", "${state.projectCosts.roundToInt()} RON")
            InformationCard(Modifier.weight(1f), "Profit anticipat", "${state.possibleGains.roundToInt()} RON")
        }
    }
}

@Composable
private fun WorkspacePlaceholderPage(title: String, description: String) {
    WorkspaceListPage(title = title, subtitle = description) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(
                "Structura modulului este pregătită în workspace. Datele și fluxurile dedicate vor fi conectate în etapa următoare.",
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkspaceListPage(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SCREEN_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION.dp),
    ) {
        Column(
            modifier = Modifier.padding(SCREEN_PADDING.dp),
            verticalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
        ) {
            Text("Sumar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Situația curentă a proiectului.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CARD_SPACING.dp)) {
                SummaryMetric("Progres general", "${state.progressPercent}%", "${state.finishedQuantity.roundToInt()} / ${state.totalQuantity.roundToInt()} mp", Modifier.weight(1f))
                SummaryMetric("Termen finalizare", state.endDate, "termen contractual", Modifier.weight(1f))
                SummaryMetric("Estimare finalizare", state.estimatedEndDate, "conform ritmului actual", Modifier.weight(1f))
                SummaryMetric("Valoare contract", "${state.budget.roundToInt()} RON", "valoare contractuală", Modifier.weight(1f))
                SummaryMetric("Profit estimat", "${state.possibleGains.roundToInt()} RON", "venit minus salarii și materiale", Modifier.weight(1f), positive = state.possibleGains >= 0)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CARD_SPACING.dp)) {
                SummaryMetric("Prezenți în șantier", state.presentOnSiteCount.toString(), "zi operațională: 03:00 – 02:59", Modifier.weight(1f))
                SummaryMetric("Ritm de lucru", "${state.projectPace.roundToInt()} mp/zi", "media tuturor lucrărilor proiectului", Modifier.weight(1f))
                SummaryMetric("Unelte în șantier", "—", "MOCK · logică în curs", Modifier.weight(1f), mock = true)
                SummaryMetric("Pontări", state.pontariCount.toString(), "apasă pentru lista de pontări", Modifier.weight(1f), onClick = { openDialog = DetailDialog.PONTARI_LIST })
                SummaryMetric("Revizii", "—", "MOCK · de făcut sau în progres", Modifier.weight(1f), mock = true)
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(390.dp),
                horizontalArrangement = Arrangement.spacedBy(CARD_SPACING.dp),
            ) {
                MockPlanVsActualCard(
                    modifier = Modifier.weight(2.05f).fillMaxHeight(),
                    projectPace = state.projectPace,
                )
                SummaryAttentionCard(
                    modifier = Modifier.weight(0.95f).fillMaxHeight(),
                )
            }

        }
    }

    when (openDialog) {
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
        DetailDialog.PONTARI_LIST -> ListPopupDialog(
            title = "Pontări proiect",
            subtitle = "${state.pontariCount} pontări înregistrate",
            onDismiss = { openDialog = null },
        ) {
            if (state.pontari.isEmpty()) EmptyHint("Nu există pontări pentru acest proiect.")
            else ReportsTable(state.pontari)
        }
        null -> Unit
    }
}

@Composable
private fun MockPlanVsActualCard(modifier: Modifier = Modifier, projectPace: Double) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Plan vs realizat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ritmul realizat în zilele lucrate.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = {}, label = { Text("MOCK") }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant))
            }
            MockLineChart(modifier = Modifier.weight(1f).fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(width = 24.dp, height = 4.dp).clip(RoundedCornerShape(99.dp)).background(MaterialTheme.colorScheme.primary))
                    Text("Mp realizați", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Medie: ${projectPace.roundToInt()} mp/zi",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun MockLineChart(modifier: Modifier = Modifier) {
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
    val line = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val paddingLeft = 18.dp.toPx()
            val paddingRight = 14.dp.toPx()
            val paddingTop = 18.dp.toPx()
            val paddingBottom = 12.dp.toPx()
            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom
            repeat(4) { index ->
                val y = paddingTop + chartHeight * index / 3f
                drawLine(grid, Offset(paddingLeft, y), Offset(size.width - paddingRight, y), strokeWidth = 1.dp.toPx())
            }
            val values = listOf(.53f, .62f, .73f, .42f, .55f, .64f, .77f)
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = paddingLeft + chartWidth * index / (values.size - 1)
                val y = paddingTop + chartHeight * (1f - value)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            values.forEachIndexed { index, value ->
                val x = paddingLeft + chartWidth * index / (values.size - 1)
                val y = paddingTop + chartHeight * (1f - value)
                drawCircle(color = surface, radius = 4.dp.toPx(), center = Offset(x, y))
                drawCircle(color = line, radius = 2.3.dp.toPx(), center = Offset(x, y))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("L", "Ma", "Mi", "J", "V", "S", "D").forEach { day ->
                Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SummaryAttentionCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Necesită atenție", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Riscuri și sugestii CONDIK AI.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AttentionNote("TERMEN", "Ritm conform planului", "Progresul și resursele disponibile susțin termenul contractual actual.", MaterialTheme.colorScheme.secondaryContainer)
            AttentionNote("ECHIPĂ", "Urmărește ritmul echipei", "Compară activitatea pe zile înainte de următoarea alocare.", Color(0xFFFFF3DD))
            AttentionNote("MATERIALE", "Acoperire stoc", "MOCK · conectarea materialelor va semnala necesarul de aprovizionare.", Color(0xFFFFF3DD))
        }
    }
}

@Composable
private fun AttentionNote(category: String, title: String, detail: String, background: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(background).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(category, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SummaryMetric(
    title: String,
    value: String,
    helper: String,
    modifier: Modifier = Modifier,
    positive: Boolean = false,
    mock: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (positive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                Text(helper, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (mock) {
                Text("MOCK", modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (onClick != null) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "Deschide $title", modifier = Modifier.align(Alignment.TopEnd).size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
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
        } else {
            Text(
                "Adaugă mai întâi o zonă; fiecare lucrare trebuie alocată unei zone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
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
        val canSave = selectedZoneId != null && qty != null && qty > 0f &&
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

private fun Double.asRon(): String {
    val sign = if (this < 0) "−" else ""
    val grouped = kotlin.math.abs(roundToInt()).toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
    return "$sign$grouped RON"
}
