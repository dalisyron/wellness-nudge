package com.mimik.wellnessnudge.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Fades a horizontally scrolling row out at its start and end edges while there is more to
 * scroll that way, so a chip cut by the screen edge reads as "more this way" rather than as
 * clipping. Apply it before `horizontalScroll`, on the row that spans the screen.
 */
internal fun Modifier.horizontalScrollFades(scroll: ScrollState, length: Dp = 24.dp): Modifier = this
    .graphicsLayer {
        // The fades erase the row's own pixels, so it needs a layer of its own, only then.
        compositingStrategy = if (scroll.canScrollBackward || scroll.canScrollForward) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
    .drawWithContent {
        drawContent()
        val edge = length.toPx().coerceAtMost(size.width / 2f)
        val rtl = layoutDirection == LayoutDirection.Rtl
        // Scrolled away from the start, and room left toward the end, each up to one edge's length.
        val start = (scroll.value / edge).coerceIn(0f, 1f)
        val end = ((scroll.maxValue - scroll.value) / edge).coerceIn(0f, 1f)
        val left = if (rtl) end else start
        val right = if (rtl) start else end
        if (left > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(0f to Color.Black.copy(alpha = left), 1f to Color.Transparent, endX = edge),
                size = Size(edge, size.height),
                blendMode = BlendMode.DstOut,
            )
        }
        if (right > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black.copy(alpha = right),
                    startX = size.width - edge,
                    endX = size.width,
                ),
                topLeft = Offset(size.width - edge, 0f),
                size = Size(edge, size.height),
                blendMode = BlendMode.DstOut,
            )
        }
    }
