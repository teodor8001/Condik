package com.example.workipi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.User
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.ConfirmDialog
import com.example.workipi.viewmodel.AddProjectUiState
import com.example.workipi.viewmodel.AddProjectViewModel
import com.example.workipi.viewmodel.OfferLucrareDraft
import com.example.workipi.viewmodel.OfferZoneDraft
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class OfferStep(
    val title: String,
    val subtitle: String,
)

private val offerSteps = listOf(
    OfferStep("Detalii", "Proiect și echipă"),
    OfferStep("Zone", "Organizarea spațiului"),
    OfferStep("Lucrări", "Suprafețe și alocare"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectScreen(
    navController: NavController,
    isOffer: Boolean = false,
    viewModel: AddProjectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(isOffer) { viewModel.setOffer(isOffer) }

    BackHandler {
        if (currentStep > 0) currentStep-- else showCancelConfirm = true
    }

    LaunchedEffect(state.createdProject) {
        val created = state.createdProject ?: return@LaunchedEffect
        viewModel.consumeCreatedProject()
        navController.navigate(
            if (created.isOffer) Screen.Ofertare.route else Screen.ProjectDetail.createRoute(created.projectId)
        ) {
            popUpTo(Screen.AddProject.route) { inclusive = true }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (state.isOffer) "Ofertă nouă" else "Proiect nou",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Pasul ${currentStep + 1} din ${offerSteps.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else showCancelConfirm = true
                    }) {
                        Icon(
                            imageVector = if (currentStep > 0) Icons.Filled.ArrowBack else Icons.Filled.Close,
                            contentDescription = if (currentStep > 0) "Pasul anterior" else "Renunță",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showCancelConfirm = true }) {
                        Text("Renunță")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            OfferNavigationBar(
                step = currentStep,
                errorMessage = state.errorMessage,
                isLoading = state.isLoading,
                isOffer = state.isOffer,
                onBack = { currentStep-- },
                onNext = {
                    if (viewModel.validateStep(currentStep)) currentStep++
                },
                onSubmit = viewModel::submit,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 980.dp),
            ) {
                OfferStepper(
                    currentStep = currentStep,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

                AnimatedContent(
                    targetState = currentStep,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val forward = targetState > initialState
                        val enter = slideInHorizontally(
                            animationSpec = tween(320),
                            initialOffsetX = { width -> if (forward) width else -width },
                        ) + fadeIn(tween(220))
                        val exit = slideOutHorizontally(
                            animationSpec = tween(260),
                            targetOffsetX = { width -> if (forward) -width / 3 else width / 3 },
                        ) + fadeOut(tween(180))
                        enter togetherWith exit
                    },
                    label = "offer-step",
                ) { step ->
                    when (step) {
                        0 -> OfferDetailsPage(
                            state = state,
                            viewModel = viewModel,
                            onStartDateClick = { showStartDatePicker = true },
                            onEndDateClick = { showEndDatePicker = true },
                        )
                        1 -> OfferZonesPage(state = state, viewModel = viewModel)
                        else -> OfferWorksPage(state = state, viewModel = viewModel)
                    }
                }
            }
        }
    }

    if (showCancelConfirm) {
        ConfirmDialog(
            title = if (state.isOffer) "Renunți la ofertă?" else "Renunți la proiect?",
            message = "Datele introduse până acum se vor pierde.",
            onConfirm = { showCancelConfirm = false; navController.popBackStack() },
            onDismiss = { showCancelConfirm = false },
            confirmLabel = "Da, renunț",
            dismissLabel = "Continuă editarea",
        )
    }

    if (showStartDatePicker) {
        DatePickerSheet(
            initialMillis = state.startDateMillis,
            onDismiss = { showStartDatePicker = false },
            onConfirm = {
                viewModel.onStartDateChange(it)
                showStartDatePicker = false
            },
        )
    }
    if (showEndDatePicker) {
        DatePickerSheet(
            initialMillis = state.endDateMillis,
            onDismiss = { showEndDatePicker = false },
            onConfirm = {
                viewModel.onEndDateChange(it)
                showEndDatePicker = false
            },
        )
    }
}

@Composable
private fun OfferStepper(currentStep: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        offerSteps.forEachIndexed { index, step ->
            val active = index <= currentStep
            val circleColor by animateColorAsState(
                targetValue = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "step-color",
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(circleColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index < currentStep) {
                        Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            text = (index + 1).toString(),
                            fontWeight = FontWeight.Bold,
                            color = if (active) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (index == currentStep) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = step.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (index < offerSteps.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .width(34.dp)
                        .height(2.dp)
                        .background(
                            if (index < currentStep) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun OfferDetailsPage(
    state: AddProjectUiState,
    viewModel: AddProjectViewModel,
    onStartDateClick: () -> Unit,
    onEndDateClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    PageColumn {
        PageIntro(
            eyebrow = "PASUL 1",
            title = "Punem oferta pe fundații bune",
            description = "Completează datele principale și alege oamenii pe care îi iei în calcul.",
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                SectionTitle(Icons.Filled.LocationOn, "Datele proiectului", "Informațiile care identifică oferta")
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Numele proiectului") },
                    placeholder = { Text("ex. Timpuri Noi Square") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = state.address,
                    onValueChange = viewModel::onAddressChange,
                    label = { Text("Adresă / locație") },
                    placeholder = { Text("Stradă, număr, localitate") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = state.budget,
                    onValueChange = viewModel::onBudgetChange,
                    label = { Text("Valoarea estimată a ofertei") },
                    suffix = { Text("RON") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp),
                )

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth >= 560.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DateField("Data de start", state.startDateMillis, onStartDateClick, Modifier.weight(1f))
                            DateField("Termen finalizare", state.endDateMillis, onEndDateClick, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DateField("Data de start", state.startDateMillis, onStartDateClick)
                            DateField("Termen finalizare", state.endDateMillis, onEndDateClick)
                        }
                    }
                }
            }
        }

        EmployeesCard(
            employees = state.availableEmployees,
            selectedIds = state.selectedEmployeeIds,
            onToggle = viewModel::toggleEmployee,
        )
    }
}

@Composable
private fun EmployeesCard(
    employees: List<User>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionTitle(Icons.Filled.Person, "Echipa propusă", "Doar angajați și manageri")
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = "${selectedIds.size} selectați",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (employees.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Person,
                    title = "Nu există persoane eligibile",
                    description = "Adaugă angajați sau manageri în firmă pentru a-i include în ofertă.",
                )
            } else {
                employees.forEach { user ->
                    EmployeeSelectionCard(
                        user = user,
                        selected = user.idUser in selectedIds,
                        onClick = { onToggle(user.idUser) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmployeeSelectionCard(user: User, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        label = "employee-container",
    )
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "employee-border",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(container)
            .border(1.dp, border, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials(user.fullName),
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.fullName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = user.role?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Angajat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.KeyboardArrowRight,
            contentDescription = if (selected) "Selectat" else "Selectează",
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun OfferZonesPage(state: AddProjectUiState, viewModel: AddProjectViewModel) {
    var zoneName by rememberSaveable { mutableStateOf("") }

    PageColumn {
        PageIntro(
            eyebrow = "PASUL 2",
            title = "Cum este împărțit proiectul?",
            description = "Zonele grupează lucrările. Poți reveni și adăuga oricâte ai nevoie.",
        )

        NoZonesCard(
            checked = state.withoutZones,
            onCheckedChange = viewModel::onWithoutZonesChange,
        )

        AnimatedVisibility(
            visible = !state.withoutZones,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(150)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        SectionTitle(Icons.Filled.LocationOn, "Adaugă o zonă", "Numele trebuie să fie ușor de recunoscut")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = zoneName,
                                onValueChange = { zoneName = it },
                                label = { Text("Numele zonei") },
                                placeholder = { Text("ex. Parter, Corp A, Etaj 2") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                            )
                            FilledTonalButton(
                                onClick = {
                                    if (viewModel.addZone(zoneName)) zoneName = ""
                                },
                                modifier = Modifier.height(56.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Filled.Add, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Adaugă")
                            }
                        }
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SectionTitle(
                            Icons.Filled.LocationOn,
                            "Zone preluate",
                            if (state.zones.isEmpty()) "Lista este momentan goală" else "${state.zones.size} zone în ofertă",
                        )
                        if (state.zones.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.LocationOn,
                                title = "Adaugă prima zonă",
                                description = "După apăsarea butonului, zona apare imediat aici.",
                            )
                        } else {
                            state.zones.forEachIndexed { index, zone ->
                                ZoneDraftCard(index, zone, onRemove = { viewModel.removeZone(zone.key) })
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.withoutZones) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Am înțeles", fontWeight = FontWeight.Bold)
                        Text(
                            "Toate lucrările vor aparține întregului proiect. În baza de date folosim o singură zonă implicită.",
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
private fun NoZonesCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val container by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.surface,
        label = "no-zones-container",
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = CardDefaults.outlinedCardBorder().let {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            )
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Fără zone", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tratează oferta ca o singură zonă pentru întregul proiect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ZoneDraftCard(index: Int, zone: OfferZoneDraft, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(zone.name, fontWeight = FontWeight.SemiBold)
            Text("Pregătită pentru lucrări", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.DeleteOutline, "Șterge zona", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun OfferWorksPage(state: AddProjectUiState, viewModel: AddProjectViewModel) {
    PageColumn {
        PageIntro(
            eyebrow = "PASUL 3",
            title = "Adaugă lucrările cerute",
            description = "Alege tipul lucrării, suprafața și zona. Totalurile zonelor se calculează automat.",
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (state.withoutZones) "Întregul proiect" else "${state.zones.size} zone disponibile",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (state.withoutZones) "Nu trebuie să alegi zona pentru fiecare lucrare."
                        else "Fiecare lucrare trebuie asociată unei zone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.requiredLucrari.isEmpty()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                EmptyState(
                    icon = Icons.Filled.Construction,
                    title = "Nicio lucrare adăugată",
                    description = "Adaugă prima lucrare pentru a completa oferta.",
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            state.requiredLucrari.forEachIndexed { index, work ->
                WorkDraftCard(
                    index = index,
                    work = work,
                    skills = state.availableSkills,
                    zones = state.zones,
                    withoutZones = state.withoutZones,
                    onSkillSelect = { viewModel.onRequiredLucrareSelect(work.key, it) },
                    onQuantityChange = { viewModel.onRequiredQuantityChange(work.key, it) },
                    onZoneSelect = { viewModel.onRequiredZoneSelect(work.key, it) },
                    onRemove = { viewModel.removeRequiredLucrare(work.key) },
                )
            }
        }

        OutlinedButton(
            onClick = viewModel::addRequiredLucrare,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(7.dp))
            Text("Adaugă o lucrare", fontWeight = FontWeight.SemiBold)
        }

        OfferMiniSummary(state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkDraftCard(
    index: Int,
    work: OfferLucrareDraft,
    skills: List<Lucrare>,
    zones: List<OfferZoneDraft>,
    withoutZones: Boolean,
    onSkillSelect: (Long) -> Unit,
    onQuantityChange: (String) -> Unit,
    onZoneSelect: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    var skillMenu by remember { mutableStateOf(false) }
    var zoneMenu by remember { mutableStateOf(false) }
    val selectedZone = zones.firstOrNull { it.key == work.zoneKey }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = work.lucrareName.ifBlank { "Lucrare nouă" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.DeleteOutline, "Șterge lucrarea", tint = MaterialTheme.colorScheme.error)
                }
            }

            ExposedDropdownMenuBox(
                expanded = skillMenu,
                onExpandedChange = { skillMenu = it },
            ) {
                OutlinedTextField(
                    value = work.lucrareName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipul lucrării") },
                    placeholder = { Text("Alege din catalog") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(skillMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(14.dp),
                )
                ExposedDropdownMenu(expanded = skillMenu, onDismissRequest = { skillMenu = false }) {
                    skills.forEach { skill ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(skill.name, fontWeight = FontWeight.SemiBold)
                                    Text(skill.unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = { onSkillSelect(skill.id); skillMenu = false },
                        )
                    }
                }
            }

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 540.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuantityField(work, onQuantityChange, Modifier.weight(1f))
                        ZoneField(
                            withoutZones = withoutZones,
                            selectedZone = selectedZone,
                            zones = zones,
                            expanded = zoneMenu,
                            onExpandedChange = { zoneMenu = it },
                            onSelect = { onZoneSelect(it); zoneMenu = false },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuantityField(work, onQuantityChange)
                        ZoneField(
                            withoutZones = withoutZones,
                            selectedZone = selectedZone,
                            zones = zones,
                            expanded = zoneMenu,
                            onExpandedChange = { zoneMenu = it },
                            onSelect = { onZoneSelect(it); zoneMenu = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityField(
    work: OfferLucrareDraft,
    onQuantityChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = work.quantity,
        onValueChange = onQuantityChange,
        label = { Text("Suprafață") },
        suffix = { Text(work.unit.ifBlank { "mp" }) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneField(
    withoutZones: Boolean,
    selectedZone: OfferZoneDraft?,
    zones: List<OfferZoneDraft>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded && !withoutZones,
        onExpandedChange = { if (!withoutZones) onExpandedChange(it) },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = if (withoutZones) "Întregul proiect" else selectedZone?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = !withoutZones,
            label = { Text("Zona") },
            placeholder = { Text("Alege zona") },
            trailingIcon = {
                if (!withoutZones) ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                else Icon(Icons.Filled.CheckCircle, null)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(14.dp),
        )
        ExposedDropdownMenu(expanded = expanded && !withoutZones, onDismissRequest = { onExpandedChange(false) }) {
            zones.forEach { zone ->
                DropdownMenuItem(text = { Text(zone.name) }, onClick = { onSelect(zone.key) })
            }
        }
    }
}

@Composable
private fun OfferMiniSummary(state: AddProjectUiState) {
    val totalSurface = state.requiredLucrari.sumOf { (it.quantity.toFloatOrNull() ?: 0f).toDouble() }
    val formatter = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("ro-RO")) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SummaryMetric("Lucrări", state.requiredLucrari.size.toString(), Modifier.weight(1f))
            SummaryMetric("Zone", if (state.withoutZones) "Fără zone" else state.zones.size.toString(), Modifier.weight(1f))
            SummaryMetric("Suprafață", "${formatter.format(totalSurface)} mp", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun OfferNavigationBar(
    step: Int,
    errorMessage: String?,
    isLoading: Boolean,
    isOffer: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.widthIn(max = 932.dp).fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Row(
                modifier = Modifier.widthIn(max = 932.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        enabled = !isLoading,
                    ) {
                        Icon(Icons.Filled.ArrowBack, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Înapoi")
                    }
                }
                Button(
                    onClick = if (step < offerSteps.lastIndex) onNext else onSubmit,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            if (step < offerSteps.lastIndex) "Continuă" else if (isOffer) "Salvează oferta" else "Salvează proiectul",
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            if (step < offerSteps.lastIndex) Icons.Filled.KeyboardArrowRight else Icons.Filled.Check,
                            null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun PageIntro(eyebrow: String, title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.1.sp,
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f), modifier = Modifier.size(32.dp))
        Text(title, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DateField(
    label: String,
    millis: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember {
        SimpleDateFormat("dd MMM yyyy", Locale("ro", "RO")).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    millis?.let { formatter.format(Date(it)) } ?: "Alege data",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (millis != null) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            Icon(Icons.Filled.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(initialMillis: Long?, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = { pickerState.selectedDateMillis?.let(onConfirm) },
            ) { Text("Alege") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    ) {
        DatePicker(state = pickerState)
    }
}

private fun initials(name: String): String = name
    .trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.take(1).uppercase() }
    .ifBlank { "?" }
