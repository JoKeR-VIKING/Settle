package com.settle.tracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = DarkBackground,
    primaryContainer = TealPrimary,
    onPrimaryContainer = LightSurface,
    secondary = AccentAmberDark,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF5B3B29),
    onSecondaryContainer = Color(0xFFFFE0D3),
    tertiary = Color(0xFF9EC8FF),
    onTertiary = DarkBackground,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = Color(0xFFF1F5F1),
    onBackground = Color(0xFFF1F5F1),
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    surfaceBright = DarkSurfaceVariant,
    surfaceContainerHighest = Color(0xFF29352F),
    error = Danger,
    onError = Color.White,
    primaryFixed = TealPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealPrimaryLight,
    onPrimaryContainer = Color(0xFF00201C),
    secondary = AccentAmber,
    onSecondary = Color.White,
    secondaryContainer = AccentAmberContainer,
    onSecondaryContainer = Color(0xFF3D210F),
    tertiary = Color(0xFF4767B0),
    onTertiary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurface = Color(0xFF1A1F1C),
    onBackground = Color(0xFF131815),
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    surfaceBright = Color(0xFFF8F1E7),
    surfaceContainerHighest = Color(0xFFEDF2ED),
    error = Danger,
    onError = Color.White,
    primaryFixed = TealPrimaryLight
)

@Composable
fun SettleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
