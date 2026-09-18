package com.mimik.wellnessnudge.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessColors
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * Paints the Daybreak canvas behind this element exactly as the screen shows it at that spot,
 * so e.g. a pinned header hides the entries scrolling under it yet stays invisible against
 * the background. [fade] dissolves the element's own bottom edge; [fadeBelow] instead keeps
 * the element opaque and extends the canvas past its bottom edge, dissolving over that
 * length, so whatever passes underneath fades out below the element rather than inside it.
 * Nothing is painted while not [enabled], e.g. while a header sits in its own place, where it
 * would cover the paper shadow of the card above.
 *
 * It mirrors DaybreakBackground drawn full screen, as the app shell draws it: the bg fill plus
 * the glow radiating from the top center of the screen.
 */
internal fun Modifier.canvasBackdrop(
    colors: WellnessColors,
    enabled: Boolean,
    fade: Dp = 0.dp,
    fadeBelow: Dp = 0.dp,
): Modifier = this then CanvasBackdropElement(colors, enabled, fade, fadeBelow)

/**
 * The canvas drawn over the top [height] of a list's viewport, fading downwards, so content
 * scrolled up dissolves into the edge under the status bar instead of being cut there.
 * [progress] (0 to 1) is how far the list has scrolled from its start, in fade lengths.
 */
@Composable
internal fun ListTopScrim(progress: () -> Float, modifier: Modifier = Modifier, height: Dp = ListEdgeFade) {
    val colors = WellnessTheme.colors
    Spacer(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { alpha = progress() }
            .canvasBackdrop(colors, enabled = true, fade = height),
    )
}

/** How far [this] list has scrolled from its start, in lengths of [fade], from 0 to 1. */
internal fun LazyListState.scrolledFraction(fade: Float): Float = when {
    firstVisibleItemIndex > 0 -> 1f
    fade <= 0f -> 0f
    else -> (firstVisibleItemScrollOffset / fade).coerceIn(0f, 1f)
}

/**
 * The canvas behind the floating tab bar, fading in over [FooterFade] above it (eased, so
 * the fade has no visible start), so a list's content dissolves before it reaches the bar
 * instead of showing through it. [contentPadding] is the bar's room at the bottom (or the
 * keyboard's), read during layout.
 */
@Composable
internal fun BottomBarScrim(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val bg = WellnessTheme.colors.bg
    Spacer(
        modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val height = (contentPadding.calculateBottomPadding() + FooterFade).roundToPx()
                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .drawWithCache {
                val scrim = easedFade(bg, FooterFade.toPx())
                onDrawBehind { drawRect(scrim) }
            },
    )
}

/** [color] fading in from transparent over [length], eased in and out (smoothstep), then solid. */
internal fun easedFade(color: Color, length: Float): Brush = Brush.verticalGradient(
    0f to color.copy(alpha = 0f),
    0.25f to color.copy(alpha = 0.16f),
    0.5f to color.copy(alpha = 0.5f),
    0.75f to color.copy(alpha = 0.84f),
    1f to color,
    startY = 0f,
    endY = length,
)

/** The fade above a bottom action or the tab bar. */
internal val FooterFade = 44.dp

/** The fade at a list's top edge, under the status bar. */
internal val ListEdgeFade = 16.dp

private data class CanvasBackdropElement(
    val colors: WellnessColors,
    val enabled: Boolean,
    val fade: Dp,
    val fadeBelow: Dp,
) : ModifierNodeElement<CanvasBackdropNode>() {

    override fun create() = CanvasBackdropNode(colors, enabled, fade, fadeBelow)

    override fun update(node: CanvasBackdropNode) = node.update(colors, enabled, fade, fadeBelow)

    override fun InspectorInfo.inspectableProperties() {
        name = "canvasBackdrop"
        properties["enabled"] = enabled
        properties["fade"] = fade
        properties["fadeBelow"] = fadeBelow
    }
}

private class CanvasBackdropNode(
    private var colors: WellnessColors,
    private var enabled: Boolean,
    private var fade: Dp,
    private var fadeBelow: Dp,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    /** Where this element sits on screen; the canvas is drawn relative to it. */
    private var origin = Offset.Unspecified
    private var screenWidth = 0f
    private val layerPaint = Paint()

    fun update(colors: WellnessColors, enabled: Boolean, fade: Dp, fadeBelow: Dp) {
        this.colors = colors
        this.enabled = enabled
        this.fade = fade
        this.fadeBelow = fadeBelow
        invalidateDraw()
    }

    // Lazy lists move items without redrawing them, so redraw whenever the spot changes.
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val position = coordinates.positionInRoot()
        val width = coordinates.findRootCoordinates().size.width.toFloat()
        if (position == origin && width == screenWidth) return
        origin = position
        screenWidth = width
        if (enabled) invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        if (enabled && origin.isSpecified) {
            val glow = Brush.radialGradient(
                0f to colors.bgGlowInner,
                0.35f to colors.bgGlowMiddle,
                0.65f to colors.bgGlowOuter,
                1f to colors.bgGlowOuter.copy(alpha = 0f),
                center = Offset(screenWidth / 2f - origin.x, -origin.y),
                radius = CanvasGlowRadius.toPx(),
            )
            val below = fadeBelow.toPx().coerceAtLeast(0f)
            val bottom = size.height + below
            val solidUntil = (size.height - fade.toPx().coerceIn(0f, size.height)).coerceAtMost(bottom)
            val bounds = Rect(0f, 0f, size.width, bottom)
            drawContext.canvas.withSaveLayer(bounds, layerPaint) {
                drawRect(colors.bg, size = bounds.size)
                drawRect(glow, size = bounds.size)
                if (solidUntil < bottom) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black,
                            1f to Color.Transparent,
                            startY = solidUntil,
                            endY = bottom,
                        ),
                        size = bounds.size,
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
        }
        drawContent()
    }
}

/** DaybreakBackground's glow radius. */
private val CanvasGlowRadius = 520.dp
