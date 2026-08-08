package com.example.workipi.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workipi.data.model.AppPermission
import com.example.workipi.data.model.SiteCorrection
import com.example.workipi.data.model.SiteJournalEntry
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.ui.session.LocalSessionState
import com.example.workipi.viewmodel.SantierUiState
import com.example.workipi.viewmodel.SantierViewModel
import com.example.workipi.viewmodel.SiteAttendanceRow
import com.example.workipi.viewmodel.SiteTimeEntryRow
import com.example.workipi.viewmodel.SiteWorkRow
import kotlin.math.roundToInt

private enum class SiteTab(val label: String) {
    TODAY("Astăzi"), TIME("Pontări"), QUALITY("Calitate"), JOURNAL("Jurnal")
}

@Composable
fun SantierScreen(
    navController: NavController,
    viewModel: SantierViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val session = LocalSessionState.current
    val canManage = session.hasPermission(AppPermission.SITE_MANAGE)
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCorrectionDialog by remember { mutableStateOf(false) }
    var showJournalDialog by remember { mutableStateOf(false) }
    var showClosureDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        val message = state.errorMessage ?: state.successMessage
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SiteHeader(onRefresh = viewModel::refresh)
            ProjectPicker(state, viewModel::selectProject)
            if (state.projects.isEmpty() && !state.isLoading) {
                Text("Nu există proiecte active accesibile.", Modifier.padding(20.dp))
            } else {
                Metrics(state)
                TabRow(selectedTabIndex = selectedTab) {
                    SiteTab.entries.forEachIndexed { index, tab ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(tab.label) })
                    }
                }
                when (SiteTab.entries[selectedTab]) {
                    SiteTab.TODAY -> TodayContent(
                        state = state,
                        canManage = canManage,
                        onAttendance = viewModel::setAttendance,
                        onCloseDay = { showClosureDialog = true },
                    )
                    SiteTab.TIME -> TimeEntriesContent(
                        entries = state.timeEntries,
                        users = state.usersById.mapValues { it.value.fullName },
                        ownUserId = session.user?.id,
                        canCreateOwn = session.hasPermission(AppPermission.TIME_ENTRIES_CREATE),
                        canReview = session.hasPermission(AppPermission.TIME_ENTRIES_REVIEW),
                    ) { targetUserId ->
                        val projectId = state.selectedProjectId
                        if (projectId != null) {
                            navController.navigate(Screen.PontareEntry.createRoute(projectId, targetUserId))
                        }
                    }
                    SiteTab.QUALITY -> QualityContent(
                        corrections = state.corrections,
                        users = state.usersById.mapValues { it.value.fullName },
                        canManage = canManage,
                        onAdd = { showCorrectionDialog = true },
                        onStatus = viewModel::updateCorrectionStatus,
                    )
                    SiteTab.JOURNAL -> JournalContent(state.journal, state.photoUrls) { showJournalDialog = true }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        if (state.isLoading || state.isSaving) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (showCorrectionDialog) CorrectionDialog(
        users = state.usersById.mapValues { it.value.fullName },
        onDismiss = { showCorrectionDialog = false },
        onSave = { title, description, severity, assignee ->
            showCorrectionDialog = false
            viewModel.createCorrection(title, description, severity, assignee)
        },
    )
    if (showJournalDialog) JournalDialog(
        onDismiss = { showJournalDialog = false },
        onSave = { type, title, description, severity, bytes, mime ->
            showJournalDialog = false
            viewModel.createJournalEntry(type, title, description, severity, bytes, mime)
        },
    )
    if (showClosureDialog) CloseDayDialog(
        onDismiss = { showClosureDialog = false },
        onSave = { summary, blockers, next ->
            showClosureDialog = false
            viewModel.closeDay(summary, blockers, next)
        },
    )
}

@Composable
private fun SiteHeader(onRefresh: () -> Unit) {
    val openDrawer = LocalOpenDrawer.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (openDrawer != null) IconButton(openDrawer) { Icon(Icons.Filled.Menu, "Deschide meniul") }
        Column(Modifier.weight(1f)) {
            Text("Șantier", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Activitatea operațională a zilei", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onRefresh) { Icon(Icons.Filled.Refresh, "Reîncarcă") }
    }
}

@Composable
private fun ProjectPicker(state: SantierUiState, onSelect: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.projects.forEach { project ->
            FilterChip(
                selected = state.selectedProjectId == project.projectId,
                onClick = { onSelect(project.projectId) },
                label = { Text(project.title) },
            )
        }
    }
}

@Composable
private fun Metrics(state: SantierUiState) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Metric("Prezenți", "${state.presentCount}/${state.attendance.size}")
        Metric("Executat azi", formatQuantity(state.todayQuantity))
        Metric("Corecturi", state.openCorrections.toString())
        Metric("Incidente", state.incidentsToday.toString())
        Metric("Zi", if (state.closure == null) "Deschisă" else "Închisă")
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TodayContent(
    state: SantierUiState,
    canManage: Boolean,
    onAttendance: (Long, String) -> Unit,
    onCloseDay: () -> Unit,
) {
    Section("Prezență zilnică") {
        if (state.attendance.isEmpty()) EmptyText("Nu sunt membri alocați proiectului.")
        state.attendance.forEach { AttendanceItem(it, canManage && state.closure == null, onAttendance) }
    }
    Section("Lucrări în desfășurare și cantități") {
        if (state.workItems.isEmpty()) EmptyText("Nu există lucrări în desfășurare.")
        state.workItems.forEach { WorkItem(it) }
    }
    Section("Raport zilnic") {
        val closure = state.closure
        if (closure == null) {
            Text("Raportul se generează din prezență, pontări, cantități, corecturi și jurnal.")
            if (canManage) Button(onClick = onCloseDay, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CheckCircle, null)
                Text("  Închide ziua și salvează raportul")
            }
        } else {
            AssistChip(onClick = {}, label = { Text("Zi închisă") }, leadingIcon = { Icon(Icons.Filled.CheckCircle, null) })
            Text(closure.summary)
            closure.blockers?.let { Text("Blocaje: $it") }
            closure.nextDayPlan?.let { Text("Plan următoarea zi: $it") }
        }
    }
}

@Composable
private fun AttendanceItem(row: SiteAttendanceRow, editable: Boolean, onAttendance: (Long, String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(row.user.fullName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(attendanceLabel(row.attendance?.status), color = MaterialTheme.colorScheme.primary)
        }
        if (editable) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("prezent" to "Prezent", "absent" to "Absent", "concediu" to "Concediu").forEach { (status, label) ->
                FilterChip(
                    selected = row.attendance?.status == status,
                    onClick = { onAttendance(row.user.idUser, status) },
                    label = { Text(label) },
                )
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun WorkItem(item: SiteWorkRow) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(item.workName, fontWeight = FontWeight.SemiBold)
                Text(item.zoneName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${formatQuantity(item.completed)} / ${formatQuantity(item.total)} ${item.unit}")
        }
        LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
        if (item.today > 0f) Text("Astăzi: +${formatQuantity(item.today)} ${item.unit}", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TimeEntriesContent(
    entries: List<SiteTimeEntryRow>,
    users: Map<Long, String>,
    ownUserId: Long?,
    canCreateOwn: Boolean,
    canReview: Boolean,
    onAdd: (Long) -> Unit,
) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (canCreateOwn && ownUserId != null) {
            Button(onClick = { onAdd(ownUserId) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null); Text("  Adaugă pontarea mea")
            }
        }
        if (canReview && users.isNotEmpty()) {
            Text("Pontare pentru membru", fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                users.forEach { (id, name) -> AssistChip(onClick = { onAdd(id) }, label = { Text(name) }) }
            }
        }
        if (entries.isEmpty()) EmptyText("Nu există pontări pentru acest proiect.")
        entries.forEach { entry ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(entry.employee, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(entry.date?.toString() ?: "—")
                    }
                    Text("${entry.work} · ${entry.zone}")
                    Text("${entry.hours} ore · ${formatQuantity(entry.quantity)} ${entry.unit} · calitate ${entry.quality ?: 0f}/10")
                }
            }
        }
    }
}

@Composable
private fun QualityContent(
    corrections: List<SiteCorrection>,
    users: Map<Long, String>,
    canManage: Boolean,
    onAdd: () -> Unit,
    onStatus: (Long, String) -> Unit,
) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (canManage) Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Add, null); Text("  Corectură nouă") }
        if (corrections.isEmpty()) EmptyText("Nu există corecturi sau probleme de calitate.")
        corrections.forEach { correction ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(correction.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(severityLabel(correction.severity), color = MaterialTheme.colorScheme.primary)
                    }
                    Text(correction.description)
                    Text("Responsabil: ${correction.assignedTo?.let(users::get) ?: "Nealocat"}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canManage && correction.status == "deschisa") OutlinedButton(onClick = { onStatus(correction.id, "in_lucru") }) { Text("În lucru") }
                        if (canManage && correction.status != "rezolvata") Button(onClick = { onStatus(correction.id, "rezolvata") }) { Text("Rezolvată") }
                        if (correction.status == "rezolvata") AssistChip(onClick = {}, label = { Text("Rezolvată") })
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalContent(entries: List<SiteJournalEntry>, photoUrls: Map<String, String>, onAdd: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Add, null); Text("  Adaugă în jurnal") }
        if (entries.isEmpty()) EmptyText("Jurnalul șantierului este gol.")
        entries.forEach { entry ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(entry.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(journalTypeLabel(entry.type), color = MaterialTheme.colorScheme.primary)
                    }
                    Text(entry.description)
                    entry.photoPath?.let { path ->
                        val url = photoUrls[path]
                        OutlinedButton(onClick = { if (url != null) uriHandler.openUri(url) }, enabled = url != null) {
                            Icon(Icons.Filled.PhotoCamera, null)
                            Text("  Deschide fotografia")
                        }
                    }
                    Text(entry.date.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CorrectionDialog(users: Map<Long, String>, onDismiss: () -> Unit, onSave: (String, String, String, Long?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("medie") }
    var assignee by remember { mutableStateOf<Long?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Corectură nouă") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Titlu") })
                OutlinedTextField(description, { description = it }, label = { Text("Descriere") }, minLines = 3)
                Text("Severitate")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("scazuta", "medie", "ridicata", "critica").forEach { value ->
                        FilterChip(severity == value, { severity = value }, { Text(severityLabel(value)) })
                    }
                }
                Text("Responsabil")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    users.forEach { (id, name) -> FilterChip(assignee == id, { assignee = id }, { Text(name) }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, description, severity, assignee) }) { Text("Salvează") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    )
}

@Composable
private fun JournalDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, ByteArray?, String?) -> Unit,
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("observatie") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("medie") }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var mimeType by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            mimeType = context.contentResolver.getType(uri)
            photoBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            type = "fotografie"
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Jurnal șantier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("observatie" to "Observație", "incident" to "Incident").forEach { (value, label) ->
                        FilterChip(type == value, { type = value; photoBytes = null }, { Text(label) })
                    }
                }
                OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PhotoCamera, null)
                    Text(if (photoBytes == null) "  Alege fotografie" else "  Fotografie selectată")
                }
                OutlinedTextField(title, { title = it }, label = { Text("Titlu") })
                OutlinedTextField(description, { description = it }, label = { Text("Descriere") }, minLines = 3)
                if (type == "incident") Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("scazuta", "medie", "ridicata", "critica").forEach { value ->
                        FilterChip(severity == value, { severity = value }, { Text(severityLabel(value)) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(type, title, description, severity, photoBytes, mimeType) }) { Text("Adaugă") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    )
}

@Composable
private fun CloseDayDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var summary by remember { mutableStateOf("") }
    var blockers by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Închiderea zilei") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(summary, { summary = it }, label = { Text("Rezumatul zilei") }, minLines = 3)
                OutlinedTextField(blockers, { blockers = it }, label = { Text("Blocaje") })
                OutlinedTextField(next, { next = it }, label = { Text("Plan pentru următoarea zi") })
                Text("După închidere, prezența și jurnalul zilei nu mai pot fi modificate din acest ecran.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onSave(summary, blockers, next) }) { Text("Închide ziua") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable private fun EmptyText(value: String) = Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
private fun formatQuantity(value: Float): String = if (value % 1f == 0f) value.roundToInt().toString() else "%.1f".format(value)
private fun attendanceLabel(value: String?): String = when (value) { "prezent" -> "Prezent"; "absent" -> "Absent"; "concediu" -> "Concediu"; "medical" -> "Medical"; else -> "Neraportat" }
private fun severityLabel(value: String): String = when (value) { "scazuta" -> "Scăzută"; "ridicata" -> "Ridicată"; "critica" -> "Critică"; else -> "Medie" }
private fun journalTypeLabel(value: String): String = when (value) { "incident" -> "Incident"; "fotografie" -> "Fotografie"; else -> "Observație" }
