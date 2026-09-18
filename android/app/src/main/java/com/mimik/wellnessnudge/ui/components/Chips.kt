package com.mimik.wellnessnudge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.format.CategoryStyle
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay

/**
 * Pill for one choice in a group: goal suggestions on Today, filters in the Journal. The
 * visible pill is 36 dp tall inside a 48 dp touch target, like Material chips, so rows
 * of chips need no extra vertical spacing. It is a control, so on paper it is a white pill
 * with a soft shadow, like the buttons (read-only chips such as [SignalChip] stay a flat
 * wash). Selected, it takes an accent tint, border and a brighter label; [checkWhenSelected]
 * also leads the label with a check mark (Journal filters), so the choice doesn't rest on
 * color alone.
 */
@Composable
fun SuggestionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    checkWhenSelected: Boolean = false,
) {
    val colors = WellnessTheme.colors
    val spec = tween<Color>(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing)
    // On paper the tint is laid over the white pill, so the shadow doesn't show through it.
    val selectedFill = if (colors.isDark) colors.tint(colors.accent) else colors.tint(colors.accent).compositeOver(colors.controlFill)
    val container by animateColorAsState(if (selected) selectedFill else colors.controlFill, spec, "chipBg")
    val border by animateColorAsState(if (selected) colors.accent else colors.controlBorder, spec, "chipBorder")
    val label by animateColorAsState(if (selected) colors.accentContent else colors.textSecondary, spec, "chipLabel")
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 36.dp)
            .paperShadow(colors, WellnessShapes.Pill, elevation = 3.dp)
            .clip(WellnessShapes.Pill)
            .background(container)
            .border(1.dp, border, WellnessShapes.Pill)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .animateContentSize(tween(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing))
            .padding(start = if (selected && checkWhenSelected) 10.dp else 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected && checkWhenSelected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = label, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Pill naming a nudge's category in its color, e.g. "Sleep" with a moon. */
@Composable
fun CategoryBadge(
    category: String?,
    modifier: Modifier = Modifier,
) {
    val style = CategoryStyle.of(category)
    Row(
        modifier = modifier
            .clip(WellnessShapes.Pill)
            .background(WellnessTheme.colors.tint(style.color))
            .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(style.icon, contentDescription = null, tint = style.contentColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = style.label, style = MaterialTheme.typography.labelMedium, color = style.contentColor)
    }
}

/** A signal the model read, e.g. "HRV 45 ms", keyed by a dot in the metric's color. */
@Composable
fun SignalChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(WellnessShapes.Pill)
            .background(colors.chipFill)
            .border(1.dp, colors.controlBorder, WellnessShapes.Pill)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Icon in a circle of its own tint, e.g. a category or metric marker. The glyph is ~55% of
 * [size]; on paper it is deepened toward ink so light hues (honey, mint) keep 3:1.
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    val colors = WellnessTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .background(colors.tint(tint), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (colors.isDark) tint else lerp(tint, colors.textPrimary, 0.3f),
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/**
 * Status light. [pulsing] adds a soft glow that breathes outwards, e.g. for a live runtime;
 * still frames (previews, animations off) show the glow at rest. The glow spills past
 * [size] without taking layout space. The slow pulse runs at about 30 frames a second, so
 * the display can lower its refresh rate, and rests while the dot is scrolled out of view.
 */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
    size: Dp = 8.dp,
) {
    var onScreen by remember { mutableStateOf(true) }
    val animate = pulsing && onScreen && rememberAnimationsEnabled()
    val pulse = remember { mutableFloatStateOf(-1f) }
    if (animate) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it }
            while (true) {
                delay(SlowFrameMillis)
                withFrameMillis { now -> pulse.floatValue = ((now - start) % PulseMillis).toFloat() / PulseMillis }
            }
        }
    }
    // Its own layer, so each pulse frame redraws only the dot.
    Canvas(
        modifier
            .size(size)
            .onScreenChanged { onScreen = it }
            .graphicsLayer(),
    ) {
        val radius = this.size.minDimension / 2f
        if (pulsing) {
            val progress = pulse.floatValue.takeIf { animate && it >= 0f }
            // Moving: a glow that grows and fades. At rest: a soft halo twice the dot's size.
            val glowRadius = if (progress != null) radius * (1.2f + 1.3f * progress) else radius * 2f
            val alpha = if (progress != null) 0.5f * (1f - progress) else 0.3f
            drawCircle(
                brush = Brush.radialGradient(
                    0f to color.copy(alpha = alpha),
                    0.45f to color.copy(alpha = alpha),
                    1f to color.copy(alpha = 0f),
                    center = center,
                    radius = glowRadius,
                ),
                radius = glowRadius,
            )
        }
        drawCircle(color = color, radius = radius)
    }
}

/** Health of the on-device stack, as shown by [OnDevicePill]. */
enum class RuntimeStatus { Ready, Starting, Error }

/**
 * Header pill saying the AI runs on this phone; opens the runtime details when tapped. The
 * label and marker change with [status] ("On-device" with a breathing dot, "Starting…" with
 * a spinner, "Unavailable" with a red dot), so the state never rests on color alone.
 */
@Composable
fun OnDevicePill(
    status: RuntimeStatus,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    val (label, state) = when (status) {
        RuntimeStatus.Ready -> "On-device" to "ready"
        RuntimeStatus.Starting -> "Starting…" to "starting"
        RuntimeStatus.Error -> "Unavailable" to "unavailable"
    }
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .heightIn(min = 36.dp)
            .control(colors, WellnessShapes.Pill)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = "Show what runs on this phone", role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) { contentDescription = "On-device AI, $state" }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (status) {
            RuntimeStatus.Ready -> StatusDot(color = colors.success, pulsing = true)
            RuntimeStatus.Starting -> Spinner(color = colors.warning, size = 12.dp, strokeWidth = 1.5.dp)
            RuntimeStatus.Error -> StatusDot(color = colors.danger)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val PulseMillis = 1_800L
