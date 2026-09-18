package com.mimik.wellnessnudge.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.theme.rememberAnimationsEnabled
import kotlin.math.roundToInt

/**
 * Rounded progress meter on a quiet track. Changes in [progress] (0 to 1) animate.
 *
 * Meters are decorative by default: a tile's meter only marks a value's place in a range,
 * and the value itself is read out. Pass [progressDescription] (e.g. "84 of 368 MB") for
 * meters that report real progress, such as model downloads, to expose them to TalkBack.
 */
@Composable
fun LinearMeter(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    progressDescription: String? = null,
) = LinearMeter(progress, SolidColor(color), modifier, height, progressDescription)

/**
 * Rounded progress meter filled with [brush]. A gradient brush spans the whole track, so
 * the fill reveals more of it as progress grows (the Daybreak gradient warms up as a
 * download completes). See the [Color] overload for [progressDescription].
 */
@Composable
fun LinearMeter(
    progress: Float,
    brush: Brush,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    progressDescription: String? = null,
) {
    val track = WellnessTheme.colors.track
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(WellnessMotion.ValueMillis, easing = WellnessMotion.Easing),
        label = "meter",
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (progressDescription == null) {
                    Modifier.clearAndSetSemantics {}
                } else {
                    Modifier
                        .progressSemantics(target)
                        .semantics { stateDescription = progressDescription }
                },
            ),
    ) {
        drawPill(track, 0f, size.width)
        // A sliver of progress still shows as a round dot.
        if (animated > 0f) drawPill(brush, 0f, (size.width * animated).coerceAtLeast(size.height))
    }
}

/** Meter for work of unknown length: a segment of [brush] sweeping along the track. */
@Composable
fun IndeterminateMeter(
    brush: Brush,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val track = WellnessTheme.colors.track
    val sweep = if (rememberAnimationsEnabled()) {
        rememberInfiniteTransition(label = "indeterminate").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
            label = "sweep",
        )
    } else {
        null
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            // Its own layer, so each sweep frame redraws only the meter.
            .graphicsLayer()
            .progressSemantics(),
    ) {
        drawPill(track, 0f, size.width)
        val segment = size.width * 0.35f
        val start = -segment + (size.width + segment) * (sweep?.value ?: 0.55f)
        val from = start.coerceAtLeast(0f)
        val to = (start + segment).coerceAtMost(size.width)
        if (to > from) drawPill(brush, from, to - from)
    }
}

/**
 * Last night's sleep as one bar: deep, REM and light (the remainder) as rounded segments
 * separated by 3 dp gaps, with a legend below. Segment widths animate as values change.
 */
@Composable
fun SleepStagesBar(
    deepPct: Float,
    remPct: Float,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true,
) {
    val colors = WellnessTheme.colors
    val deepTarget = deepPct.coerceIn(0f, 100f)
    val remTarget = remPct.coerceIn(0f, 100f - deepTarget)
    val spec = tween<Float>(WellnessMotion.ValueMillis, easing = WellnessMotion.Easing)
    val deep by animateFloatAsState(deepTarget, spec, label = "deep")
    val rem by animateFloatAsState(remTarget, spec, label = "rem")
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(10.dp),
        ) {
            val light = (100f - deep - rem).coerceAtLeast(0f)
            val gap = 3.dp.toPx()
            val segments = (if (deep > 0f) 1 else 0) + (if (rem > 0f) 1 else 0) + (if (light > 0f) 1 else 0)
            val scale = (size.width - gap * (segments - 1).coerceAtLeast(0)) / 100f
            var x = drawSegment(0f, deep * scale, gap, colors.deepSleep)
            x = drawSegment(x, rem * scale, gap, colors.rem)
            drawSegment(x, light * scale, gap, colors.lightSleep)
        }
        if (showLegend) {
            Spacer(Modifier.height(12.dp))
            val deepShown = deepTarget.roundToInt()
            val remShown = remTarget.roundToInt()
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                LegendItem("Deep", deepShown, colors.deepSleep)
                LegendItem("REM", remShown, colors.rem)
                LegendItem("Light", (100 - deepShown - remShown).coerceAtLeast(0), colors.lightSleep)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label $percent%",
            style = MaterialTheme.typography.bodySmall,
            color = WellnessTheme.colors.textSecondary,
        )
    }
}

/** Draws one stage segment when it has any width and returns where the next one starts. */
private fun DrawScope.drawSegment(x: Float, width: Float, gap: Float, color: Color): Float {
    if (width <= 0f) return x
    drawPill(color, x, width)
    return x + width + gap
}

/** A pill as tall as the canvas, spanning [width] from [x]. */
private fun DrawScope.drawPill(brush: Brush, x: Float, width: Float) = drawRoundRect(
    brush = brush,
    topLeft = Offset(x, 0f),
    size = Size(width.coerceAtMost(size.width - x), size.height),
    cornerRadius = CornerRadius(size.height / 2f),
)

private fun DrawScope.drawPill(color: Color, x: Float, width: Float) = drawRoundRect(
    color = color,
    topLeft = Offset(x, 0f),
    size = Size(width.coerceAtMost(size.width - x), size.height),
    cornerRadius = CornerRadius(size.height / 2f),
)
