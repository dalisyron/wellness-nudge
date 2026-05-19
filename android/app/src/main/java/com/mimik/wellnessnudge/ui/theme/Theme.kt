package com.mimik.wellnessnudge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * The single, opinionated brand theme. We deliberately don't pull dynamic
 * colors from the user's wallpaper — the cream + forest-green identity is
 * the point. Dark mode is intentionally not supported yet; Stitch only
 * shipped a light spec.
 */
@Composable
fun WellnessNudgeTheme(
    content: @Composable () -> Unit,
) {
    val statusColors = WellnessStatusColors(
        ready = WellnessColors.StatusReady,
        connecting = WellnessColors.StatusConnecting,
        offline = WellnessColors.StatusOffline,
        memoryBackground = WellnessColors.MemoryBackground,
        memoryText = WellnessColors.MemoryText,
    )
    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = WellnessLightColorScheme,
            typography = WellnessTypography,
            content = content,
        )
    }
}
