package com.mimik.wellnessnudge.ui.format

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import com.mimik.wellnessnudge.ui.theme.WellnessColors
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * How a wellness-goal category (the mim's classifier slug) is labeled, drawn and colored.
 * [color] is the pure hue for icons, dots and tinted backgrounds; [contentColor] is the same
 * hue adjusted to keep 4.5:1 contrast when used for text on the category's tint.
 */
@Immutable
data class CategoryStyle(
    val slug: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val contentColor: Color,
) {
    companion object {

        /** Every category the mim's classifier can return, "other" last. */
        val Slugs = listOf(
            "improve-sleep",
            "improve-recovery",
            "reduce-fatigue",
            "reduce-stress",
            "improve-mood",
            "improve-fitness",
            "weight-loss",
            "improve-appetite",
            "other",
        )

        /** The style for [slug] in the current theme; unknown or missing slugs read as "General". */
        @Composable
        @ReadOnlyComposable
        fun of(slug: String?): CategoryStyle = of(slug, WellnessTheme.colors)

        fun of(slug: String?, colors: WellnessColors): CategoryStyle {
            fun style(label: String, icon: ImageVector, color: Color) = CategoryStyle(
                slug = slug ?: "other",
                label = label,
                icon = icon,
                color = color,
                // Deepen the hue on paper, lift it on ink, so labels stay readable on the tint.
                contentColor = lerp(color, colors.textPrimary, if (colors.isDark) 0.2f else 0.35f),
            )
            return when (slug) {
                "improve-sleep" -> style("Sleep", Icons.Rounded.Bedtime, colors.categorySleep)
                "improve-recovery" -> style("Recovery", Icons.Rounded.SelfImprovement, colors.categoryRecovery)
                "reduce-fatigue" -> style("Energy", Icons.Rounded.Bolt, colors.categoryEnergy)
                "reduce-stress" -> style("Stress", Icons.Rounded.Spa, colors.categoryStress)
                "improve-mood" -> style("Mood", Icons.Rounded.SentimentSatisfied, colors.categoryMood)
                "improve-fitness" -> style("Fitness", Icons.AutoMirrored.Rounded.DirectionsRun, colors.categoryFitness)
                // The filled scale reads as a solid block at badge size; the outline reads as a scale.
                "weight-loss" -> style("Weight", Icons.Outlined.MonitorWeight, colors.categoryWeight)
                "improve-appetite" -> style("Appetite", Icons.Rounded.Restaurant, colors.categoryAppetite)
                else -> style("General", Icons.Rounded.AutoAwesome, colors.categoryGeneral)
            }
        }
    }
}
