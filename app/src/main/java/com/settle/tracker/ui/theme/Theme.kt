package com.settle.tracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary             = BrandTeal,
    onPrimary           = Color.White,
    primaryContainer    = BrandTealDeep,
    onPrimaryContainer  = Color.White,

    secondary           = BrandBlue,
    onSecondary         = Color.White,
    secondaryContainer  = BrandBlueDeep,
    onSecondaryContainer= Color.White,

    tertiary            = AccentLilac,
    onTertiary          = Color(0xFF120B21),
    tertiaryContainer   = Color(0xFF2A1F4A),
    onTertiaryContainer = AccentLilac,

    background          = DarkBackground,
    onBackground        = DarkOnSurface,

    surface             = DarkSurface,
    onSurface           = DarkOnSurface,
    surfaceVariant      = DarkSurfaceVar,
    onSurfaceVariant    = DarkOnSurfaceVar,

    surfaceBright       = Success,        // kept for compatibility (positive amount)
    error               = Danger,
    onError             = Color.White,
    errorContainer      = Color(0xFF3B0F14),
    onErrorContainer    = DangerLight,
    outline             = Color(0xFF324155),
    outlineVariant      = Color(0xFF1E2A3D)
)

private val LightColorScheme = lightColorScheme(
    primary             = BrandTeal,
    onPrimary           = Color.White,
    primaryContainer    = BrandTealSoft,
    onPrimaryContainer  = BrandTealDeep,

    secondary           = BrandBlue,
    onSecondary         = Color.White,
    secondaryContainer  = BrandBlueSoft,
    onSecondaryContainer= BrandBlueDeep,

    tertiary            = AccentLilac,
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFEDE4FF),
    onTertiaryContainer = Color(0xFF3B2A6B),

    background          = LightBackground,
    onBackground        = LightOnSurface,

    surface             = LightSurface,
    onSurface           = LightOnSurface,
    surfaceVariant      = LightSurfaceVar,
    onSurfaceVariant    = LightOnSurfaceVar,

    surfaceBright       = Success,
    error               = Danger,
    onError             = Color.White,
    errorContainer      = Color(0xFFFFE0E0),
    onErrorContainer    = Color(0xFF7A1F1F),
    outline             = Color(0xFFCBD5D7),
    outlineVariant      = Color(0xFFE4EEEF)
)

@Composable
fun SettleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // keep brand-consistent: don't adopt Material You tint
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
