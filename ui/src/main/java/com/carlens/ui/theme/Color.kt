package com.carlens.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Teal accent
val TealPrimary = Color(0xFF00897B)
val TealPrimaryDark = Color(0xFF4DB6AC)
val TealContainer = Color(0xFFE0F2F1)
val TealContainerDark = Color(0xFF004D40)

// Status colors
val StatusAmber = Color(0xFFFFB300)      // Approaching / Due Soon
val StatusRed = Color(0xFFE53935)        // Strictly Overdue
val StatusGreen = Color(0xFF43A047)      // Up to Date
val StatusMuted = Color(0xFF78909C)      // Mileage unknown / neutral

// Charcoal Dark Theme
val CharcoalBackground = Color(0xFF131417)
val CharcoalSurface = Color(0xFF1E2024)
val CharcoalSurfaceVariant = Color(0xFF292C33)
val CharcoalBorder = Color(0xFF383C44)
val DarkTextPrimary = Color(0xFFF1F3F5)
val DarkTextSecondary = Color(0xFF9EA3AE)

// Warm Light Theme
val WarmBackground = Color(0xFFF9F8F6)
val WarmSurface = Color(0xFFFFFFFF)
val WarmSurfaceVariant = Color(0xFFF0EFEA)
val WarmBorder = Color(0xFFE2E0D8)
val LightTextPrimary = Color(0xFF1C1D20)
val LightTextSecondary = Color(0xFF6B7280)

val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = Color(0xFF003830),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    background = WarmBackground,
    onBackground = LightTextPrimary,
    surface = WarmSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = WarmBorder,
    error = StatusRed,
    onError = Color.White
)

val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color(0xFF003830),
    primaryContainer = TealContainerDark,
    onPrimaryContainer = Color(0xFFA7FFEB),
    secondary = TealPrimaryDark,
    onSecondary = Color(0xFF003830),
    background = CharcoalBackground,
    onBackground = DarkTextPrimary,
    surface = CharcoalSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = CharcoalBorder,
    error = StatusRed,
    onError = Color.White
)
