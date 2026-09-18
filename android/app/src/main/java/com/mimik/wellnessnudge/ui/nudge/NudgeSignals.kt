package com.mimik.wellnessnudge.ui.nudge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.ui.components.SignalChip
import com.mimik.wellnessnudge.ui.format.formatPercent
import com.mimik.wellnessnudge.ui.format.formatSleep
import com.mimik.wellnessnudge.ui.format.formatSteps
import com.mimik.wellnessnudge.ui.theme.WellnessColors
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/** One reading the model was given, e.g. "HRV 45 ms", keyed by the metric it comes from. */
@Immutable
data class Signal(val kind: SignalKind, val label: String)

enum class SignalKind { Sleep, DeepSleep, Rem, RestingHr, Hrv, Steps, Goal }

/** The signals in [NudgeRequest], in the Today screen's order with the goal last. */
internal fun NudgeRequest.toSignals(): List<Signal> = buildList {
    sleepHours?.let { add(Signal(SignalKind.Sleep, "${formatSleep(it.toFloat())} sleep")) }
    deepSleepPct?.let { add(Signal(SignalKind.DeepSleep, "Deep ${formatPercent(it.toFloat())}")) }
    remSleepPct?.let { add(Signal(SignalKind.Rem, "REM ${formatPercent(it.toFloat())}")) }
    restingHR?.let { add(Signal(SignalKind.RestingHr, "Resting HR $it")) }
    hrvMs?.let { add(Signal(SignalKind.Hrv, "HRV $it ms")) }
    stepsYesterday?.let { add(Signal(SignalKind.Steps, "${formatSteps(it)} steps")) }
    userGoal?.trim()?.takeIf { it.isNotEmpty() }?.let { add(Signal(SignalKind.Goal, "Goal · $it")) }
}

/**
 * [signals] as chips in their metric colors. [appearance] gives how far the chip at an index
 * has faded in, from 0 to 1; it is read while drawing, so a cascade redraws without recomposing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SignalChips(
    signals: List<Signal>,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(ChipGap),
    appearance: ((index: Int) -> Float)? = null,
) {
    val colors = WellnessTheme.colors
    val rise = with(LocalDensity.current) { ChipRise.toPx() }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        signals.forEachIndexed { index, signal ->
            val chipModifier = if (appearance == null) {
                Modifier
            } else {
                Modifier.graphicsLayer {
                    val shown = appearance(index)
                    alpha = shown
                    translationY = rise * (1f - shown)
                }
            }
            val color = signal.kind.color(colors)
            if (signal.kind == SignalKind.Goal) {
                GoalChip(signal.label, color, chipModifier)
            } else {
                SignalChip(signal.label, color, chipModifier)
            }
        }
    }
}

/**
 * A [SignalChip] for the user's own words: a long goal wraps onto more lines instead of being
 * cut short, and the dot stays with the first line. On one line it matches [SignalChip].
 */
@Composable
private fun GoalChip(label: String, color: Color, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    val style = MaterialTheme.typography.labelMedium
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    Box(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(GoalChipShape)
            .background(colors.chipFill)
            .border(1.dp, colors.controlBorder, GoalChipShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row {
            Box(Modifier.height(lineHeight), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(color, CircleShape),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = style,
                color = colors.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun SignalKind.color(colors: WellnessColors): Color = when (this) {
    SignalKind.Sleep -> colors.sleep
    SignalKind.DeepSleep -> colors.deepSleep
    SignalKind.Rem -> colors.rem
    SignalKind.RestingHr -> colors.restingHr
    SignalKind.Hrv -> colors.hrv
    SignalKind.Steps -> colors.steps
    SignalKind.Goal -> colors.accent
}

private val ChipGap = 8.dp
private val ChipRise = 6.dp

// Half the height of a one-line chip, so it reads as a pill until it wraps.
private val GoalChipShape = RoundedCornerShape(16.dp)
