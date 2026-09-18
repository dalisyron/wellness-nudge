package com.mimik.wellnessnudge.ui.components

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Colored light spilling from behind [shape], as under the Generate button: the shape
 * blurred by [blurRadius] and shifted down by [offsetY]. Needs API 28 for blurred drawing
 * on a hardware canvas; older devices simply skip the glow.
 */
fun Modifier.glow(
    color: Color,
    shape: Shape,
    blurRadius: Dp = 24.dp,
    offsetY: Dp = 0.dp,
    spread: Dp = 0.dp,
): Modifier {
    if (!color.isSpecified || color.alpha == 0f || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return this
    return drawWithCache {
        val spreadPx = spread.toPx()
        val outline = shape.createOutline(
            Size(size.width + spreadPx * 2, size.height + spreadPx * 2),
            layoutDirection,
            this,
        )
        val path = outline.toPath().asAndroidPath()
        val blurPx = blurRadius.toPx()
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            if (blurPx > 0f) maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        val dy = offsetY.toPx() - spreadPx
        onDrawBehind {
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                val saved = native.save()
                native.translate(-spreadPx, dy)
                native.drawPath(path, paint)
                native.restoreToCount(saved)
            }
        }
    }
}

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
