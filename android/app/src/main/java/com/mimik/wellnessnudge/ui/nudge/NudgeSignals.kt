package com.mimik.wellnessnudge.ui.nudge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.ui.components.SignalChip
import com.mimik.wellnessnudge.ui.format.formatPercent
import com.mimik.wellnessnudge.ui.format.formatSleep
import com.mimik.wellnessnudge.ui.format.formatSteps
import com.mimik.wellnessnudge.ui.theme.WellnessColors
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.today.spokenDuration
import kotlin.math.ceil

/**
 * One reading the model was given, e.g. "HRV 45 ms", keyed by the metric it comes from.
 * [spoken] is how TalkBack reads it ("heart rate variability 45 milliseconds").
 */
@Immutable
data class Signal(val kind: SignalKind, val label: String, val spoken: String = label)

enum class SignalKind { Sleep, DeepSleep, Rem, RestingHr, Hrv, Steps, Goal }

/** The signals in [NudgeRequest], in the Today screen's order with the goal last. */
internal fun NudgeRequest.toSignals(): List<Signal> = buildList {
    sleepHours?.let {
        add(Signal(SignalKind.Sleep, "${formatSleep(it.toFloat())} sleep", "${spokenDuration(it.toFloat())} of sleep"))
    }
    deepSleepPct?.let { add(Signal(SignalKind.DeepSleep, "Deep ${formatPercent(it.toFloat())}", "deep sleep ${formatPercent(it.toFloat())}")) }
    remSleepPct?.let { add(Signal(SignalKind.Rem, "REM ${formatPercent(it.toFloat())}", "REM sleep ${formatPercent(it.toFloat())}")) }
    restingHR?.let { add(Signal(SignalKind.RestingHr, "Resting HR $it", "resting heart rate $it")) }
    hrvMs?.let { add(Signal(SignalKind.Hrv, "HRV $it ms", "heart rate variability $it milliseconds")) }
    stepsYesterday?.let { add(Signal(SignalKind.Steps, "${formatSteps(it)} steps")) }
    userGoal?.trim()?.takeIf { it.isNotEmpty() }?.let { add(Signal(SignalKind.Goal, "Goal · $it", "goal: $it")) }
}

/** True when the request carries no signal at all, so there is nothing to generate from again. */
internal val NudgeRequest.isEmpty: Boolean
    get() = toSignals().isEmpty()

/**
 * [signals] as chips in their metric colors, the goal marked with a flag. [appearance] gives
 * how far the chip at an index has faded in, from 0 to 1; it is read while drawing, so a
 * cascade redraws without recomposing. [centered]: the chips are centered (the thinking
 * stage), and so are the lines of a goal that wraps.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SignalChips(
    signals: List<Signal>,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    appearance: ((index: Int) -> Float)? = null,
) {
    val colors = WellnessTheme.colors
    val rise = with(LocalDensity.current) { ChipRise.toPx() }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = if (centered) Arrangement.spacedBy(ChipGap, Alignment.CenterHorizontally) else Arrangement.spacedBy(ChipGap),
        verticalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        signals.forEachIndexed { index, signal ->
            val shown = if (appearance == null) {
                Modifier
            } else {
                Modifier.graphicsLayer {
                    val progress = appearance(index)
                    alpha = progress
                    translationY = rise * (1f - progress)
                }
            }
            val chipModifier = shown.clearAndSetSemantics { contentDescription = signal.spoken }
            if (signal.kind == SignalKind.Goal) {
                GoalChip(signal.label, centered, chipModifier)
            } else {
                SignalChip(signal.label, signal.kind.color(colors), chipModifier)
            }
        }
    }
}

/**
 * A chip for the user's own words, led by the flag the goal field on Today shows, rather than
 * a dot that could pass for a metric. The flag is set in the text, so it stays at the start of
 * the first line. A long goal wraps onto balanced lines instead of being cut short, and the
 * chip hugs its widest line; on one line it matches [SignalChip].
 */
@Composable
private fun GoalChip(label: String, centered: Boolean, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    val density = LocalDensity.current
    val style = MaterialTheme.typography.labelMedium.copy(lineBreak = LineBreak.Heading)
    val text = remember(label) {
        buildAnnotatedString {
            appendInlineContent(FlagId, "\u2691")
            append(label)
        }
    }
    val flag = remember(density, colors) {
        with(density) {
            // Sized in dp, as the dots of the other chips: the flag doesn't grow with the text.
            val placeholder = Placeholder(
                width = (FlagSize + FlagGap).toSp(),
                height = FlagSize.toSp(),
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            )
            placeholder to InlineTextContent(placeholder) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(FlagSize))
                }
            }
        }
    }
    Box(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(GoalChipShape)
            .background(colors.chipFill)
            .border(1.dp, colors.controlBorder, GoalChipShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        ShrinkWrapText(
            text = text,
            style = style,
            color = colors.textPrimary,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            placeholder = flag.first,
            inlineContent = mapOf(FlagId to flag.second),
        )
    }
}

/**
 * Text as wide as its widest line rather than as the space it may take, so a wrapped text in
 * a pill leaves no empty band at its end. The lines break as they would at the full width.
 * [placeholder] is the size of the inline content that leads [text].
 */
@Composable
private fun ShrinkWrapText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign,
    placeholder: Placeholder,
    inlineContent: Map<String, InlineTextContent>,
) {
    val measurer = rememberTextMeasurer()
    Layout(
        content = {
            Text(
                text = text,
                style = style,
                color = color,
                textAlign = textAlign,
                maxLines = GoalMaxLines,
                overflow = TextOverflow.Ellipsis,
                inlineContent = inlineContent,
            )
        },
    ) { measurables, constraints ->
        val probe = measurer.measure(
            text,
            style,
            overflow = TextOverflow.Ellipsis,
            maxLines = GoalMaxLines,
            placeholders = listOf(AnnotatedString.Range(placeholder, 0, 1)),
            constraints = Constraints(maxWidth = constraints.maxWidth),
        )
        val widest = (0 until probe.lineCount).maxOfOrNull { probe.getLineRight(it) - probe.getLineLeft(it) } ?: 0f
        val width = ceil(widest).toInt().coerceIn(constraints.minWidth, constraints.maxWidth)
        val placeable = measurables.first().measure(Constraints.fixedWidth(width))
        layout(width, placeable.height) { placeable.place(0, 0) }
    }
}

private fun SignalKind.color(colors: WellnessColors): Color = when (this) {
    SignalKind.Sleep -> colors.sleep
    SignalKind.DeepSleep -> colors.deepSleep
    SignalKind.Rem -> colors.rem
    SignalKind.RestingHr -> colors.restingHr
    SignalKind.Hrv -> colors.hrv
    SignalKind.Steps -> colors.steps
    SignalKind.Goal -> colors.textSecondary
}

private val ChipGap = 8.dp
private val ChipRise = 6.dp
private const val GoalMaxLines = 3
private const val FlagId = "flag"
private val FlagSize = 12.dp
private val FlagGap = 6.dp

// Half the height of a one-line chip, so it reads as a pill until it wraps.
private val GoalChipShape = RoundedCornerShape(16.dp)
