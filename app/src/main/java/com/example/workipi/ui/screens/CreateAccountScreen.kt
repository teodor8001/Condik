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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.toUser
import com.example.workipi.navigation.Screen
import com.example.workipi.viewmodel.CreateAdminAccountUiState
import com.example.workipi.viewmodel.CreateAccountAdminViewModel
import com.example.workipi.viewmodel.CreateAccountRegularViewModel

private enum class CreateAccountMode(val label: String) {
    NEW_COMPANY("Firma noua"),
    INVITE_CODE("Cu cod invitatie"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    navController: NavController,
    createAccountAdminViewModel: CreateAccountAdminViewModel = hiltViewModel(),
    createAccountRegularViewModel: CreateAccountRegularViewModel =  hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val adminState by createAccountAdminViewModel.uiState.collectAsState()
    val regularState by createAccountRegularViewModel.uiState.collectAsState()

    var mode by remember { mutableStateOf(CreateAccountMode.NEW_COMPANY) }

    // This is for admin created account
    LaunchedEffect(adminState.createdUser) {
        val utilizator = adminState.createdUser ?: return@LaunchedEffect
        MockSession.currentUser = utilizator.toUser()
        createAccountAdminViewModel.consumeCreatedUser()
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Login.route) { inclusive = true }
        }
    }

    LaunchedEffect(regularState.createdUser) {
        val user = regularState.createdUser ?: return@LaunchedEffect
        MockSession.currentUser = user.toUser()
        createAccountRegularViewModel.consumeCreatedUser()
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Login.route) {
                inclusive = true
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
                text = "CONDIK",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Creeaza cont",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CreateAccountMode.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = mode == option,
                        onClick = { mode = option },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = CreateAccountMode.entries.size
                        ),
                        label = { Text(option.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (mode) {
                CreateAccountMode.NEW_COMPANY -> NewCompanyCard(
                    state = adminState,
                    createAccountAdminViewModel = createAccountAdminViewModel,
                    focusManager = focusManager,
                )
                CreateAccountMode.INVITE_CODE -> InviteCodeCard(
                    code = regularState.invitationCode,
                    onCodeChange = createAccountRegularViewModel::onInvitationCodeChange,
                    password = regularState.password,
                    onPasswordChange = createAccountRegularViewModel::onPasswordChange,
                    confirmPassword = regularState.confirmPassword,
                    onConfirmPasswordChange = createAccountRegularViewModel::onConfirmPassword,
                    passwordVisible = regularState.passwordVisible,
                    onTogglePasswordVisibility = createAccountRegularViewModel::togglePasswordVisibility,
                    errorMessage = regularState.errorMessage.orEmpty(),
                    focusManager = focusManager,
                    onSubmit = createAccountRegularViewModel::submitRegularAccount,
                )
            }
        }
    }
}

@Composable
private fun NewCompanyCard(
    state: CreateAdminAccountUiState,
    createAccountAdminViewModel: CreateAccountAdminViewModel,
    focusManager: FocusManager,
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
                text = "Inregistrare administrator",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = state.denumireFirma,
                onValueChange = createAccountAdminViewModel::onDenumireFirmaChange,
                label = { Text("Denumire firma") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = state.numePrenume,
                onValueChange = createAccountAdminViewModel::onNumePrenumeChange,
                label = { Text("Nume si prenume") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = state.email,
                onValueChange = createAccountAdminViewModel::onEmailChange,
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
                value = state.telefon,
                onValueChange = createAccountAdminViewModel::onTelefonChange,
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
                value = state.password,
                onValueChange = createAccountAdminViewModel::onPasswordChange,
                label = { Text("Parola") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                trailingIcon = {
                    IconButton(onClick = createAccountAdminViewModel::togglePasswordVisibility) {
                        Icon(
                            imageVector = if (state.passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (state.passwordVisible) "Ascunde parola" else "Arata parola",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = createAccountAdminViewModel::onConfirmPasswordChange,
                label = { Text("Confirma parola") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    createAccountAdminViewModel.submitAdminAccount()
                }),
                shape = RoundedCornerShape(10.dp),
            )

            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { createAccountAdminViewModel.submitAdminAccount() },
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
                        text = "Creeaza cont",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteCodeCard(
    code: String,
    onCodeChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    errorMessage: String,
    focusManager: FocusManager,
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
                text = "Activare cont cu cod invitatie",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Introdu codul primit de la administrator si seteaza-ti o parola.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                label = { Text("Cod invitatie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = RoundedCornerShape(10.dp),
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Parola") },
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
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onSubmit()
                }),
                shape = RoundedCornerShape(10.dp),
            )

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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Activeaza cont",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}