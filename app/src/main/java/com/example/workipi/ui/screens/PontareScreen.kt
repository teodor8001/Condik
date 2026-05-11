package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.UserRole
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.ui.components.SuccessToast
import com.example.workipi.viewmodel.StandalonePontareViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PontareScreen(
    navController: NavController,
    viewModel: StandalonePontareViewModel = hiltViewModel(),
) {
    val currentUser = MockSession.currentUser
    val openDrawer = LocalOpenDrawer.current
    val focusManager = LocalFocusManager.current

    if (currentUser == null ||
        (currentUser.role != UserRole.ADMIN && currentUser.role != UserRole.PROJECT_MANAGER)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (openDrawer != null) {
                IconButton(
                    onClick = { openDrawer() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Deschide meniu")
                }
            }
            Text(
                text = "Nu ai acces la aceasta sectiune.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        return
    }

    val state by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var zoneMenuExpanded by remember { mutableStateOf(false) }
    var skillMenuExpanded by remember { mutableStateOf(false) }
    var employeeMenuExpanded by remember { mutableStateOf(false) }

    val selectedProject = state.projects.firstOrNull { it.projectId == state.selectedProjectId }
    val selectedZone = state.zonesForProject.firstOrNull { it.id == state.selectedZoneId }
    val selectedSkill = state.skills.firstOrNull { it.id == state.selectedSkillId }

    val earnedPointsText = remember(state.selectedSkillId, state.quantity) {
        val qty = state.quantity.toFloatOrNull() ?: 0f
        if (selectedSkill != null && qty > 0f) "${(selectedSkill.points * qty.toInt())} pts" else "—"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (openDrawer != null) {
                    IconButton(onClick = { openDrawer() }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Deschide meniu")
                    }
                }
                Text(
                    text = "Pontare",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Proiect
                        ExposedDropdownMenuBox(
                            expanded = projectMenuExpanded,
                            onExpandedChange = { projectMenuExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedProject?.title ?: "Alege proiect",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Proiect") },
                                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(10.dp),
                            )
                            ExposedDropdownMenu(
                                expanded = projectMenuExpanded,
                                onDismissRequest = { projectMenuExpanded = false },
                            ) {
                                state.projects.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.title) },
                                        onClick = {
                                            viewModel.selectProject(p.projectId)
                                            projectMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        // Zona (apare doar daca avem proiect selectat)
                        if (state.selectedProjectId != null) {
                            ExposedDropdownMenuBox(
                                expanded = zoneMenuExpanded,
                                onExpandedChange = { zoneMenuExpanded = it },
                            ) {
                                OutlinedTextField(
                                    value = selectedZone?.name ?: "Alege zona",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Zona") },
                                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(10.dp),
                                )
                                ExposedDropdownMenu(
                                    expanded = zoneMenuExpanded,
                                    onDismissRequest = { zoneMenuExpanded = false },
                                ) {
                                    state.zonesForProject.forEach { z ->
                                        DropdownMenuItem(
                                            text = { Text("${z.name ?: "Zona"} • ${z.surface.toInt()} mp") },
                                            onClick = {
                                                viewModel.selectZone(z.id)
                                                zoneMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        // Angajat (cu cautare)
                        ExposedDropdownMenuBox(
                            expanded = employeeMenuExpanded,
                            onExpandedChange = { employeeMenuExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = state.employeeQuery,
                                onValueChange = {
                                    viewModel.onEmployeeQueryChange(it)
                                    employeeMenuExpanded = true
                                },
                                label = { Text("Angajat") },
                                placeholder = { Text("Cauta dupa nume...") },
                                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                            )
                            ExposedDropdownMenu(
                                expanded = employeeMenuExpanded && state.filteredEmployees.isNotEmpty(),
                                onDismissRequest = { employeeMenuExpanded = false },
                            ) {
                                state.filteredEmployees.forEach { user ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(user.fullName)
                                                Text(
                                                    text = user.role?.replaceFirstChar { it.uppercase() } ?: "Angajat",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectEmployee(user.idUser)
                                            employeeMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        // Lucrare
                        ExposedDropdownMenuBox(
                            expanded = skillMenuExpanded,
                            onExpandedChange = { skillMenuExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedSkill?.let { "${it.name} (${it.unit})" } ?: "Alege lucrare",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tip lucrare") },
                                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(10.dp),
                            )
                            ExposedDropdownMenu(
                                expanded = skillMenuExpanded,
                                onDismissRequest = { skillMenuExpanded = false },
                            ) {
                                state.skills.forEach { lucrare ->
                                    DropdownMenuItem(
                                        text = { Text("${lucrare.name} • ${lucrare.points} pts/${lucrare.unit}") },
                                        onClick = {
                                            viewModel.selectSkill(lucrare.id)
                                            skillMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.quantity,
                            onValueChange = viewModel::onQuantityChange,
                            label = {
                                Text("Cantitate" + (selectedSkill?.let { " (${it.unit})" } ?: ""))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            shape = RoundedCornerShape(10.dp),
                        )

                        OutlinedTextField(
                            value = state.hours,
                            onValueChange = viewModel::onHoursChange,
                            label = { Text("Ore lucrate") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            shape = RoundedCornerShape(10.dp),
                        )

                        DateField(
                            label = "Data pontarii",
                            millis = state.workDateMillis,
                            onClick = { showDatePicker = true },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Punctaj cuvenit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = earnedPointsText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        state.errorMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        Button(
                            onClick = { viewModel.submit() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !state.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Salveaza pontaj", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SuccessToast(
                message = "Pontajul a fost salvat cu succes!",
                visible = state.saved,
                onDismiss = { viewModel.consumeSaved() }
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.workDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { viewModel.onWorkDateChange(it) }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Anuleaza") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DateField(label: String, millis: Long, onClick: () -> Unit) {
    val formatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale("ro", "RO")).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val display = formatter.format(Date(millis))

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = display,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
