package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
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
import com.example.workipi.navigation.Screen
import com.example.workipi.viewmodel.AddProjectViewModel
import com.example.workipi.viewmodel.ZoneDraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectScreen(
    navController: NavController,
    viewModel: AddProjectViewModel = hiltViewModel(),
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.uiState.collectAsState()

    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.createdProject) {
        val created = state.createdProject
        if (created != null) {
            viewModel.consumeCreatedProject()
            val destination = if (created.isOffer)
                Screen.Ofertare.route
            else
                Screen.ProjectDetail.createRoute(created.projectId)
            navController.navigate(destination) {
                popUpTo(Screen.AddProject.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Inapoi")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Adauga proiect",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Completeaza datele proiectului",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text("Denumire proiect") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        shape = RoundedCornerShape(10.dp),
                    )

                    OutlinedTextField(
                        value = state.address,
                        onValueChange = viewModel::onAddressChange,
                        label = { Text("Adresa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        shape = RoundedCornerShape(10.dp),
                    )

                    OutlinedTextField(
                        value = state.budget,
                        onValueChange = viewModel::onBudgetChange,
                        label = { Text("Buget (RON)") },
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
                        label = "Termen finalizare",
                        millis = state.endDateMillis,
                        onClick = { showEndDatePicker = true },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HasZonesToggleCard(
                hasZones = state.hasZones,
                onChange = viewModel::onHasZonesChange,
            )

            if (state.hasZones) {
                Spacer(modifier = Modifier.height(16.dp))
                ZonesCard(
                    zones = state.zones,
                    totalSurface = state.zones.sumOf { (it.surface.toFloatOrNull() ?: 0f).toDouble() },
                    onNameChange = viewModel::onZoneNameChange,
                    onSurfaceChange = viewModel::onZoneSurfaceChange,
                    onAddZone = viewModel::addZone,
                    onRemoveZone = viewModel::removeZone,
                    focusManager = focusManager,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            EmployeesCard(
                employees = state.availableEmployees,
                selectedIds = state.selectedEmployeeIds,
                onToggle = viewModel::toggleEmployee,
            )

            Spacer(modifier = Modifier.height(16.dp))

            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Button(
                onClick = { viewModel.submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Salveaza proiect",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showEndDatePicker) {
        DatePickerSheet(
            initialMillis = state.endDateMillis,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { millis ->
                viewModel.onEndDateChange(millis)
                showEndDatePicker = false
            },
        )
    }
}

@Composable
private fun IsOfferToggleCard(
    isOffer: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Salveaza ca oferta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (isOffer)
                        "Va aparea in sectiunea Ofertare, nu in Proiecte."
                    else
                        "Va fi proiect activ, vizibil in lista de proiecte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = isOffer, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun EmployeesCard(
    employees: List<com.example.workipi.data.model.User>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Echipa (${selectedIds.size} selectati)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (employees.isEmpty()) {
                Text(
                    text = "Niciun angajat in firma.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                employees.forEach { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = user.idUser in selectedIds,
                            onCheckedChange = { onToggle(user.idUser) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = user.role?.replaceFirstChar { it.uppercase() } ?: "Angajat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HasZonesToggleCard(
    hasZones: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Proiect cu zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (hasZones)
                        "Lucrarile se vor adauga pe fiecare zona."
                    else
                        "Lucrarile se vor adauga direct pe proiect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = hasZones, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun ZonesCard(
    zones: List<ZoneDraft>,
    totalSurface: Double,
    onNameChange: (Long, String) -> Unit,
    onSurfaceChange: (Long, String) -> Unit,
    onAddZone: () -> Unit,
    onRemoveZone: (Long) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                Column {
                    Text(
                        text = "Zone proiect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Total suprafata: ${totalSurface.toInt()} mp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = onAddZone,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Adauga zona")
                }
            }

            zones.forEach { zone ->
                ZoneRow(
                    zone = zone,
                    canRemove = zones.size > 1,
                    onNameChange = { onNameChange(zone.key, it) },
                    onSurfaceChange = { onSurfaceChange(zone.key, it) },
                    onRemove = { onRemoveZone(zone.key) },
                    focusManager = focusManager,
                )
            }
        }
    }
}

@Composable
private fun ZoneRow(
    zone: ZoneDraft,
    canRemove: Boolean,
    onNameChange: (String) -> Unit,
    onSurfaceChange: (String) -> Unit,
    onRemove: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = zone.name,
            onValueChange = onNameChange,
            label = { Text("Nume") },
            singleLine = true,
            modifier = Modifier.weight(2f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            shape = RoundedCornerShape(10.dp),
        )
        OutlinedTextField(
            value = zone.surface,
            onValueChange = onSurfaceChange,
            label = { Text("mp") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            shape = RoundedCornerShape(10.dp),
        )
        IconButton(
            onClick = onRemove,
            enabled = canRemove,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Sterge zona",
                tint = if (canRemove) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DateField(label: String, millis: Long?, onClick: () -> Unit) {
    val formatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale("ro", "RO")).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val display = millis?.let { formatter.format(Date(it)) } ?: "Alege data"

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = display,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (millis == null)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onBackground
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    pickerState.selectedDateMillis?.let(onConfirm)
                },
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuleaza") }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
