package com.mimik.wellnessnudge.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * The Daybreak brand gradient (iris → orchid → coral → honey), identical in both themes.
 * Reserved for AI moments: the orb, the Generate button, the nudge card and model downloads.
 */
object Daybreak {
    val Iris = Color(0xFF7B6CFF)
    val Orchid = Color(0xFFC86DD7)
    val Coral = Color(0xFFFF8A6B)
    val Honey = Color(0xFFFFC46B)

    /** Buttons and progress fills: left to right across the first three stops. */
    val Horizontal: Brush = Brush.horizontalGradient(listOf(Iris, Orchid, Coral))

    /** Borders and strokes: the first three stops, top-left to bottom-right. */
    val Diagonal: Brush = Brush.linearGradient(listOf(Iris, Orchid, Coral))

    /** All four stops, top-left to bottom-right. */
    val Full: Brush = Brush.linearGradient(listOf(Iris, Orchid, Coral, Honey))
}

/**
 * The extended palette behind [WellnessTheme]: surfaces, text and status tokens plus the
 * metric and category colors that Material's [ColorScheme] has no slots for.
 * Read it through `WellnessTheme.colors`.
 */
@Immutable
class WellnessColors(
    val isDark: Boolean,
    // Canvas and surfaces
    val bg: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    // Interaction and status
    val accent: Color,
    val accentStrong: Color,
    val onAccent: Color,
    val success: Color,
    val danger: Color,
    val warning: Color,
    // Metrics
    val sleep: Color,
    val deepSleep: Color,
    val rem: Color,
    val lightSleep: Color,
    val restingHr: Color,
    val hrv: Color,
    val steps: Color,
    // Categories (see CategoryStyle)
    val categorySleep: Color,
    val categoryRecovery: Color,
    val categoryEnergy: Color,
    val categoryStress: Color,
    val categoryMood: Color,
    val categoryFitness: Color,
    val categoryWeight: Color,
    val categoryAppetite: Color,
    val categoryGeneral: Color,
    // Atmosphere
    /** Alpha for tinted badge and icon-circle backgrounds. */
    val tintAlpha: Float,
    /** Colored glow under hero and AI elements. */
    val glow: Color,
    /** Peak alpha of the orb's outer halo. */
    val haloAlpha: Float,
    /** Stops of the radial glow at the top of every screen, center outwards. */
    val bgGlowIris: Color,
    val bgGlowOrchid: Color,
    val bgGlowCoral: Color,
    /** Soft card shadow; transparent in the dark theme, which uses hairlines only. */
    val shadowAmbient: Color,
    val shadowSpot: Color,
    /** Highlight that sweeps across skeleton placeholders. */
    val shimmer: Color,
) {
    /** Horizontal Daybreak gradient for buttons and progress fills. */
    val daybreak: Brush get() = Daybreak.Horizontal

    /** Diagonal Daybreak gradient for 1.5 dp borders. */
    val daybreakBorder: Brush get() = Daybreak.Diagonal

    /** [color] at the theme's tint alpha, for badge and chip backgrounds. */
    fun tint(color: Color): Color = color.copy(alpha = tintAlpha)
}

private val Ink = Color(0xFF16151D)

val DarkWellnessColors = WellnessColors(
    isDark = true,
    bg = Color(0xFF0B0B12),
    surface = Color(0xFF15151F),
    surfaceRaised = Color(0xFF1C1C29),
    surfaceSunken = Color(0xFF0F0F18),
    hairline = Color.White.copy(alpha = 0.07f),
    hairlineStrong = Color.White.copy(alpha = 0.12f),
    textPrimary = Color(0xFFF5F4FA),
    textSecondary = Color(0xFFA5A3B3),
    textTertiary = Color(0xFF6F6D80),
    accent = Color(0xFFA99EFF),
    accentStrong = Color(0xFF7B6CFF),
    onAccent = Color(0xFF0B0B12),
    success = Color(0xFF3DD6B0),
    danger = Color(0xFFFF6B81),
    warning = Color(0xFFFFB547),
    sleep = Color(0xFF7B6CFF),
    deepSleep = Color(0xFF5B8CFF),
    rem = Color(0xFFC86DD7),
    lightSleep = Color(0xFF6F6D80).copy(alpha = 0.35f),
    restingHr = Color(0xFFFF6B81),
    hrv = Color(0xFF3DD6B0),
    steps = Color(0xFFFFB547),
    categorySleep = Color(0xFF7B6CFF),
    categoryRecovery = Color(0xFF3DD6B0),
    categoryEnergy = Color(0xFFFFB547),
    categoryStress = Color(0xFF5BB8FF),
    categoryMood = Color(0xFFF58BD0),
    categoryFitness = Color(0xFFFF8A6B),
    categoryWeight = Color(0xFFA7E36B),
    categoryAppetite = Color(0xFFFFA36B),
    categoryGeneral = Color(0xFFA5A3B3),
    tintAlpha = 0.16f,
    glow = Daybreak.Iris.copy(alpha = 0.40f),
    haloAlpha = 0.35f,
    bgGlowIris = Daybreak.Iris.copy(alpha = 0.16f),
    bgGlowOrchid = Daybreak.Orchid.copy(alpha = 0.10f),
    bgGlowCoral = Daybreak.Coral.copy(alpha = 0.06f),
    shadowAmbient = Color.Transparent,
    shadowSpot = Color.Transparent,
    shimmer = Color.White.copy(alpha = 0.06f),
)

val LightWellnessColors = WellnessColors(
    isDark = false,
    bg = Color(0xFFF7F5F2),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF2F0EC),
    surfaceSunken = Color(0xFFECE9E4),
    hairline = Ink.copy(alpha = 0.08f),
    hairlineStrong = Ink.copy(alpha = 0.14f),
    textPrimary = Ink,
    textSecondary = Color(0xFF5E5C6C),
    textTertiary = Color(0xFF8E8C9B),
    accent = Color(0xFF5B4BE0),
    accentStrong = Color(0xFF4B3BD0),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF0E9F7E),
    danger = Color(0xFFE0445E),
    warning = Color(0xFFD98300),
    sleep = Color(0xFF5B4BE0),
    deepSleep = Color(0xFF3D6FE6),
    rem = Color(0xFFA948BC),
    lightSleep = Color(0xFF8E8C9B).copy(alpha = 0.35f),
    restingHr = Color(0xFFE0445E),
    hrv = Color(0xFF0E9F7E),
    steps = Color(0xFFD98300),
    categorySleep = Color(0xFF5B4BE0),
    categoryRecovery = Color(0xFF0E9F7E),
    categoryEnergy = Color(0xFFD98300),
    categoryStress = Color(0xFF1E88D8),
    categoryMood = Color(0xFFD0439A),
    categoryFitness = Color(0xFFE4572E),
    categoryWeight = Color(0xFF4E9A1E),
    categoryAppetite = Color(0xFFD66A1F),
    categoryGeneral = Color(0xFF6E6C7C),
    tintAlpha = 0.12f,
    glow = Daybreak.Iris.copy(alpha = 0.30f),
    haloAlpha = 0.20f,
    bgGlowIris = Daybreak.Iris.copy(alpha = 0.10f),
    bgGlowOrchid = Daybreak.Orchid.copy(alpha = 0.07f),
    bgGlowCoral = Daybreak.Coral.copy(alpha = 0.05f),
    shadowAmbient = Ink.copy(alpha = 0.05f),
    shadowSpot = Ink.copy(alpha = 0.09f),
    shimmer = Color.White.copy(alpha = 0.6f),
)

/**
 * Material color scheme derived from the tokens, so stock components (sheets, dialogs,
 * snackbars, sliders, text-field cursors) look native to the brand without per-call colors.
 */
internal fun WellnessColors.toColorScheme(): ColorScheme {
    val accentContainer = tint(accent).compositeOver(surface)
    val dangerContainer = tint(danger).compositeOver(surface)
    val tertiary = categoryFitness
    val tertiaryContainer = tint(tertiary).compositeOver(surface)
    return if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accentContainer,
            onPrimaryContainer = accent,
            inversePrimary = LightWellnessColors.accent,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = accentContainer,
            onSecondaryContainer = accent,
            tertiary = tertiary,
            onTertiary = onAccent,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = tertiary,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceRaised,
            onSurfaceVariant = textSecondary,
            surfaceTint = Color.Transparent,
            inverseSurface = textPrimary,
            inverseOnSurface = bg,
            error = danger,
            onError = onAccent,
            errorContainer = dangerContainer,
            onErrorContainer = danger,
            outline = hairlineStrong,
            outlineVariant = hairline,
            scrim = Color.Black,
            surfaceBright = surfaceRaised,
            surfaceDim = bg,
            surfaceContainerLowest = surfaceSunken,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceRaised,
            surfaceContainerHighest = surfaceRaised,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accentContainer,
            onPrimaryContainer = accentStrong,
            inversePrimary = DarkWellnessColors.accent,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = accentContainer,
            onSecondaryContainer = accentStrong,
            tertiary = tertiary,
            onTertiary = onAccent,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = tertiary,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceRaised,
            onSurfaceVariant = textSecondary,
            surfaceTint = Color.Transparent,
            inverseSurface = textPrimary,
            inverseOnSurface = bg,
            error = danger,
            onError = onAccent,
            errorContainer = dangerContainer,
            onErrorContainer = danger,
            outline = hairlineStrong,
            outlineVariant = hairline,
            scrim = Ink,
            surfaceBright = surface,
            surfaceDim = surfaceSunken,
            surfaceContainerLowest = surface,
            surfaceContainerLow = surface,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerHighest = surfaceRaised,
        )
    }
}
