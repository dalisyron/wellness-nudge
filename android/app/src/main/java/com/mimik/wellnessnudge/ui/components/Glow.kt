package com.mimik.wellnessnudge.ui.components

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessColors
import kotlin.math.roundToInt

/**
 * Colored light spilling from behind [shape]: the shape blurred by [blurRadius], shifted
 * down by [offsetY] and grown by [spread] (negative tucks it under the shape). Needs API 28
 * for blurred drawing on a hardware canvas; older devices simply skip the glow.
 */
fun Modifier.glow(
    color: Color,
    shape: Shape,
    blurRadius: Dp = 24.dp,
    offsetY: Dp = 0.dp,
    spread: Dp = 0.dp,
): Modifier {
    if (!color.isSpecified || color.alpha == 0f) return this
    return glow(SolidColor(color), shape, alpha = 1f, blurRadius, offsetY, spread)
}

/**
 * Light in the colors of [brush] spilling from behind [shape] at [alpha], e.g. the Daybreak
 * gradient under the Generate button, so each end of the pill glows in its own hue. The
 * brush spans the glow's bounds; see the [Color] overload for the other parameters.
 */
fun Modifier.glow(
    brush: Brush,
    shape: Shape,
    alpha: Float,
    blurRadius: Dp = 24.dp,
    offsetY: Dp = 0.dp,
    spread: Dp = 0.dp,
): Modifier {
    if (alpha <= 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return this
    return drawWithCache { blurredShape(brush, shape, alpha, blurRadius, offsetY, spread) }
}

/**
 * The soft paper shadow of the light theme: a tight contact shadow and a wide, softer key
 * shadow below [shape], in the theme's shadowAmbient and shadowSpot colors. Nothing in the
 * dark theme, which separates surfaces with hairlines. Drawn with a blur rather than
 * elevation, so previews and snapshot tests show exactly what the phone does.
 */
internal fun Modifier.paperShadow(colors: WellnessColors, shape: Shape, elevation: Dp): Modifier {
    if (colors.isDark) return this
    return this
        .glow(colors.shadowSpot, shape, blurRadius = elevation * 2f, offsetY = elevation * 0.6f)
        .glow(colors.shadowAmbient, shape, blurRadius = elevation * 0.35f, offsetY = elevation * 0.1f)
}

private fun CacheDrawScope.blurredShape(
    brush: Brush,
    shape: Shape,
    alpha: Float,
    blurRadius: Dp,
    offsetY: Dp,
    spread: Dp,
): DrawResult {
    val spreadPx = spread.toPx()
    val glowSize = Size(size.width + spreadPx * 2, size.height + spreadPx * 2)
    val outline = shape.createOutline(glowSize, layoutDirection, this)
    // Evenly rounded shapes (pills, cards) draw as a round rect, which the GPU blurs analytically.
    val roundRect = (outline as? Outline.Rounded)?.roundRect?.takeIf { it.hasEvenCorners() }
    val shapePath = if (roundRect == null) outline.toPath().asAndroidPath() else null
    val paint = Paint()
    brush.applyTo(glowSize, paint, alpha)
    val blurPx = blurRadius.toPx()
    if (blurPx > 0f) paint.asFrameworkPaint().maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
    val dy = offsetY.toPx() - spreadPx
    return onDrawBehind {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val saved = native.save()
            native.translate(-spreadPx, dy)
            val framework = paint.asFrameworkPaint()
            if (roundRect != null) {
                val r = roundRect.topLeftCornerRadius
                native.drawRoundRect(roundRect.left, roundRect.top, roundRect.right, roundRect.bottom, r.x, r.y, framework)
            } else if (shapePath != null) {
                native.drawPath(shapePath, framework)
            }
            native.restoreToCount(saved)
        }
    }
}

private fun RoundRect.hasEvenCorners(): Boolean =
    topLeftCornerRadius == topRightCornerRadius &&
        topLeftCornerRadius == bottomRightCornerRadius &&
        topLeftCornerRadius == bottomLeftCornerRadius

/**
 * A soft radial wash of [color] centered on [alignment]'s point of the bounds and fading
 * out at [radius], e.g. the orchid glow in a corner of the nudge card.
 */
fun Modifier.radialGlow(
    color: Color,
    alignment: Alignment = Alignment.TopEnd,
    radius: Dp = 240.dp,
): Modifier {
    if (!color.isSpecified || color.alpha == 0f) return this
    return drawWithCache {
        val point = alignment.align(
            IntSize.Zero,
            IntSize(size.width.roundToInt(), size.height.roundToInt()),
            layoutDirection,
        )
        val brush = Brush.radialGradient(
            0f to color,
            0.45f to color.copy(alpha = color.alpha * 0.45f),
            1f to color.copy(alpha = 0f),
            center = Offset(point.x.toFloat(), point.y.toFloat()),
            radius = radius.toPx(),
        )
        onDrawBehind { drawRect(brush) }
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}
