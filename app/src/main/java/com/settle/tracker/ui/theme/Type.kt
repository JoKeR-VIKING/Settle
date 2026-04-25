package com.settle.tracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import com.settle.tracker.R

// Body-friendly, readable sans (already shipped)
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular,     FontWeight.Normal),
    Font(R.font.inter_semi_bold,   FontWeight.SemiBold),
    Font(R.font.inter_bold,        FontWeight.Bold),
    Font(R.font.inter_extra_bold,  FontWeight.ExtraBold),
)

// Orbitron = display face, reserved for HERO / brand / amounts
val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron_regular,     FontWeight.Normal),
    Font(R.font.orbitron_medium,      FontWeight.Medium),
    Font(R.font.orbitron_semi_bold,   FontWeight.SemiBold),
    Font(R.font.orbitron_bold,        FontWeight.Bold),
    Font(R.font.orbitron_extra_bold,  FontWeight.ExtraBold),
)

val Typography = Typography(
    // Display / Hero – Orbitron for brand moments
    displayLarge = TextStyle(
        fontFamily = OrbitronFontFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = OrbitronFontFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = 1.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = 0.5.sp
    ),

    // Titles / Labels – Inter for breathing UI
    titleLarge = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp
    ),

    labelLarge = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp
    ),
)
