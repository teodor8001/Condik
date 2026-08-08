package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.SkillLevel
import com.example.workipi.data.model.UserRole
import com.example.workipi.viewmodel.InvitationCodeViewModel

private enum class InviteRole(val label: String, val role: UserRole) {
    ANGAJAT("Angajat", UserRole.ANGAJAT),
    SEF_ECHIPA("Sef de echipa", UserRole.SEF_ECHIPA),
    INGINER("Inginer", UserRole.INGINER),
    MANAGER("Manager", UserRole.MANAGER),
    CLIENT("Client", UserRole.CLIENT),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeScreen(
    navController: NavController,
    viewModel: InvitationCodeViewModel = hiltViewModel(),
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.uiState.collectAsState()

    // Dupa ce contul a fost creat, editarea s-a terminat -> nu mai confirmam la iesire.
    LaunchedEffect(state.createdEmployeeName) {
        com.example.workipi.ui.components.NavEditGuard.skipConfirm = state.createdEmployeeName != null
    }
    DisposableEffect(Unit) {
        onDispose { com.example.workipi.ui.components.NavEditGuard.skipConfirm = false }
    }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf(InviteRole.ANGAJAT) }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf("") }

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
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Inapoi",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Adauga angajat nou",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (state.createdEmployeeName == null) "Creeaza contul angajatului"
                       else "Cont creat cu succes",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            val createdName = state.createdEmployeeName
            if (createdName == null) {
                FormCard(
                    fullName = fullName,
                    onFullNameChange = { fullName = it; localError = "" },
                    email = email,
                    onEmailChange = { email = it; localError = "" },
                    phone = phone,
                    onPhoneChange = { phone = it.filter { c -> c.isDigit() }; localError = "" },
                    salary = salary,
                    onSalaryChange = { salary = it.filter { c -> c.isDigit() || c == '.' }; localError = "" },
                    password = password,
                    onPasswordChange = { password = it; localError = "" },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it; localError = "" },
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    role = role,
                    onRoleChange = { role = it },
                    roleMenuExpanded = roleMenuExpanded,
                    onRoleMenuExpandedChange = { roleMenuExpanded = it },
                    errorMessage = localError.ifEmpty { state.errorMessage.orEmpty() },
                    isLoading = state.isLoading,
                    focusManager = focusManager,
                    onSubmit = {
                        val parsedSalary = salary.takeIf { it.isNotBlank() }?.toFloatOrNull()
                        val error = when {
                            fullName.isBlank() -> "Introdu numele si prenumele."
                            email.isBlank() -> "Introdu email-ul."
                            com.example.workipi.util.Validation.emailError(email) != null ->
                                com.example.workipi.util.Validation.emailError(email)
                            phone.isBlank() -> "Introdu numarul de telefon."
                            com.example.workipi.util.Validation.phoneError(phone) != null ->
                                com.example.workipi.util.Validation.phoneError(phone)
                            salary.isNotBlank() && parsedSalary == null -> "Salariul trebuie sa fie un numar valid."
                            password.length < 6 -> "Parola trebuie sa aiba cel putin 6 caractere."
                            password != confirmPassword -> "Parolele nu coincid."
                            else -> null
                        }
                        if (error != null) {
                            localError = error
                            return@FormCard
                        }
                        viewModel.createEmployeeAccount(
                            fullName = fullName,
                            email = email,
                            phoneNumber = phone,
                            role = role.role,
                            salary = parsedSalary,
                            password = password,
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SkillsCard(
                    available = state.availableSkills,
                    selected = state.selectedSkills,
                    onToggle = viewModel::toggleSkill,
                    onSetLevel = viewModel::setSkillLevel,
                )
            } else {
                AccountCreatedCard(
                    employeeName = createdName,
                    onAddAnother = {
                        fullName = ""
                        email = ""
                        phone = ""
                        salary = ""
                        password = ""
                        confirmPassword = ""
                        role = InviteRole.ANGAJAT
                        localError = ""
                        viewModel.reset()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormCard(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    salary: String,
    onSalaryChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    role: InviteRole,
    onRoleChange: (InviteRole) -> Unit,
    roleMenuExpanded: Boolean,
    onRoleMenuExpandedChange: (Boolean) -> Unit,
    errorMessage: String,
    isLoading: Boolean,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onSubmit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Detalii angajat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = { Text("Nume si prenume") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Numar de telefon") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = salary,
                onValueChange = onSalaryChange,
                label = { Text("Salariu lunar (RON)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Parola initiala") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ascunde parola" else "Arata parola",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirma parola") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(10.dp),
            )

            Text(
                text = "Angajatul se va loga cu acest email si parola, apoi va fi rugat sa-si schimbe parola. Pana atunci ramane \"in asteptare\", dar poate fi adaugat in proiecte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ExposedDropdownMenuBox(
                expanded = roleMenuExpanded,
                onExpandedChange = onRoleMenuExpandedChange
            ) {
                OutlinedTextField(
                    value = role.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rol") },
                    trailingIcon = {
                        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(10.dp),
                )
                ExposedDropdownMenu(
                    expanded = roleMenuExpanded,
                    onDismissRequest = { onRoleMenuExpandedChange(false) }
                ) {
                    InviteRole.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onRoleChange(option)
                                onRoleMenuExpandedChange(false)
                            }
                        )
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Creeaza cont angajat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCreatedCard(
    employeeName: String,
    onAddAnother: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )

            Text(
                text = "Cont creat pentru $employeeName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Da-i angajatului email-ul si parola initiala. La prima logare i se va cere sa-si schimbe parola. Pana atunci apare ca \"in asteptare\", dar poate fi adaugat in proiecte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = onAddAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = "Adauga alt angajat",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillsCard(
    available: List<Lucrare>,
    selected: Map<Long, SkillLevel>,
    onToggle: (Long) -> Unit,
    onSetLevel: (Long, SkillLevel) -> Unit,
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
                text = "Competente (${selected.size} selectate)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (available.isEmpty()) {
                Text(
                    text = "Nu exista lucrari definite. Adauga-le din Supabase intai.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            available.forEach { lucrare ->
                val isSelected = lucrare.id in selected
                val level = selected[lucrare.id] ?: SkillLevel.JUNIOR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else
                                Color.Transparent
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle(lucrare.id) },
                    )
                    Text(
                        text = lucrare.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (isSelected) {
                        SkillLevelDropdown(
                            current = level,
                            onSelect = { onSetLevel(lucrare.id, it) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillLevelDropdown(
    current: SkillLevel,
    onSelect: (SkillLevel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = current.label,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .width(120.dp),
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            },
            textStyle = MaterialTheme.typography.bodySmall,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SkillLevel.entries.forEach { lvl ->
                DropdownMenuItem(
                    text = { Text(lvl.label) },
                    onClick = {
                        onSelect(lvl)
                        expanded = false
                    },
                )
            }
        }
    }
}
