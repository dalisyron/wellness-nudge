package com.mimik.wellnessnudge.ui.journal

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessColors

/**
 * Paints the Daybreak canvas behind this element exactly as the screen shows it at that spot,
 * so a pinned header hides the entries scrolling under it yet stays invisible against the
 * background. Its bottom [fade] dissolves into whatever passes underneath. Nothing is painted
 * while not [enabled], e.g. while the header sits in its own place, where it would cover the
 * paper shadow of the card above.
 *
 * It mirrors DaybreakBackground drawn full screen, as the app shell draws it: the bg fill plus
 * the glow radiating from the top center of the screen.
 */
internal fun Modifier.canvasBackdrop(colors: WellnessColors, enabled: Boolean, fade: Dp = 0.dp): Modifier =
    this then CanvasBackdropElement(colors, enabled, fade)

private data class CanvasBackdropElement(val colors: WellnessColors, val enabled: Boolean, val fade: Dp) :
    ModifierNodeElement<CanvasBackdropNode>() {

    override fun create() = CanvasBackdropNode(colors, enabled, fade)

    override fun update(node: CanvasBackdropNode) = node.update(colors, enabled, fade)

    override fun InspectorInfo.inspectableProperties() {
        name = "canvasBackdrop"
        properties["enabled"] = enabled
        properties["fade"] = fade
    }
}

private class CanvasBackdropNode(
    private var colors: WellnessColors,
    private var enabled: Boolean,
    private var fade: Dp,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    /** Where this element sits on screen; the canvas is drawn relative to it. */
    private var origin = Offset.Unspecified
    private var screenWidth = 0f
    private val layerPaint = Paint()

    fun update(colors: WellnessColors, enabled: Boolean, fade: Dp) {
        this.colors = colors
        this.enabled = enabled
        this.fade = fade
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
            val fadeHeight = fade.toPx().coerceIn(0f, size.height)
            drawContext.canvas.withSaveLayer(size.toRect(), layerPaint) {
                drawRect(colors.bg)
                drawRect(glow)
                if (fadeHeight > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black,
                            1f to Color.Transparent,
                            startY = size.height - fadeHeight,
                            endY = size.height,
                        ),
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
