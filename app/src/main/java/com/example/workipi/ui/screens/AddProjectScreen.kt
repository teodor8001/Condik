package com.example.workipi.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.example.workipi.data.model.Lucrare
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.ConfirmDialog
import com.example.workipi.viewmodel.AddProjectUiState
import com.example.workipi.viewmodel.AddProjectViewModel
import com.example.workipi.viewmodel.OfferLucrareDraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectScreen(
    navController: NavController,
    isOffer: Boolean = false,
    viewModel: AddProjectViewModel = hiltViewModel(),
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(isOffer) { viewModel.setOffer(isOffer) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    // Butonul back fizic cere confirmare, ca sa nu pierzi datele din greseala.
    BackHandler(enabled = !showCancelConfirm) { showCancelConfirm = true }

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
            onClick = { showCancelConfirm = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Renunta")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.isOffer) "Adauga oferta" else "Adauga proiect",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (state.isOffer) "Completeaza datele ofertei" else "Completeaza datele proiectului",
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
                        label = "Data de start",
                        millis = state.startDateMillis,
                        onClick = { showStartDatePicker = true },
                    )

                    DateField(
                        label = "Termen finalizare",
                        millis = state.endDateMillis,
                        onClick = { showEndDatePicker = true },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EmployeesCard(
                employees = state.availableEmployees,
                selectedIds = state.selectedEmployeeIds,
                onToggle = viewModel::toggleEmployee,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LucrariCeruteCard(
                skills = state.availableSkills,
                required = state.requiredLucrari,
                onAdd = viewModel::addRequiredLucrare,
                onRemove = viewModel::removeRequiredLucrare,
                onSelect = viewModel::onRequiredLucrareSelect,
                onQuantityChange = viewModel::onRequiredQuantityChange,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OfferSummaryCard(state = state)

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
                        text = if (state.isOffer) "Salveaza oferta" else "Salveaza proiect",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showCancelConfirm) {
        ConfirmDialog(
            title = if (state.isOffer) "Renunti la oferta?" else "Renunti la proiect?",
            message = "Datele introduse pana acum se vor pierde.",
            onConfirm = { showCancelConfirm = false; navController.popBackStack() },
            onDismiss = { showCancelConfirm = false },
            confirmLabel = "Da, renunt",
            dismissLabel = "Nu",
        )
    }

    if (showStartDatePicker) {
        DatePickerSheet(
            initialMillis = state.startDateMillis,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { millis ->
                viewModel.onStartDateChange(millis)
                showStartDatePicker = false
            },
        )
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

// ---------------- Oferta: lucrari cerute + sumar (cheltuieli + recomandare personal) ----------------

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

private fun durationDays(startMillis: Long?, endMillis: Long?): Int {
    if (startMillis == null || endMillis == null) return 0
    return ((endMillis - startMillis) / DAY_MILLIS).toInt().coerceAtLeast(1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LucrariCeruteCard(
    skills: List<Lucrare>,
    required: List<OfferLucrareDraft>,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
    onSelect: (Long, Long) -> Unit,
    onQuantityChange: (Long, String) -> Unit,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lucrari cerute", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pentru estimarea personalului necesar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = onAdd, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Adauga")
                }
            }

            if (required.isEmpty()) {
                Text(
                    "Adauga lucrarile cerute ca sa estimam cati angajati iti trebuie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                required.forEach { row ->
                    LucrareCerutaRow(
                        row = row,
                        skills = skills,
                        onSelect = { onSelect(row.key, it) },
                        onQuantityChange = { onQuantityChange(row.key, it) },
                        onRemove = { onRemove(row.key) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LucrareCerutaRow(
    row: OfferLucrareDraft,
    skills: List<Lucrare>,
    onSelect: (Long) -> Unit,
    onQuantityChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExposedDropdownMenuBox(
            expanded = menu,
            onExpandedChange = { menu = it },
            modifier = Modifier.weight(2f),
        ) {
            OutlinedTextField(
                value = row.lucrareName.ifBlank { "Alege" },
                onValueChange = {}, readOnly = true,
                label = { Text("Lucrare") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(10.dp),
            )
            ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                skills.forEach { s ->
                    DropdownMenuItem(text = { Text("${s.name} (${s.unit})") }, onClick = { onSelect(s.id); menu = false })
                }
            }
        }
        OutlinedTextField(
            value = row.quantity,
            onValueChange = onQuantityChange,
            label = { Text(row.unit.ifBlank { "cant" }) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(10.dp),
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Sterge", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun OfferSummaryCard(state: AddProjectUiState) {
    val days = durationDays(state.startDateMillis, state.endDateMillis)
    val selected = state.availableEmployees.filter { it.idUser in state.selectedEmployeeIds }
    val cheltuieli = selected.sumOf { (it.salary ?: 0.0) / 30.0 } * days

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Sumar oferta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SummaryRow("Durata", if (days > 0) "$days zile" else "—")
            SummaryRow("Angajati selectati", selected.size.toString())
            SummaryRow("Cheltuieli salarii (estimat)", if (days > 0) "${cheltuieli.roundToInt()} RON" else "—")

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Text("Recomandare personal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            val active = state.requiredLucrari.filter { it.lucrareId != null && (it.quantity.toFloatOrNull() ?: 0f) > 0f }
            if (active.isEmpty()) {
                Text(
                    "Adauga lucrari cerute si alege termenele ca sa estimam personalul.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                var total = 0
                active.forEach { req ->
                    val qty = req.quantity.toFloatOrNull() ?: 0f
                    val needed = if (req.avgMpPerDay > 0.0 && days > 0)
                        ceil(qty / (req.avgMpPerDay * days)).toInt().coerceAtLeast(1)
                    else null
                    total += needed ?: 0
                    SummaryRow(
                        label = "${req.lucrareName} (${qty.roundToInt()} ${req.unit})",
                        value = if (needed != null) "$needed pers • ${req.avgMpPerDay.roundToInt()} ${req.unit}/zi" else "fara istoric",
                    )
                }
                SummaryRow("Total recomandat", "$total angajati", emphasize = true)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
    }
}
