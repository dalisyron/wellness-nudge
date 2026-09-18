package com.mimik.wellnessnudge.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * Material slider dressed for the metric editors: an 8 dp rounded track filled with the
 * metric [color] and a white 24 dp thumb ringed in it that grows slightly while held. The
 * track spans the slider's bounds, lining up with the labels above it; at either end the
 * thumb overhangs by half its width. [valueDescription] is what TalkBack reads for the
 * value, e.g. "6h 30m" instead of "54 percent".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    valueDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .then(if (valueDescription != null) Modifier.semantics { stateDescription = valueDescription } else Modifier)
            .layout { measurable, constraints ->
                // Material insets the track by half a thumb on each side; widen the slider by
                // the same amount so the track itself fills the given width.
                val overhang = (ThumbSize / 2).roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = constraints.minWidth + overhang * 2,
                        maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + overhang * 2 else constraints.maxWidth,
                    ),
                )
                layout(placeable.width - overhang * 2, placeable.height) { placeable.place(-overhang, 0) }
            },
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        thumb = { SliderThumb(color, interactionSource) },
        track = { state -> SliderTrack(state, color) },
    )
}

@Composable
private fun SliderThumb(color: Color, interactionSource: MutableInteractionSource) {
    val active = interactionSource.collectIsPressedAsState().value || interactionSource.collectIsDraggedAsState().value
    val scale by animateFloatAsState(if (active) 1.12f else 1f, label = "thumbScale")
    Box(
        Modifier
            .size(ThumbSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(3.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(3.dp, color, CircleShape),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderTrack(state: SliderState, color: Color) {
    val inactive = WellnessTheme.colors.trackStrong
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(8.dp),
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(inactive, cornerRadius = radius)
        val range = state.valueRange.endInclusive - state.valueRange.start
        val fraction = if (range > 0f) ((state.value - state.valueRange.start) / range).coerceIn(0f, 1f) else 0f
        if (fraction > 0f) {
            drawRoundRect(color, size = Size((size.width * fraction).coerceAtLeast(size.height), size.height), cornerRadius = radius)
        }
    }
}

private val ThumbSize = 24.dp
