package com.example.reminderstudent2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppLightColorScheme = lightColorScheme(
    primary = PaletteStrong,
    onPrimary = Color.White,
    primaryContainer = PaletteSky,
    onPrimaryContainer = PaletteNavy,
    secondary = PaletteMedium,
    onSecondary = PaletteNavy,
    secondaryContainer = PaletteLightBlue,
    onSecondaryContainer = PaletteNavy,
    tertiary = PaletteNavy,
    onTertiary = Color.White,
    background = PaletteIce,
    onBackground = PaletteNavy,
    surface = CardElevated,
    onSurface = PaletteNavy,
    surfaceVariant = PaletteLightBlue,
    onSurfaceVariant = TextSecondary,
    outline = PaletteSky,
    outlineVariant = PaletteLightBlue
)

@Composable
fun ReminderStudent2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppLightColorScheme,
        typography = Typography,
        content = content
    )
}
