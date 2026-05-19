package com.mimik.wellnessnudge.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Colors lifted verbatim from the Stitch design system DESIGN.md.
// Keep these as the single source of truth — don't shift them in screens.

object WellnessColors {
    val Surface = Color(0xFFF9FAF2)
    val SurfaceDim = Color(0xFFD9DBD3)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF3F4ED)
    val SurfaceContainer = Color(0xFFEDEFE7)
    val SurfaceContainerHigh = Color(0xFFE7E9E1)
    val SurfaceContainerHighest = Color(0xFFE2E3DC)
    val OnSurface = Color(0xFF191C18)
    val OnSurfaceVariant = Color(0xFF42493E)
    val InverseSurface = Color(0xFF2E312C)
    val InverseOnSurface = Color(0xFFF0F1EA)
    val Outline = Color(0xFF72796E)
    val OutlineVariant = Color(0xFFC2C9BB)

    val Primary = Color(0xFF154212)         // dark forest
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF2D5A27)
    val OnPrimaryContainer = Color(0xFF9DD090)
    val PrimaryFixed = Color(0xFFBCF0AE)
    val PrimaryFixedDim = Color(0xFFA1D494)
    val OnPrimaryFixed = Color(0xFF002201)
    val OnPrimaryFixedVariant = Color(0xFF23501E)

    val Secondary = Color(0xFF585F6C)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFDCE2F3)
    val OnSecondaryContainer = Color(0xFF5E6572)

    val Tertiary = Color(0xFF60233E)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFF7C3A55)
    val OnTertiaryContainer = Color(0xFFFFAAC8)

    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)

    // Status-light extras (not part of Material's standard palette)
    val StatusReady = Color(0xFF10B981)
    val StatusConnecting = Color(0xFFF59E0B)
    val StatusOffline = Color(0xFFEF4444)

    // "By the way" memory note tints
    val MemoryBackground = Color(0xFFF0F9FF)
    val MemoryText = Color(0xFF0369A1)
}

val WellnessLightColorScheme = lightColorScheme(
    primary = WellnessColors.Primary,
    onPrimary = WellnessColors.OnPrimary,
    primaryContainer = WellnessColors.PrimaryFixed,
    onPrimaryContainer = WellnessColors.OnPrimaryFixed,

    secondary = WellnessColors.Secondary,
    onSecondary = WellnessColors.OnSecondary,
    secondaryContainer = WellnessColors.SecondaryContainer,
    onSecondaryContainer = WellnessColors.OnSecondaryContainer,

    tertiary = WellnessColors.Tertiary,
    onTertiary = WellnessColors.OnTertiary,
    tertiaryContainer = WellnessColors.TertiaryContainer,
    onTertiaryContainer = WellnessColors.OnTertiaryContainer,

    background = WellnessColors.Surface,
    onBackground = WellnessColors.OnSurface,
    surface = WellnessColors.Surface,
    onSurface = WellnessColors.OnSurface,
    surfaceVariant = WellnessColors.SurfaceContainerHighest,
    onSurfaceVariant = WellnessColors.OnSurfaceVariant,

    surfaceContainerLowest = WellnessColors.SurfaceContainerLowest,
    surfaceContainerLow = WellnessColors.SurfaceContainerLow,
    surfaceContainer = WellnessColors.SurfaceContainer,
    surfaceContainerHigh = WellnessColors.SurfaceContainerHigh,
    surfaceContainerHighest = WellnessColors.SurfaceContainerHighest,

    outline = WellnessColors.Outline,
    outlineVariant = WellnessColors.OutlineVariant,

    inverseSurface = WellnessColors.InverseSurface,
    inverseOnSurface = WellnessColors.InverseOnSurface,
    inversePrimary = WellnessColors.PrimaryFixedDim,

    error = WellnessColors.Error,
    onError = WellnessColors.OnError,
    errorContainer = WellnessColors.ErrorContainer,
    onErrorContainer = WellnessColors.OnErrorContainer,
)

/**
 * Theme extensions for runtime status colors. These don't fit cleanly into
 * Material's slot system, so we expose them via a composition local.
 */
@Immutable
data class WellnessStatusColors(
    val ready: Color,
    val connecting: Color,
    val offline: Color,
    val memoryBackground: Color,
    val memoryText: Color,
)

val LocalStatusColors = staticCompositionLocalOf {
    WellnessStatusColors(
        ready = WellnessColors.StatusReady,
        connecting = WellnessColors.StatusConnecting,
        offline = WellnessColors.StatusOffline,
        memoryBackground = WellnessColors.MemoryBackground,
        memoryText = WellnessColors.MemoryText,
    )
}

object WellnessThemeExtras {
    val statusColors: WellnessStatusColors
        @Composable get() = androidx.compose.runtime.compositionLocalOf {
            WellnessStatusColors(
                WellnessColors.StatusReady,
                WellnessColors.StatusConnecting,
                WellnessColors.StatusOffline,
                WellnessColors.MemoryBackground,
                WellnessColors.MemoryText,
            )
        }.current
}
