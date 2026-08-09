package com.example.workipi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WorkIPIColorScheme = lightColorScheme(
    primary          = IndigoPrimary,
    onPrimary        = AppSurface,
    primaryContainer = IndigoLight,
    onPrimaryContainer = IndigoDark,
    secondary        = TealAccent,
    secondaryContainer = TealLight,

    background       = AppBackground,
    onBackground     = InkPrimary,

    surface          = AppSurface,
    surfaceVariant   = SurfaceSubtle,
    onSurface        = InkPrimary,
    onSurfaceVariant = InkSecondary,

    error            = ErrorRed,
    outline          = OutlineSoft
)

@Composable
fun WorkIPITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WorkIPIColorScheme,
        typography  = Typography,
        content     = content
    )
}
