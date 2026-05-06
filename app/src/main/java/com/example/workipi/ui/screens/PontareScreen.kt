package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.workipi.data.mock.MockData
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.MockEmployee
import com.example.workipi.data.model.MockProject
import com.example.workipi.data.model.Skill
import com.example.workipi.data.model.UserRole
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.ui.components.SuccessToast

private data class SkillEntry(
    val skill: Skill? = null,
    val quantity: String = ""
) {
    val points: Double
        get() = (skill?.pointsPerUnit ?: 0.0) * (quantity.toDoubleOrNull() ?: 0.0)
}

@Composable
fun PontareScreen(navController: NavController) {
    val currentUser = MockSession.currentUser
    val openDrawer  = LocalOpenDrawer.current

    if (currentUser == null ||
        (currentUser.role != UserRole.ADMIN && currentUser.role != UserRole.PROJECT_MANAGER)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (openDrawer != null) {
                IconButton(
                    onClick  = { openDrawer() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Menu,
                        contentDescription = "Deschide meniu",
                        tint               = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Text(
                text     = "Nu ai acces la aceasta sectiune.",
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    var selectedEmployee by remember { mutableStateOf<MockEmployee?>(null) }
    var selectedMockProject  by remember { mutableStateOf<MockProject?>(null) }
    var skillEntries     by remember { mutableStateOf(listOf<SkillEntry>()) }
    var submitSuccess    by remember { mutableStateOf(false) }

    val totalPoints = skillEntries.sumOf { it.points }
    val canSubmit   = selectedEmployee != null &&
                      selectedMockProject  != null &&
                      skillEntries.isNotEmpty() &&
                      skillEntries.all { it.skill != null && (it.quantity.toDoubleOrNull() ?: 0.0) > 0.0 }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ---- Header FIX (nu scrolleaza) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
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
                Text(
                    text = "Pontare",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // ---- Continut scrollabil ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // Selectie angajat
                    PontareDropdown(
                        label       = "Angajat",
                        placeholder = "Selecteaza angajat",
                        selected    = selectedEmployee?.name,
                        options     = MockData.employees.map { it.name },
                        onSelect    = { name ->
                            selectedEmployee = MockData.employees.find { it.name == name }
                            skillEntries = listOf()
                            submitSuccess = false
                        }
                    )

                    // Selectie proiect
                    PontareDropdown(
                        label       = "Proiect",
                        placeholder = "Selecteaza proiect",
                        selected    = selectedMockProject?.name,
                        options     = MockData.mockProjects.map { it.name },
                        onSelect    = { name ->
                            selectedMockProject = MockData.mockProjects.find { it.name == name }
                            submitSuccess = false
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Skill-uri
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Activitate zilnica",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        skillEntries.forEachIndexed { index, entry ->
                            SkillEntryRow(
                                entry    = entry,
                                onChange = { updated ->
                                    skillEntries = skillEntries.toMutableList().also { it[index] = updated }
                                },
                                onRemove = {
                                    skillEntries = skillEntries.toMutableList().also { it.removeAt(index) }
                                }
                            )
                        }

                        OutlinedButton(
                            onClick = { skillEntries = skillEntries + SkillEntry() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Adauga skill")
                        }
                    }

                    // Total puncte
                    if (skillEntries.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total puncte sesiune",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+ ${totalPoints.toInt()} pts",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Submit
                    Button(
                        onClick = {
                            if (canSubmit) {
                                // Mock — va fi inlocuit cu Supabase
                                submitSuccess = true
                                selectedEmployee = null
                                selectedMockProject  = null
                                skillEntries     = listOf()
                            }
                        },
                        enabled  = canSubmit,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Salveaza pontaj", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            } // sfarsit Card
            } // sfarsit Column scrollabil
        } // sfarsit Column exterior (background)

        // Toast overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SuccessToast(
                message   = "Pontajul a fost salvat cu succes!",
                visible   = submitSuccess,
                onDismiss = { submitSuccess = false }
            )
        }
    }
}

@Composable
private fun SkillEntryRow(
    entry: SkillEntry,
    onChange: (SkillEntry) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Skill",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Sterge",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            PontareDropdown(
                label       = "",
                placeholder = "Selecteaza skill",
                selected    = entry.skill?.name,
                options     = MockData.skills.map { it.name },
                onSelect    = { name ->
                    val skill = MockData.skills.find { it.name == name }
                    onChange(entry.copy(skill = skill, quantity = ""))
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = entry.quantity,
                    onValueChange = { onChange(entry.copy(quantity = it)) },
                    label         = { Text("Cantitate") },
                    placeholder   = { Text("0") },
                    modifier      = Modifier.weight(1f),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape  = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    trailingIcon = if (entry.skill != null) {
                        {
                            Text(
                                text = entry.skill.unit.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    } else null
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+ ${entry.points.toInt()} pts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (entry.points > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.skill != null) {
                        Text(
                            text = "${entry.skill.pointsPerUnit} pts/${entry.skill.unit.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PontareDropdown(
    label: String,
    placeholder: String,
    selected: String?,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label.isNotBlank()) {
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ExposedDropdownMenuBox(
            expanded         = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value         = selected ?: "",
                onValueChange = {},
                readOnly      = true,
                placeholder   = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier      = Modifier.fillMaxWidth().menuAnchor(),
                shape  = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            ExposedDropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text    = { Text(option) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}