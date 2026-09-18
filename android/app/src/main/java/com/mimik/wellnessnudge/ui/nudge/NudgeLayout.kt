package com.mimik.wellnessnudge.ui.nudge

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing

/** Actions pinned to the bottom of a stage, above the navigation bar. */
@Composable
internal fun StageFooter(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(top = 12.dp, bottom = 12.dp),
    ) {
        content()
    }
}

/**
 * Two buttons side by side at equal widths and heights, or stacked at full width when either
 * label wouldn't fit its half, e.g. at large font sizes.
 */
@Composable
internal fun ButtonPair(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = WellnessSpacing.ItemGap,
) {
    Layout(contents = listOf(first, second), modifier = modifier) { (firstSlot, secondSlot), constraints ->
        val a = firstSlot.first()
        val b = secondSlot.first()
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        val half = (width - gap) / 2
        val sideBySide = listOf(a, b).all { it.maxIntrinsicWidth(Constraints.Infinity) <= half }
        if (sideBySide) {
            val height = maxOf(a.maxIntrinsicHeight(half), b.maxIntrinsicHeight(half))
            val placeA = a.measure(Constraints.fixed(half, height))
            val placeB = b.measure(Constraints.fixed(half, height))
            layout(width, height) {
                placeA.placeRelative(0, 0)
                placeB.placeRelative(width - half, 0)
            }
        } else {
            val full = Constraints(minWidth = width, maxWidth = width)
            val placeA = a.measure(full)
            val placeB = b.measure(full)
            layout(width, placeA.height + gap + placeB.height) {
                placeA.placeRelative(0, 0)
                placeB.placeRelative(0, placeA.height + gap)
            }
        }
    }
}

/**
 * Fades the content out at the top and bottom edges while there is more to scroll that way,
 * instead of cutting it off under the top bar or above the footer.
 */
internal fun Modifier.scrollFades(scroll: ScrollState, length: Dp = 28.dp): Modifier = this
    .graphicsLayer {
        // The fades erase the content's own pixels, so it needs a layer of its own, only then.
        compositingStrategy = if (scroll.canScrollBackward || scroll.canScrollForward) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
    .drawWithContent {
        drawContent()
        val edge = length.toPx().coerceAtMost(size.height / 2f)
        val top = (scroll.value / edge).coerceIn(0f, 1f)
        if (top > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = top),
                    1f to Color.Transparent,
                    endY = edge,
                ),
                size = Size(size.width, edge),
                blendMode = BlendMode.DstOut,
            )
        }
        val bottom = ((scroll.maxValue - scroll.value) / edge).coerceIn(0f, 1f)
        if (bottom > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black.copy(alpha = bottom),
                    startY = size.height - edge,
                    endY = size.height,
                ),
                topLeft = Offset(0f, size.height - edge),
                size = Size(size.width, edge),
                blendMode = BlendMode.DstOut,
            )
        }
    }

/** Footer buttons are as tall as the gradient call to action. */
internal val FooterButtonHeight = 60.dp
