package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SnTekkenColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ArenaObsidian,
    primaryContainer = Color(0xFF003B46),
    onPrimaryContainer = NeonCyan,
    secondary = BlazingOrange,
    onSecondary = ArenaObsidian,
    secondaryContainer = Color(0xFF4A2000),
    onSecondaryContainer = BlazingOrange,
    tertiary = NeonCrimson,
    onTertiary = Color.White,
    background = ArenaObsidian,
    onBackground = TextPrimary,
    surface = ArenaSurface,
    onSurface = TextPrimary,
    surfaceVariant = ArenaSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = OverlordCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SnTekkenColorScheme,
        typography = Typography,
        content = content
    )
}
