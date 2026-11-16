package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val InterBody = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium)
)

fun limonTypography(fontScale: Float = 1.0f): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = (30f * fontScale).sp,
        lineHeight = (36f * fontScale).sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = (21.5f * fontScale).sp,
        lineHeight = (27f * fontScale).sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = (18f * fontScale).sp,
        lineHeight = (23f * fontScale).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14.5f * fontScale).sp,
        lineHeight = (20f * fontScale).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterBody,
        fontWeight = FontWeight.Normal,
        fontSize = (14f * fontScale).sp,
        lineHeight = (20f * fontScale).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterBody,
        fontWeight = FontWeight.Normal,
        fontSize = (13f * fontScale).sp,
        lineHeight = (18f * fontScale).sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterBody,
        fontWeight = FontWeight.Normal,
        fontSize = (11f * fontScale).sp,
        lineHeight = (15f * fontScale).sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = (13f * fontScale).sp,
        lineHeight = (17f * fontScale).sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = (10f * fontScale).sp,
        lineHeight = (13f * fontScale).sp
    )
)
