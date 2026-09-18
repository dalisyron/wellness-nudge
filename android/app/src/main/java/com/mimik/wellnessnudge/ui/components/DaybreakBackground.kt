package com.mimik.wellnessnudge.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

private val GlowRadius = 520.dp

/**
 * Root of every screen: the canvas color with the Daybreak glow radiating from the top
 * center. The glow stays put while content scrolls over it.
 */
@Composable
fun DaybreakBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = WellnessTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val glow = Brush.radialGradient(
                    0f to colors.bgGlowIris,
                    0.35f to colors.bgGlowOrchid,
                    0.65f to colors.bgGlowCoral,
                    1f to colors.bgGlowCoral.copy(alpha = 0f),
                    center = Offset(size.width / 2f, 0f),
                    radius = GlowRadius.toPx(),
                )
                onDrawBehind {
                    drawRect(colors.bg)
                    drawRect(glow)
                }
            },
        content = content,
    )
}
