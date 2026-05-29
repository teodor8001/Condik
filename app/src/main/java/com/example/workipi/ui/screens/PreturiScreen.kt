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
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.viewmodel.PreturiViewModel

@Composable
fun PreturiScreen(
    navController: NavController,
    viewModel: PreturiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    var editingSkill by remember { mutableStateOf<Lucrare?>(null) }
    var deletingSkill by remember { mutableStateOf<Lucrare?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val openDrawer = LocalOpenDrawer.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = "Preturi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${state.skills.size} skill-uri definite",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Skill nou")
            }
        }

        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        when {
            state.isLoading && state.skills.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.skills.isEmpty() -> {
                Text(
                    text = "Nu ai inca skills definite. Apasa „Skill nou\".",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Skill", modifier = Modifier.weight(2f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Unitate", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Pret/unit", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Puncte/unit", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(32.dp))
                    }

                    LazyColumn {
                        itemsIndexed(state.skills) { index, skill ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(skill.name, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                                Text(skill.unit, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${skill.price.toInt()} RON", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${skill.points} pts", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                IconButton(
                                    onClick = { editingSkill = skill },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editeaza", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { deletingSkill = skill },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Sterge", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < state.skills.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingSkill?.let { skill ->
        SkillDialog(
            title = "Editeaza skill",
            initialName = skill.name,
            initialUnit = parseUnit(skill.unit),
            initialPrice = skill.price.toInt().toString(),
            initialPoints = skill.points.toString(),
            isSaving = state.isSaving,
            onDismiss = { editingSkill = null },
            onConfirm = { name, unit, price, points ->
                viewModel.updateSkill(skill.id, name, unit.label, price, points)
                editingSkill = null
            },
        )
    }

    deletingSkill?.let { skill ->
        AlertDialog(
            onDismissRequest = { deletingSkill = null },
            title = { Text("Sterge lucrare") },
            text = { Text("Esti sigur ca vrei sa stergi \"${skill.name}\"? Aceasta actiune nu poate fi anulata.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSkill(skill.id)
                    deletingSkill = null
                }) { Text("Sterge", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingSkill = null }) { Text("Anuleaza") }
            },
        )
    }

    if (showAddDialog) {
        SkillDialog(
            title = "Skill nou",
            initialName = "",
            initialUnit = MeasureUnit.MP,
            initialPrice = "",
            initialPoints = "",
            isSaving = state.isSaving,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, unit, price, points ->
                viewModel.createSkill(name, unit.label, price, points)
                showAddDialog = false
            },
        )
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
    val isValid = name.isNotBlank() && parsedPrice != null && parsedPrice >= 0f
            && parsedPoints != null && parsedPoints >= 0L

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nume skill") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )

                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(
                        value = unit.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unitate de masura") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp),
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        MeasureUnit.entries.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u.label) },
                                onClick = { unit = u; unitExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Pret per ${unit.label} (RON)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                )

                OutlinedTextField(
                    value = points,
                    onValueChange = { points = it.filter { c -> c.isDigit() } },
                    label = { Text("Puncte per ${unit.label}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Anuleaza") }

                    Button(
                        onClick = {
                            onConfirm(name, unit, parsedPrice ?: 0f, parsedPoints ?: 0L)
                        },
                        enabled = isValid && !isSaving,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Salveaza")
                        }
                    }
                }
            }
        }
    }
}
