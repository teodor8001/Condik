package com.example.workipi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workipi.viewmodel.HistoryViewModel
import com.example.workipi.viewmodel.PontareUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PontareEntryScreen(
    navController: NavController,
    projectId: Long,
    userId: Long,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) { viewModel.load(projectId) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pontare") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Inapoi")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            PontareFormBody(
                                state = state,
                                onSelectZone = viewModel::selectZone,
                                onSelectSkill = viewModel::selectSkill,
                                onQuantityChange = viewModel::onQuantityChange,
                                onHoursChange = viewModel::onHoursChange,
                                onPickDate = { showDatePicker = true },
                                onSubmit = { viewModel.submit(userId) },
                                focusManager = focusManager,
                            )
                        }
                    }
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
}

/**
 * Corpul formularului de pontare (zona, lucrare, cantitate, ore, data, punctaj, buton salveaza).
 * Refolosit de ecranul de pontare si de popup-ul din Detalii proiect — toata logica sta in HistoryViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PontareFormBody(
    state: PontareUiState,
    onSelectZone: (Long) -> Unit,
    onSelectSkill: (Long) -> Unit,
    onQuantityChange: (String) -> Unit,
    onHoursChange: (String) -> Unit,
    onPickDate: () -> Unit,
    onSubmit: () -> Unit,
    focusManager: FocusManager,
    submitEnabled: Boolean = true,
    compact: Boolean = false,
) {
    var skillMenuExpanded by remember { mutableStateOf(false) }
    var zoneMenuExpanded by remember { mutableStateOf(false) }
    val fieldText =
        if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge

    val selectedSkill = state.skills.firstOrNull { it.id == state.selectedSkillId }
    val earnedPointsText = remember(state.selectedSkillId, state.quantity) {
        val skill = state.skills.firstOrNull { it.id == state.selectedSkillId }
        val qty = state.quantity.toFloatOrNull() ?: 0f
        if (skill != null && qty > 0f) "${(skill.points * qty.toInt())} pts" else "—"
    }

    val selectedZone = state.zones.firstOrNull { it.id == state.selectedZoneId }
    ExposedDropdownMenuBox(
        expanded = zoneMenuExpanded,
        onExpandedChange = { zoneMenuExpanded = it },
    ) {
        OutlinedTextField(
            value = selectedZone?.name ?: "Alege zona",
            onValueChange = {},
            readOnly = true,
            textStyle = fieldText,
            label = { Text("Zona") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(10.dp),
        )
        ExposedDropdownMenu(
            expanded = zoneMenuExpanded,
            onDismissRequest = { zoneMenuExpanded = false },
        ) {
            state.zones.forEach { zone ->
                DropdownMenuItem(
                    text = { Text("${zone.name ?: "Zona"} • ${zone.surface.toInt()} mp") },
                    onClick = {
                        onSelectZone(zone.id)
                        zoneMenuExpanded = false
                    },
                )
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = skillMenuExpanded,
        onExpandedChange = { skillMenuExpanded = it },
    ) {
        OutlinedTextField(
            value = selectedSkill?.let { "${it.name} (${it.unit})" } ?: "Alege lucrare",
            onValueChange = {},
            readOnly = true,
            textStyle = fieldText,
            label = { Text("Tip lucrare") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(10.dp),
        )
        ExposedDropdownMenu(
            expanded = skillMenuExpanded,
            onDismissRequest = { skillMenuExpanded = false },
        ) {
            if (state.skills.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nicio lucrare definita") },
                    onClick = {},
                    enabled = false,
                )
            }
            state.skills.forEach { lucrare ->
                DropdownMenuItem(
                    text = { Text("${lucrare.name} • ${lucrare.points} pts/${lucrare.unit}") },
                    onClick = {
                        onSelectSkill(lucrare.id)
                        skillMenuExpanded = false
                    },
                )
            }
        }
    }

    OutlinedTextField(
        value = state.quantity,
        onValueChange = onQuantityChange,
        textStyle = fieldText,
        label = { Text("Cantitate" + (selectedSkill?.let { " (${it.unit})" } ?: "")) },
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
        onValueChange = onHoursChange,
        textStyle = fieldText,
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
        onClick = onPickDate,
        compact = compact,
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
        onClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 44.dp else 50.dp),
        shape = RoundedCornerShape(10.dp),
        enabled = submitEnabled && !state.isSaving,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (state.isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text("Salveaza pontare", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PontareDatePicker(
    initialMillis: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { onConfirm(it) }
                    onDismiss()
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

@Composable
private fun DateField(label: String, millis: Long, onClick: () -> Unit, compact: Boolean = false) {
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
                .padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 14.dp),
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
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
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
