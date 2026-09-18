package com.mimik.wellnessnudge.ui.components

import android.provider.Settings
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.LocalWellnessColors
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Sweeps a soft highlight across the element, for loading placeholders. Every shimmer on
 * screen shares the frame clock, so they move in step. Clip before applying it; it is
 * static in previews and when animations are off.
 */
fun Modifier.shimmer(): Modifier = this then ShimmerElement

/**
 * A loading placeholder block with a moving highlight; size it with [modifier]. It uses
 * surfaceRaised on ink and surfaceSunken on paper, so it reads on the canvas and on cards.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val colors = WellnessTheme.colors
    Box(
        modifier
            .clip(shape)
            .background(if (colors.isDark) colors.surfaceRaised else colors.surfaceSunken)
            .shimmer(),
    )
}

private data object ShimmerElement : ModifierNodeElement<ShimmerNode>() {
    override fun create() = ShimmerNode()
    override fun update(node: ShimmerNode) = Unit
    override fun InspectorInfo.inspectableProperties() {
        name = "shimmer"
    }
}

private class ShimmerNode : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    private var progress = StillProgress
    private var brush: Brush? = null
    private var brushBand = 0f
    private var brushColor = Color.Unspecified

    override fun onAttach() {
        if (currentValueOf(LocalInspectionMode)) return
        val resolver = currentValueOf(LocalContext).contentResolver
        if (Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) return
        coroutineScope.launch {
            while (isActive) {
                withInfiniteAnimationFrameMillis { frameMillis ->
                    progress = (frameMillis % PeriodMillis) / PeriodMillis.toFloat()
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val band = size.width * BandFraction
        val highlight = currentValueOf(LocalWellnessColors).shimmer
        val brush = cachedBrush(band, highlight)
        val x = -band + (size.width + band) * progress
        translate(left = x) {
            drawRect(brush, topLeft = Offset.Zero, size = Size(band, size.height))
        }
    }

    // Rebuilt only when the size or theme changes, so frames allocate nothing.
    private fun cachedBrush(band: Float, highlight: Color): Brush {
        brush?.let { if (brushBand == band && brushColor == highlight) return it }
        return Brush.horizontalGradient(
            listOf(highlight.copy(alpha = 0f), highlight, highlight.copy(alpha = 0f)),
            startX = 0f,
            endX = band,
        ).also {
            brush = it
            brushBand = band
            brushColor = highlight
        }
    }
}

private const val PeriodMillis = 1400L
private const val BandFraction = 0.6f
private const val StillProgress = 0.35f
