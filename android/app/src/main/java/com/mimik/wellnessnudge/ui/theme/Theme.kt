package com.mimik.wellnessnudge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalWellnessColors = staticCompositionLocalOf { DarkWellnessColors }
internal val LocalWellnessTypeExtras = staticCompositionLocalOf { WellnessTypeExtrasDefault }

private val DarkColorScheme = DarkWellnessColors.toColorScheme()
private val LightColorScheme = LightWellnessColors.toColorScheme()

/**
 * The Daybreak theme: a deep-ink dark theme and a "morning paper" light theme, following
 * the system setting by default. No dynamic color; the brand palette is the point.
 */
@Composable
fun WellnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkWellnessColors else LightWellnessColors
    CompositionLocalProvider(
        LocalWellnessColors provides colors,
        LocalWellnessTypeExtras provides WellnessTypeExtrasDefault,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = WellnessTypography,
            shapes = WellnessMaterialShapes,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.textPrimary, content = content)
        }
    }
}

/** Daybreak extensions to [MaterialTheme]: the extended palette and the extra text styles. */
object WellnessTheme {
    val colors: WellnessColors
        @Composable @ReadOnlyComposable get() = LocalWellnessColors.current

    val type: WellnessTypeExtras
        @Composable @ReadOnlyComposable get() = LocalWellnessTypeExtras.current
}
