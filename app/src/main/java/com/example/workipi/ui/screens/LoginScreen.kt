package com.example.workipi.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.example.workipi.ui.screens.login.LoginViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.signedInUser) {
        val utilizator = state.signedInUser ?: return@LaunchedEffect
        MockSession.currentUser = utilizator.toUser()
        viewModel.consumeSignedInUser()
        // Angajatii creati de admin trebuie sa-si schimbe parola initiala inainte de a intra in aplicatie.
        val destination = if (utilizator.needsPasswordChange) Screen.ChangePassword.route else Screen.Home.route
        navController.navigate(destination) {
            popUpTo(Screen.Login.route) { inclusive = true }
        }
    }

    // Animatie de intrare — trigger o data
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val headerAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "header-alpha",
    )
    val headerOffsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else (-24).dp,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "header-offset",
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "card-alpha",
    )
    val cardOffsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 32.dp,
        animationSpec = tween(600, delayMillis = 150, easing = FastOutSlowInEasing),
        label = "card-offset",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decor de fundal — gradient + cercuri plutitoare + val
        WaveDecor(modifier = Modifier.fillMaxSize())

        // Continut centrat cu lățime maxima pe tableta
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .alpha(headerAlpha)
                    .offset(y = headerOffsetY),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Icon brand cu fundal circular
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Engineering,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Condik",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Management santier",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Card(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .alpha(cardAlpha)
                    .offset(y = cardOffsetY),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Text(
                        text = "Autentificare",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("Parola") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (state.passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.submit()
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = viewModel::togglePasswordVisibility) {
                                Icon(
                                    imageVector = if (state.passwordVisible)
                                        Icons.Filled.Visibility
                                    else
                                        Icons.Filled.VisibilityOff,
                                    contentDescription = if (state.passwordVisible)
                                        "Ascunde parola"
                                    else
                                        "Arata parola",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
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
                        onClick = { viewModel.submit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Intra in cont",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    TextButton(
                        onClick = { navController.navigate(Screen.CreateCompany.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Creeaza firma noua",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = { navController.navigate(Screen.ActivateAccount.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Am un cod de invitatie",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveDecor(modifier: Modifier = Modifier) {
    val brand = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val waveBottomY = h * 0.42f

        // Fundal cu val portocaliu — gradient subtil
        val wavePath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, waveBottomY - 30.dp.toPx())
            cubicTo(
                w * 0.7f, waveBottomY + 50.dp.toPx(),
                w * 0.3f, waveBottomY - 90.dp.toPx(),
                0f, waveBottomY,
            )
            close()
        }
        drawPath(
            path = wavePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    brand.copy(alpha = 0.18f),
                    brand.copy(alpha = 0.06f),
                ),
                startY = 0f,
                endY = waveBottomY,
            ),
        )

        // Cercuri plutitoare in zona val — subtil, alpha mic
        drawCircle(
            color = brand.copy(alpha = 0.10f),
            radius = 90.dp.toPx(),
            center = Offset(w * 0.18f, h * 0.12f),
        )
        drawCircle(
            color = brand.copy(alpha = 0.07f),
            radius = 60.dp.toPx(),
            center = Offset(w * 0.85f, h * 0.22f),
        )
        drawCircle(
            color = brand.copy(alpha = 0.05f),
            radius = 110.dp.toPx(),
            center = Offset(w * 0.55f, h * 0.05f),
        )

        // Cerc decorativ jos pentru echilibru
        drawCircle(
            color = brand.copy(alpha = 0.04f),
            radius = 130.dp.toPx(),
            center = Offset(w * 0.1f, h * 0.95f),
        )
    }
}
