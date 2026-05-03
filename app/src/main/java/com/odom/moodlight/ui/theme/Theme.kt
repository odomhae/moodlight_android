package com.odom.moodlight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    background = AppColors.Background,
    surface = AppColors.Panel,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary,
    primary = AppColors.WarmYellow,
    onPrimary = AppColors.Background,
)

@Composable
fun MoodLightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
