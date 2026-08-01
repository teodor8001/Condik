package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.MeasureUnit
import com.example.workipi.data.model.Unealta
import com.example.workipi.ui.components.ConfirmDialog
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.viewmodel.PreturiViewModel
import com.example.workipi.viewmodel.UneltaViewModel

private enum class FirmaTab(val label: String) { LUCRARI("Lucrari"), MATERIALE("Materiale"), UNELTE("Unelte") }

@Composable
fun FirmaScreen(navController: NavController) {
    val openDrawer = LocalOpenDrawer.current
    var tab by remember { mutableStateOf(FirmaTab.LUCRARI) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (openDrawer != null) {
                IconButton(onClick = { openDrawer() }) {
                    Icon(Icons.Filled.Menu, contentDescription = "Deschide meniu", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Firma", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("Lucrari, materiale, unelte", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        TabRow(
            selectedTabIndex = tab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            FirmaTab.entries.forEach { t ->
                Tab(
                    selected = tab == t,
                    onClick = { tab = t },
                    text = { Text(t.label, fontWeight = if (tab == t) FontWeight.SemiBold else FontWeight.Normal) },
                )
            }
        }

        when (tab) {
            FirmaTab.LUCRARI -> LucrariTab()
            FirmaTab.MATERIALE -> MaterialeTab()
            FirmaTab.UNELTE -> UnelteTab()
        }
    }
}

// ----------------------------- Tab Lucrari (fostul Preturi) -----------------------------

@Composable
private fun LucrariTab(viewModel: PreturiViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var editingSkill by remember { mutableStateOf<Lucrare?>(null) }
    var deletingSkill by remember { mutableStateOf<Lucrare?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${state.skills.size} lucrari definite", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Lucrare noua")
            }
        }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        when {
            state.isLoading && state.skills.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            state.skills.isEmpty() ->
                Text("Nu ai inca lucrari definite. Apasa „Lucrare noua\".", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Lucrare", Modifier.weight(2f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Unitate", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Pret/unit", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Puncte", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(32.dp))
                }
                LazyColumn {
                    itemsIndexed(state.skills) { index, skill ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(skill.name, Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                            Text(skill.unit, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${skill.price.toInt()} RON", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${skill.points} pts", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { editingSkill = skill }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editeaza", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { deletingSkill = skill }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Sterge", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (index < state.skills.lastIndex) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }

    editingSkill?.let { skill ->
        SkillDialog(
            title = "Editeaza lucrare",
            initialName = skill.name,
            initialUnit = parseUnit(skill.unit),
            initialPrice = skill.price.toInt().toString(),
            initialPoints = skill.points.toString(),
            isSaving = state.isSaving,
            onDismiss = { editingSkill = null },
            onConfirm = { name, unit, price, points -> viewModel.updateSkill(skill.id, name, unit.label, price, points); editingSkill = null },
        )
    }
    deletingSkill?.let { skill ->
        ConfirmDialog(
            title = "Esti sigur?",
            message = "Lucrarea \"${skill.name}\" va fi stearsa.",
            onConfirm = { viewModel.deleteSkill(skill.id); deletingSkill = null },
            onDismiss = { deletingSkill = null },
        )
    }
    if (showAddDialog) {
        SkillDialog(
            title = "Lucrare noua",
            initialName = "",
            initialUnit = MeasureUnit.MP,
            initialPrice = "",
            initialPoints = "",
            isSaving = state.isSaving,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, unit, price, points -> viewModel.createSkill(name, unit.label, price, points); showAddDialog = false },
        )
    }
}

// ----------------------------- Tab Materiale (placeholder) -----------------------------

@Composable
private fun MaterialeTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Materialele sunt gestionate per proiect deocamdata.\nCatalogul de materiale la nivel de firma vine in curand.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ----------------------------- Tab Unelte -----------------------------

@Composable
private fun UnelteTab(viewModel: UneltaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Unealta?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${state.tools.size} unelte", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { showAdd = true },
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Unealta noua")
            }
        }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        when {
            state.isLoading && state.tools.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            state.tools.isEmpty() ->
                Text("Nicio unealta. Apasa „Unealta noua\".", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(state.tools) { _, tool -> UnealtaRow(tool, onDelete = { deleting = tool }) }
            }
        }
    }

    if (showAdd) {
        UnealtaDialog(
            isSaving = state.isSaving,
            onDismiss = { showAdd = false },
            onConfirm = { name, total -> viewModel.addTool(name, total); showAdd = false },
        )
    }
    deleting?.let { tool ->
        ConfirmDialog(
            title = "Esti sigur?",
            message = "Unealta \"${tool.name}\" va fi stearsa.",
            onConfirm = { viewModel.deleteTool(tool.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun UnealtaRow(tool: Unealta, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tool.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatChip("Total", "${tool.totalQuantity}", MaterialTheme.colorScheme.onSurfaceVariant)
                    StatChip("Disponibile", "${tool.availableQuantity}", MaterialTheme.colorScheme.primary)
                    StatChip("In uz", "${tool.inUse}", MaterialTheme.colorScheme.error)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Sterge", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UnealtaDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, total: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    val parsedTotal = total.toIntOrNull()
    val isValid = name.isNotBlank() && parsedTotal != null && parsedTotal > 0

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Unealta noua", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Denumire (ex. Ciocan)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = total, onValueChange = { total = it.filter { c -> c.isDigit() } }, label = { Text("Cantitate totala") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Anuleaza") }
                    Button(
                        onClick = { onConfirm(name, parsedTotal ?: 0) },
                        enabled = isValid && !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text("Salveaza") }
                }
            }
        }
    }
}

private fun parseUnit(text: String): MeasureUnit =
    MeasureUnit.entries.firstOrNull { it.label.equals(text, ignoreCase = true) } ?: MeasureUnit.MP

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillDialog(
    title: String,
    initialName: String,
    initialUnit: MeasureUnit,
    initialPrice: String,
    initialPoints: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, unit: MeasureUnit, price: Float, points: Long) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var unit by remember { mutableStateOf(initialUnit) }
    var price by remember { mutableStateOf(initialPrice) }
    var points by remember { mutableStateOf(initialPoints) }
    var unitExpanded by remember { mutableStateOf(false) }

    val parsedPrice = price.toFloatOrNull()
    val parsedPoints = points.toLongOrNull()
    val isValid = name.isNotBlank() && parsedPrice != null && parsedPrice >= 0f && parsedPoints != null && parsedPoints >= 0L

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nume lucrare") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(
                        value = unit.label, onValueChange = {}, readOnly = true,
                        label = { Text("Unitate de masura") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(10.dp),
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        MeasureUnit.entries.forEach { u ->
                            DropdownMenuItem(text = { Text(u.label) }, onClick = { unit = u; unitExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Pret per ${unit.label} (RON)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = points, onValueChange = { points = it.filter { c -> c.isDigit() } }, label = { Text("Puncte per ${unit.label}") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Anuleaza") }
                    Button(
                        onClick = { onConfirm(name, unit, parsedPrice ?: 0f, parsedPoints ?: 0L) },
                        enabled = isValid && !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Salveaza")
                    }
                }
            }
        }
    }
}
