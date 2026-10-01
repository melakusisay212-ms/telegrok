package com.teleexpense.counter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ethio Telecom inspired palette – green primary, clean surfaces
val EthioGreen = Color(0xFF008C45)
val EthioGreenDark = Color(0xFF006B35)
val EthioYellow = Color(0xFFFCDD09)
val TelebirrGreen = Color(0xFF00A651)
val SoftBackground = Color(0xFFF5F9F6)
val CardSurface = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF5A6B5F)
val Danger = Color(0xFFDA121A)

private val LightColorScheme = lightColorScheme(
    primary = EthioGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F0E0),
    onPrimaryContainer = EthioGreenDark,
    secondary = EthioYellow,
    onSecondary = TextPrimary,
    secondaryContainer = Color(0xFFFFF8D6),
    background = SoftBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE8F2EC),
    onSurfaceVariant = TextSecondary,
    error = Danger,
    outline = Color(0xFFB0C4B8)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF7A),
    onPrimary = Color.Black,
    primaryContainer = EthioGreenDark,
    onPrimaryContainer = Color(0xFFD4F0E0),
    secondary = EthioYellow,
    onSecondary = Color.Black,
    background = Color(0xFF0F1A14),
    onBackground = Color(0xFFE8F2EC),
    surface = Color(0xFF1A2A20),
    onSurface = Color(0xFFE8F2EC),
    surfaceVariant = Color(0xFF24352C),
    onSurfaceVariant = Color(0xFFA8BDB0),
    error = Color(0xFFFF6B6B),
    outline = Color(0xFF3A4F42)
)

@Composable
fun TeleExpenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
