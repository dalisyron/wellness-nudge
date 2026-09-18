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
}

/**
 * The extended palette behind [WellnessTheme]: surfaces, text and status tokens plus the
 * metric and category colors that Material's [ColorScheme] has no slots for.
 * Read it through `WellnessTheme.colors`.
 *
 * Every `text*` token and the `*Text` status tokens keep at least 4.5:1 on bg, surface,
 * surfaceRaised and the top glow. [textDisabled] is the exception, on purpose.
 */
@Immutable
class WellnessColors(
    val isDark: Boolean,
    // Canvas and surfaces
    val bg: Color,
    val surface: Color,
    val surfaceRaised: Color,
    /** Wells inside cards. Meter and slider tracks use [track] and [trackStrong]. */
    val surfaceSunken: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    // Controls
    /** Pill and circle buttons, the On-device pill and text inputs: raised on ink, white paper in light. */
    val controlFill: Color,
    /** Border of controls and chips. */
    val controlBorder: Color,
    /** Unselected chips: raised on ink, a translucent ink wash in light, so they work on the canvas and on sheets. */
    val chipFill: Color,
    /** Unfilled part of meters. */
    val track: Color,
    /** Unfilled part of slider tracks, a step stronger so the end of the range stays visible. */
    val trackStrong: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    /** Captions, meta lines, eyebrows, timestamps and placeholders. Still 4.5:1. */
    val textTertiary: Color,
    /**
     * Below text contrast by design: disabled labels and icons, and quiet glyphs such as the
     * edit pencil or an input's leading icon. Never for words someone needs to read.
     */
    val textDisabled: Color,
    // Interaction and status
    val accent: Color,
    val accentStrong: Color,
    val onAccent: Color,
    /** Accent content sitting on an accent tint: a selected chip's label, the selected tab. */
    val accentContent: Color,
    /** Status hues for dots, fills, meters and icons. */
    val success: Color,
    val danger: Color,
    val warning: Color,
    /** Status hues for text, e.g. a selected "Helpful", a "Delete" button or a failure message. */
    val successText: Color,
    val dangerText: Color,
    val warningText: Color,
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
    /** Opacity of the colored light under hero and AI elements, such as the Generate button's glow. */
    val glowAlpha: Float,
    /** Peak alpha of the orb's outer halo. */
    val haloAlpha: Float,
    /** Stops of the radial glow at the top of every screen, center outwards. */
    val bgGlowInner: Color,
    val bgGlowMiddle: Color,
    val bgGlowOuter: Color,
    /**
     * The light theme's paper shadow, drawn blurred under cards, the tab bar and pill controls:
     * [shadowAmbient] is the tight contact shadow, [shadowSpot] the wide, soft one below.
     * Transparent in the dark theme, which separates surfaces with hairlines only.
     */
    val shadowAmbient: Color,
    val shadowSpot: Color,
    /** Highlight that sweeps across skeleton placeholders. */
    val shimmer: Color,
    /** Loading placeholders on the canvas. */
    val skeleton: Color,
    /** Loading placeholders inside a card or a sheet, a step lighter on ink so they read on the surface. */
    val skeletonOnSurface: Color,
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
    controlFill = Color(0xFF1C1C29),
    controlBorder = Color.White.copy(alpha = 0.07f),
    chipFill = Color(0xFF1C1C29),
    track = Color.White.copy(alpha = 0.10f),
    trackStrong = Color.White.copy(alpha = 0.14f),
    textPrimary = Color(0xFFF5F4FA),
    textSecondary = Color(0xFFA5A3B3),
    textTertiary = Color(0xFF8A889A),
    textDisabled = Color(0xFF6F6D80),
    accent = Color(0xFFA99EFF),
    accentStrong = Color(0xFF7B6CFF),
    onAccent = Color(0xFF0B0B12),
    accentContent = Color(0xFFC9C2FF),
    success = Color(0xFF3DD6B0),
    danger = Color(0xFFFF6B81),
    warning = Color(0xFFFFB547),
    // The dark hues already read as text (5.4:1 and up).
    successText = Color(0xFF3DD6B0),
    dangerText = Color(0xFFFF6B81),
    warningText = Color(0xFFFFB547),
    sleep = Color(0xFF7B6CFF),
    deepSleep = Color(0xFF5B8CFF),
    rem = Color(0xFFC86DD7),
    // A quiet lavender from the same family: light sleep is a stage, not an empty track.
    lightSleep = Color(0xFF9C95E6).copy(alpha = 0.55f),
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
    glowAlpha = 0.40f,
    haloAlpha = 0.35f,
    // Cool light only: warm coral over ink turns maroon at the screen edges.
    bgGlowInner = Daybreak.Iris.copy(alpha = 0.16f),
    bgGlowMiddle = Daybreak.Orchid.copy(alpha = 0.08f),
    bgGlowOuter = Daybreak.Orchid.copy(alpha = 0.03f),
    shadowAmbient = Color.Transparent,
    shadowSpot = Color.Transparent,
    shimmer = Color.White.copy(alpha = 0.05f),
    skeleton = Color(0xFF1C1C29),
    // White at 8% over the surface: #282831, 1.24:1 (surfaceRaised would be 1.08:1 there).
    skeletonOnSurface = Color.White.copy(alpha = 0.08f),
)

val LightWellnessColors = WellnessColors(
    isDark = false,
    bg = Color(0xFFF7F5F2),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF2F0EC),
    surfaceSunken = Color(0xFFECE9E4),
    hairline = Ink.copy(alpha = 0.08f),
    hairlineStrong = Ink.copy(alpha = 0.14f),
    controlFill = Color(0xFFFFFFFF),
    controlBorder = Ink.copy(alpha = 0.12f),
    chipFill = Ink.copy(alpha = 0.06f),
    track = Color(0xFFECE9E4),
    trackStrong = Color(0xFFE6E3DD),
    textPrimary = Ink,
    textSecondary = Color(0xFF524F5F),
    textTertiary = Color(0xFF686676),
    textDisabled = Color(0xFF8E8C9B),
    accent = Color(0xFF5B4BE0),
    accentStrong = Color(0xFF4B3BD0),
    onAccent = Color(0xFFFFFFFF),
    accentContent = Color(0xFF4B3BD0),
    success = Color(0xFF0E9F7E),
    danger = Color(0xFFE0445E),
    warning = Color(0xFFD98300),
    // Deepened so text keeps 4.5:1 on paper, white, raised and the status's own 12-16% tint.
    successText = Color(0xFF0A6F58),
    dangerText = Color(0xFFBA1F38),
    warningText = Color(0xFF905600),
    sleep = Color(0xFF5B4BE0),
    deepSleep = Color(0xFF3D6FE6),
    rem = Color(0xFFA948BC),
    lightSleep = Color(0xFF8C84E0).copy(alpha = 0.45f),
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
    glowAlpha = 0.26f,
    haloAlpha = 0.20f,
    bgGlowInner = Daybreak.Iris.copy(alpha = 0.10f),
    bgGlowMiddle = Daybreak.Orchid.copy(alpha = 0.07f),
    bgGlowOuter = Daybreak.Coral.copy(alpha = 0.05f),
    shadowAmbient = Ink.copy(alpha = 0.05f),
    shadowSpot = Ink.copy(alpha = 0.08f),
    shimmer = Color.White.copy(alpha = 0.45f),
    skeleton = Color(0xFFECE9E4),
    skeletonOnSurface = Color(0xFFECE9E4),
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
            onPrimaryContainer = accentContent,
            inversePrimary = LightWellnessColors.accent,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = accentContainer,
            onSecondaryContainer = accentContent,
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
            error = dangerText,
            onError = onAccent,
            errorContainer = dangerContainer,
            onErrorContainer = dangerText,
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
            onPrimaryContainer = accentContent,
            inversePrimary = DarkWellnessColors.accent,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = accentContainer,
            onSecondaryContainer = accentContent,
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
            // Stock error labels and supporting text are text: use the deepened red.
            error = dangerText,
            onError = onAccent,
            errorContainer = dangerContainer,
            onErrorContainer = dangerText,
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
