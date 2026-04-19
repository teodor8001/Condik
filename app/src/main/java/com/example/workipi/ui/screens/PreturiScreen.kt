package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.workipi.data.mock.MockData
import com.example.workipi.data.model.MeasureUnit
import com.example.workipi.data.model.Skill
import com.example.workipi.ui.components.LocalOpenDrawer

@Composable
fun PreturiScreen(navController: NavController) {
    // Stare locala care reflecta lista de skill-uri (mock)
    var skills        by remember { mutableStateOf(MockData.skills.toList()) }
    var editingSkill  by remember { mutableStateOf<Skill?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    val openDrawer    = LocalOpenDrawer.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ---- Header ----
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
                        text = "${skills.size} skill-uri definite",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = { showAddDialog = true },
                shape  = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Skill nou")
            }
        }

        // ---- Header tabel ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            // Titluri coloane
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Skill",        modifier = Modifier.weight(2f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Unitate",      modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Pret/unit",    modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Puncte/unit",  modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(32.dp))
            }

            LazyColumn {
                itemsIndexed(skills) { index, skill ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(skill.name,                         modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                        Text(skill.unit.label,                   modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${skill.pricePerUnit} RON",        modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${skill.pointsPerUnit} pts",       modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        IconButton(
                            onClick  = { editingSkill = skill },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editeaza", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (index < skills.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }

    // ---- Dialog editare skill ----
    editingSkill?.let { skill ->
        SkillDialog(
            title      = "Editeaza skill",
            initial    = skill,
            onDismiss  = { editingSkill = null },
            onConfirm  = { updated ->
                MockData.skills.replaceAll { if (it.id == updated.id) updated else it }
                skills       = MockData.skills.toList()
                editingSkill = null
            }
        )
    }

    // ---- Dialog adaugare skill nou ----
    if (showAddDialog) {
        val newId = "s${System.currentTimeMillis()}"
        SkillDialog(
            title     = "Skill nou",
            initial   = Skill(newId, "", MeasureUnit.MP, 0.0, 0.0),
            onDismiss = { showAddDialog = false },
            onConfirm = { newSkill ->
                MockData.skills.add(newSkill)
                skills        = MockData.skills.toList()
                showAddDialog = false
            }
        )
    }
}

// ---- Dialog reutilizabil: edit + add ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillDialog(
    title: String,
    initial: Skill,
    onDismiss: () -> Unit,
    onConfirm: (Skill) -> Unit
) {
    var name          by remember { mutableStateOf(initial.name) }
    var unit          by remember { mutableStateOf(initial.unit) }
    var pricePerUnit  by remember { mutableStateOf(if (initial.pricePerUnit == 0.0) "" else initial.pricePerUnit.toString()) }
    var pointsPerUnit by remember { mutableStateOf(if (initial.pointsPerUnit == 0.0) "" else initial.pointsPerUnit.toString()) }
    var unitExpanded  by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() &&
                  (pricePerUnit.toDoubleOrNull()  ?: -1.0) >= 0.0 &&
                  (pointsPerUnit.toDoubleOrNull() ?: -1.0) >= 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Nume skill
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Nume skill") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp),
                    colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                // Unitate masura dropdown
                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                    OutlinedTextField(
                        value         = unit.label,
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Unitate de masura") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier      = Modifier.fillMaxWidth().menuAnchor(),
                        shape         = RoundedCornerShape(10.dp),
                        colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        MeasureUnit.entries.forEach { u ->
                            DropdownMenuItem(
                                text    = { Text(u.label) },
                                onClick = { unit = u; unitExpanded = false }
                            )
                        }
                    }
                }

                // Pret per unitate
                OutlinedTextField(
                    value           = pricePerUnit,
                    onValueChange   = { pricePerUnit = it },
                    label           = { Text("Pret per ${unit.label} (RON)") },
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape           = RoundedCornerShape(10.dp),
                    colors          = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                // Puncte per unitate
                OutlinedTextField(
                    value           = pointsPerUnit,
                    onValueChange   = { pointsPerUnit = it },
                    label           = { Text("Puncte per ${unit.label}") },
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape           = RoundedCornerShape(10.dp),
                    colors          = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                // Butoane
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp)
                    ) { Text("Anuleaza") }

                    Button(
                        onClick  = {
                            onConfirm(
                                initial.copy(
                                    name          = name.trim(),
                                    unit          = unit,
                                    pricePerUnit  = pricePerUnit.toDoubleOrNull()  ?: 0.0,
                                    pointsPerUnit = pointsPerUnit.toDoubleOrNull() ?: 0.0
                                )
                            )
                        },
                        enabled  = isValid,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Salveaza") }
                }
            }
        }
    }
}