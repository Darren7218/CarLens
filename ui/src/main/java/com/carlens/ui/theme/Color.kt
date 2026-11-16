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

// Extended semantic & chart palette
val StatusTeal = Color(0xFF00897B)
val StatusAmberContainerLight = Color(0xFFFFF4E5)
val StatusAmberContainerDark = Color(0xFF332610)
val StatusRedContainerLight = Color(0xFFFDECEC)
val StatusRedContainerDark = Color(0xFF3A1A1A)
val StatusGreenContainerLight = Color(0xFFE8F5E9)
val StatusGreenContainerDark = Color(0xFF163022)

// Category chart & icon palette (harmonized across warm light & charcoal dark)
val CategoryFuelColor = Color(0xFF00897B)
val CategoryServiceColor = Color(0xFF1E88E5)
val CategoryTollColor = Color(0xFF00ACC1)
val CategoryParkingColor = Color(0xFF5E35B1)
val CategoryInsuranceColor = Color(0xFFF4511E)
val CategoryRoadTaxColor = Color(0xFFFB8C00)
val CategoryAccessoriesColor = Color(0xFF43A047)
val CategoryRepairsColor = Color(0xFFE53935)
val CategoryOtherColor = Color(0xFF78909C)

val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = Color(0xFF003830),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9ECE9),
    onSecondaryContainer = Color(0xFF0C3833),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF0DB),
    onTertiaryContainer = Color(0xFF4A2800),
    background = WarmBackground,
    onBackground = LightTextPrimary,
    surface = WarmSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF8F5),
    surfaceContainer = Color(0xFFF5F2EC),
    surfaceContainerHigh = Color(0xFFEEEAE2),
    surfaceContainerHighest = Color(0xFFE6E1D8),
    outline = WarmBorder,
    outlineVariant = Color(0xFFE4DFD5),
    error = StatusRed,
    onError = Color.White,
    errorContainer = StatusRedContainerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color(0xFF003830),
    primaryContainer = TealContainerDark,
    onPrimaryContainer = Color(0xFFA7FFEB),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00332C),
    secondaryContainer = Color(0xFF1E3230),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF3E2200),
    tertiaryContainer = Color(0xFF3A2B16),
    onTertiaryContainer = Color(0xFFFFE0B2),
    background = CharcoalBackground,
    onBackground = DarkTextPrimary,
    surface = CharcoalSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = Color(0xFF101214),
    surfaceContainerLow = Color(0xFF171A1D),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF25292E),
    surfaceContainerHighest = Color(0xFF2E3339),
    outline = CharcoalBorder,
    outlineVariant = Color(0xFF2C3036),
    error = StatusRed,
    onError = Color.White,
    errorContainer = StatusRedContainerDark,
    onErrorContainer = Color(0xFFFFDAD6)
)
