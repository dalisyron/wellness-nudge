package com.mimik.wellnessnudge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mimik.wellnessnudge.R

/** UI sans: labels, body copy and every number. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/** Editorial serif at its 16 pt text size: the 18 sp quotes and journal previews. */
val Newsreader = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
)

/**
 * Newsreader's 36 pt optical size: finer hairlines and tighter spacing for the display and
 * headline styles (greetings, titles, the nudge itself), where the text cut looks wide.
 */
val NewsreaderDisplay = FontFamily(
    Font(R.font.newsreader_display, FontWeight.Normal),
)

private const val TabularFigures = "tnum"

/**
 * Tabular (fixed-width) figures, for numbers that change in place: the live elapsed time
 * ("3.2 s"), download progress ("84 / 368 MB · 23%"), animating metric values. Running text
 * keeps proportional figures, where a tabular "1" looks detached.
 */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TabularFigures)

private fun manrope(weight: FontWeight, size: Int, line: Int, tracking: Float = 0f) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

private fun serif(
    family: FontFamily,
    size: Int,
    line: Int,
    tracking: Float = 0f,
    style: FontStyle = FontStyle.Normal,
) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight.Normal,
    fontStyle = style,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

/**
 * Material type scale. Serif display and headline styles carry the voice; Manrope carries
 * the interface. `labelSmall` is the eyebrow style: uppercase the text at the call site.
 */
val WellnessTypography = Typography(
    displayLarge = serif(NewsreaderDisplay, 36, 42, -0.3f),
    displayMedium = serif(NewsreaderDisplay, 32, 38, -0.2f),
    displaySmall = serif(NewsreaderDisplay, 28, 34, -0.1f),
    headlineLarge = serif(NewsreaderDisplay, 26, 32, -0.1f),
    headlineMedium = serif(NewsreaderDisplay, 24, 30, -0.1f),
    headlineSmall = serif(NewsreaderDisplay, 26, 35),
    titleLarge = manrope(FontWeight.SemiBold, 20, 26),
    titleMedium = manrope(FontWeight.SemiBold, 16, 22),
    titleSmall = manrope(FontWeight.SemiBold, 14, 20),
    bodyLarge = manrope(FontWeight.Normal, 16, 24),
    bodyMedium = manrope(FontWeight.Normal, 14, 20),
    bodySmall = manrope(FontWeight.Normal, 12, 16),
    labelLarge = manrope(FontWeight.Bold, 15, 20, 0.1f),
    labelMedium = manrope(FontWeight.SemiBold, 12, 16, 0.2f),
    labelSmall = manrope(FontWeight.Bold, 11, 14, 1.2f),
)

/** Styles outside Material's scale. Read them through `WellnessTheme.type`. */
@Immutable
class WellnessTypeExtras(
    /** Hero metric value, e.g. the sleep duration. Tabular figures. */
    val metricXL: TextStyle,
    /** Tile metric value. Tabular figures. */
    val metricL: TextStyle,
    /** Unit next to a metric value; pair it with textSecondary. Tabular figures. */
    val metricUnit: TextStyle,
    /** Quoted helpful nudges in For you. */
    val nudgeQuote: TextStyle,
    /** Nudge previews in the Journal. */
    val nudgePreview: TextStyle,
)

internal val WellnessTypeExtrasDefault = WellnessTypeExtras(
    // Metric values animate ("Sample day"), so their digits keep a fixed width.
    metricXL = manrope(FontWeight.Medium, 44, 48, -1.0f).tabular(),
    metricL = manrope(FontWeight.Medium, 32, 36, -0.6f).tabular(),
    metricUnit = manrope(FontWeight.Medium, 14, 18).tabular(),
    nudgeQuote = serif(Newsreader, 18, 26, style = FontStyle.Italic),
    nudgePreview = serif(Newsreader, 18, 25),
)
