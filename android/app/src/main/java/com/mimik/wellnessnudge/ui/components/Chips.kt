package com.mimik.wellnessnudge.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.format.CategoryStyle
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.theme.rememberAnimationsEnabled

/**
 * 36 dp pill for one choice in a group: goal suggestions on Today, filters in the Journal.
 * Selected, it takes an accent tint, border and label.
 */
@Composable
fun SuggestionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    val spec = tween<Color>(WellnessMotion.SmallMillis)
    val container by animateColorAsState(if (selected) colors.tint(colors.accent) else colors.surfaceRaised, spec, "chipBg")
    val border by animateColorAsState(if (selected) colors.accent else colors.hairline, spec, "chipBorder")
    val label by animateColorAsState(if (selected) colors.accent else colors.textPrimary, spec, "chipLabel")
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(WellnessShapes.Pill)
            .background(container)
            .border(1.dp, border, WellnessShapes.Pill)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = label, maxLines = 1)
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
            .height(32.dp)
            .clip(WellnessShapes.Pill)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.hairline, WellnessShapes.Pill)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.textPrimary, maxLines = 1)
    }
}

/** Icon in a circle of its own tint, e.g. a category or metric marker. The glyph is ~55% of [size]. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(WellnessTheme.colors.tint(tint), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** Status light. [pulsing] adds a soft ring that radiates outwards, e.g. for a live runtime. */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
    size: Dp = 8.dp,
) {
    val animate = pulsing && rememberAnimationsEnabled()
    val pulse = if (animate) {
        rememberInfiniteTransition(label = "statusPulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
            label = "statusPulseProgress",
        )
    } else {
        null
    }
    Canvas(modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        if (pulsing) {
            // Still pose: the ring halfway out.
            val progress = pulse?.value ?: 0.5f
            drawCircle(
                color = color.copy(alpha = 0.45f * (1f - progress)),
                radius = radius * (1f + 1.25f * progress),
            )
        }
        drawCircle(color = color, radius = radius)
    }
}

/** Health of the on-device stack, as shown by [OnDevicePill]. */
enum class RuntimeStatus { Ready, Starting, Error }

/** Header pill saying the AI runs on this phone; opens the runtime details when tapped. */
@Composable
fun OnDevicePill(
    status: RuntimeStatus,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    val (dot, state) = when (status) {
        RuntimeStatus.Ready -> colors.success to "ready"
        RuntimeStatus.Starting -> colors.warning to "starting"
        RuntimeStatus.Error -> colors.danger to "unavailable"
    }
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(WellnessShapes.Pill)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.hairline, WellnessShapes.Pill)
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
        StatusDot(color = dot, pulsing = status == RuntimeStatus.Ready)
        Spacer(Modifier.width(8.dp))
        Text(text = "On-device", style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
    }
}
