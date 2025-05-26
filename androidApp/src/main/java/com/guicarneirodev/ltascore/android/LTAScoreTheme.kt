package com.guicarneirodev.ltascore.android

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object LTAThemeColors {
    val DarkBackground = Color(0xFF1A1A1F)
    val CardBackground = Color(0xFF2A2A30)
    val PrimaryGold = Color(0xFFB1A280)
    val SecondaryRed = Color(0xFFFF4550)
    val TertiaryGold = Color(0xFFD9B151)

    val TextPrimary = Color(0xFFEEEEEE)
    val TextSecondary = Color(0xFFAAAAAA)

    val Success = Color(0xFF00CD4D)
    val Warning = Color(0xFFFFAA00)
    val LiveRed = Color(0xFFFF4550)
}

@Composable
fun LTAScoreTheme(
    content: @Composable () -> Unit
) {
    val colors = darkColorScheme(
        primary = LTAThemeColors.PrimaryGold,
        onPrimary = LTAThemeColors.TextPrimary,
        secondary = LTAThemeColors.SecondaryRed,
        onSecondary = LTAThemeColors.TextPrimary,
        tertiary = LTAThemeColors.TertiaryGold,
        onTertiary = Color.Black,
        background = LTAThemeColors.DarkBackground,
        onBackground = LTAThemeColors.TextPrimary,
        surface = LTAThemeColors.CardBackground,
        onSurface = LTAThemeColors.TextPrimary,
        surfaceVariant = Color(0xFF252530),
        onSurfaceVariant = LTAThemeColors.TextSecondary,
        error = LTAThemeColors.SecondaryRed,
        onError = LTAThemeColors.TextPrimary,
        primaryContainer = Color(0xFF2A2A30),
        onPrimaryContainer = LTAThemeColors.PrimaryGold,
        secondaryContainer = Color(0xFF252530),
        onSecondaryContainer = LTAThemeColors.TextPrimary,
        tertiaryContainer = Color(0xFF353540),
        onTertiaryContainer = LTAThemeColors.TertiaryGold
    )

    val typography = Typography(
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    )

    val shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp)
    )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}