package com.example.reminderstudent2.ui.theme

import androidx.compose.ui.graphics.Color

/** Palette from design reference (light blues → navy). */
val PaletteIce = Color(0xFFEFF3FF)
val PaletteLightBlue = Color(0xFFC6DBEF)
val PaletteSky = Color(0xFF9ECAE1)
val PaletteMedium = Color(0xFF6BAED6)
val PaletteStrong = Color(0xFF3182BD)
val PaletteNavy = Color(0xFF08519C)

val TextSecondary = PaletteNavy.copy(alpha = 0.62f)
val TextMuted = PaletteNavy.copy(alpha = 0.48f)

/** Status accents (same blue family as requested). */
val StatusSoonBg = PaletteSky.copy(alpha = 0.55f)
val StatusSoonFg = PaletteNavy
val StatusScheduledBg = PaletteLightBlue
val StatusScheduledFg = PaletteStrong
val StatusLaterBg = PaletteIce
val StatusLaterFg = PaletteStrong

val CardElevated = Color.White
val NavBarSurface = PaletteLightBlue
val ProgressTrack = PaletteLightBlue
