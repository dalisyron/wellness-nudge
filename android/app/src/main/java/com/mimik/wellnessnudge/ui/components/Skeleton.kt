package com.mimik.wellnessnudge.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.LocalWellnessColors
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Sweeps a soft highlight across loading placeholders. The band travels in screen space,
 * so a single wave crosses every placeholder on screen: a 1.6 s sweep, then a short rest.
 * Clip before applying it; the clip also gives the shimmer its own layer, so frames redraw
 * only the placeholder. Previews, snapshot tests and "Remove animations" show plain blocks.
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

private class ShimmerNode :
    Modifier.Node(),
    DrawModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    /** How far the band has crossed the screen, 0 to 1; negative while resting or still. */
    private var sweep = Resting
    private var originX = 0f
    private var screenWidth = 0f
    private var brush: Brush? = null
    private var brushBand = 0f
    private var brushColor = Color.Unspecified

    override fun onAttach() {
        if (currentValueOf(LocalInspectionMode)) return
        coroutineScope.launch {
            // Compose mirrors the system animator scale here, as snapshot state.
            val motion = coroutineContext[MotionDurationScale]
            snapshotFlow { motion?.scaleFactor ?: 1f }.collectLatest { scale ->
                if (scale == 0f) {
                    update(Resting)
                    return@collectLatest
                }
                while (true) {
                    withInfiniteAnimationFrameMillis { frameMillis ->
                        // Every shimmer reads the same frame time, so they move in step.
                        val t = (frameMillis % CycleMillis).toFloat()
                        update(if (t < SweepMillis) WellnessMotion.Easing.transform(t / SweepMillis) else Resting)
                    }
                }
            }
        }
    }

    private fun update(value: Float) {
        if (value == sweep) return
        sweep = value
        invalidateDraw()
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        originX = coordinates.positionInRoot().x
        screenWidth = coordinates.findRootCoordinates().size.width.toFloat()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val progress = sweep
        if (progress < 0f) return
        val band = BandWidth.toPx()
        val x = -band + (screenWidth + band) * progress - originX
        if (x >= size.width || x + band <= 0f) return
        val brush = cachedBrush(band, currentValueOf(LocalWellnessColors).shimmer)
        translate(left = x) {
            drawRect(brush, topLeft = Offset.Zero, size = Size(band, size.height))
        }
    }

    // Rebuilt only when the density or theme changes, so frames allocate nothing.
    private fun cachedBrush(band: Float, highlight: Color): Brush {
        brush?.let { if (brushBand == band && brushColor == highlight) return it }
        val soft = highlight.copy(alpha = highlight.alpha * 0.35f)
        val clear = highlight.copy(alpha = 0f)
        return Brush.horizontalGradient(listOf(clear, soft, highlight, soft, clear), startX = 0f, endX = band).also {
            brush = it
            brushBand = band
            brushColor = highlight
        }
    }
}

private val BandWidth = 280.dp
private const val SweepMillis = 1600L
private const val CycleMillis = 2000L
private const val Resting = -1f
