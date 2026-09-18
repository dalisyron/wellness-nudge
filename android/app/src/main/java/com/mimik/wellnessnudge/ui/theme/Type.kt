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
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

/** Editorial serif: greetings, titles and the nudge itself. */
val Newsreader = FontFamily(
    Font(R.font.newsreader_regular, FontWeight.Normal),
    Font(R.font.newsreader_medium, FontWeight.Medium),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
)

// Tabular figures keep metric values from jittering while they animate. The serif styles
// set prose (numbers inside sentences), which reads better with proportional figures.
private const val TabularFigures = "tnum"

private fun manrope(weight: FontWeight, size: Int, line: Int, tracking: Float = 0f) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
    fontFeatureSettings = TabularFigures,
)

private fun newsreader(size: Int, line: Int, tracking: Float = 0f, style: FontStyle = FontStyle.Normal) = TextStyle(
    fontFamily = Newsreader,
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
    displayLarge = newsreader(36, 42, -0.5f),
    displayMedium = newsreader(32, 38, -0.3f),
    displaySmall = newsreader(28, 34, -0.3f),
    headlineLarge = newsreader(26, 32, -0.2f),
    headlineMedium = newsreader(24, 30, -0.2f),
    headlineSmall = newsreader(26, 35, -0.2f),
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
    /** Hero metric value, e.g. the sleep duration. */
    val metricXL: TextStyle,
    /** Tile metric value. */
    val metricL: TextStyle,
    /** Unit next to a metric value; pair it with textSecondary. */
    val metricUnit: TextStyle,
    /** Quoted helpful nudges in For you. */
    val nudgeQuote: TextStyle,
    /** Nudge previews in the Journal. */
    val nudgePreview: TextStyle,
)

internal val WellnessTypeExtrasDefault = WellnessTypeExtras(
    metricXL = manrope(FontWeight.Medium, 44, 48, -1.0f),
    metricL = manrope(FontWeight.Medium, 32, 36, -0.6f),
    metricUnit = manrope(FontWeight.Medium, 14, 18),
    nudgeQuote = newsreader(18, 26, style = FontStyle.Italic),
    nudgePreview = newsreader(18, 25),
)
