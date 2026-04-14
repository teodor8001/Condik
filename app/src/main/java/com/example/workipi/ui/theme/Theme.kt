package com.example.workipi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WorkIPIColorScheme = lightColorScheme(
    primary          = OrangePrimary,
    onPrimary        = BackgroundWhite,
    primaryContainer = OrangeLight,
    onPrimaryContainer = OrangeDark,

    background       = BackgroundWhite,
    onBackground     = TextPrimary,

    surface          = SurfaceWhite,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,

    error            = ErrorRed,
    outline          = DividerGray
)

@Composable
fun WorkIPITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WorkIPIColorScheme,
        typography  = Typography,
        content     = content
    )
}