package com.mimik.wellnessnudge.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimik.wellnessnudge.ui.components.CircleIconButton
import com.mimik.wellnessnudge.ui.components.IconBadge
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.components.WellnessBottomSheet
import com.mimik.wellnessnudge.ui.components.WellnessSlider
import com.mimik.wellnessnudge.ui.format.formatPercent
import com.mimik.wellnessnudge.ui.format.formatSleep
import com.mimik.wellnessnudge.ui.format.formatSteps
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.theme.tabular
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The sheet editing [editor]'s signals. Changes apply as they happen, through
 * [onMetricChange]; Done slides the sheet away, then calls [onDismiss], as a swipe does.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TodayEditorSheet(
    editor: TodayEditor,
    metrics: DayMetrics,
    onMetricChange: (Metric, Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    WellnessBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        TodayEditorContent(
            editor = editor,
            metrics = metrics,
            onMetricChange = onMetricChange,
            onDone = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) onDismiss()
                }
            },
        )
    }
}

/** What the sheet holds for [editor]. Snapshot tests draw it inside a WellnessBottomSheetFrame. */
@Composable
internal fun TodayEditorContent(
    editor: TodayEditor,
    metrics: DayMetrics,
    onMetricChange: (Metric, Float) -> Unit,
    onDone: () -> Unit,
) {
    val colors = WellnessTheme.colors
    when (editor) {
        TodayEditor.Sleep -> SleepEditor(metrics, onMetricChange, onDone)
        TodayEditor.RestingHr -> MetricEditor(
            metric = Metric.RestingHr,
            value = metrics.restingHr,
            icon = Icons.Rounded.Favorite,
            color = colors.restingHr,
            title = "Resting HR",
            name = "resting heart rate",
            unit = "bpm",
            description = "Beats per minute at complete rest.",
            format = { it.roundToInt().toString() },
            onMetricChange = onMetricChange,
            onDone = onDone,
        )
        TodayEditor.Hrv -> MetricEditor(
            metric = Metric.Hrv,
            value = metrics.hrvMs,
            icon = Icons.Rounded.MonitorHeart,
            color = colors.hrv,
            title = "HRV",
            name = "heart rate variability",
            unit = "ms",
            description = "Heart rate variability (RMSSD), in milliseconds.",
            format = { it.roundToInt().toString() },
            onMetricChange = onMetricChange,
            onDone = onDone,
        )
        TodayEditor.Steps -> MetricEditor(
            metric = Metric.Steps,
            value = metrics.steps,
            icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
            color = colors.steps,
            title = "Steps yesterday",
            name = "steps",
            unit = "steps",
            description = "Total steps you took yesterday.",
            format = { formatSteps(it.roundToInt()) },
            onMetricChange = onMetricChange,
            onDone = onDone,
        )
    }
}

/** Total sleep and the two stages, each a labeled slider with −/+ buttons. */
@Composable
private fun SleepEditor(
    metrics: DayMetrics,
    onMetricChange: (Metric, Float) -> Unit,
    onDone: () -> Unit,
) {
    val colors = WellnessTheme.colors
    EditorColumn {
        EditorTitle(icon = Icons.Rounded.Bedtime, color = colors.sleep, title = "Last night’s sleep")
        Spacer(Modifier.height(28.dp))
        SliderRow(
            label = "Total sleep",
            value = formatSleep(metrics.sleepHours),
            // TalkBack would read the "h" and "m" of "6h 30m" as letters.
            spokenValue = spokenDuration(metrics.sleepHours),
            metric = Metric.SleepHours,
            current = metrics.sleepHours,
            color = colors.sleep,
            name = "total sleep",
            onMetricChange = onMetricChange,
        )
        Spacer(Modifier.height(24.dp))
        SliderRow(
            label = "Deep sleep",
            value = formatPercent(metrics.deepSleepPct),
            metric = Metric.DeepSleep,
            current = metrics.deepSleepPct,
            color = colors.deepSleep,
            name = "deep sleep",
            onMetricChange = onMetricChange,
        )
        Spacer(Modifier.height(24.dp))
        SliderRow(
            label = "REM",
            value = formatPercent(metrics.remSleepPct),
            metric = Metric.RemSleep,
            current = metrics.remSleepPct,
            color = colors.rem,
            name = "REM sleep",
            onMetricChange = onMetricChange,
        )
        Spacer(Modifier.height(32.dp))
        SecondaryButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

/** One body metric: its value large and centered, what it measures, and a slider. */
@Composable
private fun MetricEditor(
    metric: Metric,
    value: Float,
    icon: ImageVector,
    color: Color,
    title: String,
    name: String,
    unit: String,
    description: String,
    format: (Float) -> String,
    onMetricChange: (Metric, Float) -> Unit,
    onDone: () -> Unit,
) {
    val colors = WellnessTheme.colors
    val shown = format(value)
    EditorColumn {
        EditorTitle(icon = icon, color = color, title = title)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.align(Alignment.CenterHorizontally)) {
            Text(
                text = shown,
                modifier = Modifier.alignByBaseline(),
                style = WellnessTheme.type.metricXL,
                color = colors.textPrimary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = unit,
                modifier = Modifier.alignByBaseline(),
                // Grown with the value, in the tiles' proportion of value to unit (32:14).
                style = WellnessTheme.type.metricUnit.copy(fontSize = 20.sp, lineHeight = 24.sp),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        EditorSlider(
            metric = metric,
            current = value,
            color = color,
            name = name,
            valueDescription = "$shown $unit",
            onMetricChange = onMetricChange,
        )
        Spacer(Modifier.height(32.dp))
        SecondaryButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

/** The editor's content, scrolling when the sheet is shorter than it, e.g. in landscape. */
@Composable
private fun EditorColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(bottom = 16.dp),
        content = content,
    )
}

@Composable
private fun EditorTitle(icon: ImageVector, color: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, tint = color, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = WellnessTheme.colors.textPrimary,
        )
    }
}

/** A label with the current value at its end, over an [EditorSlider]; TalkBack reads [spokenValue]. */
@Composable
private fun SliderRow(
    label: String,
    value: String,
    metric: Metric,
    current: Float,
    color: Color,
    name: String,
    onMetricChange: (Metric, Float) -> Unit,
    spokenValue: String = value,
) {
    val colors = WellnessTheme.colors
    Column {
        Row {
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textSecondary,
            )
            Text(
                text = value,
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.titleMedium.tabular(),
                color = colors.textPrimary,
            )
        }
        Spacer(Modifier.height(10.dp))
        EditorSlider(
            metric = metric,
            current = current,
            color = color,
            name = name,
            valueDescription = spokenValue,
            onMetricChange = onMetricChange,
        )
    }
}

/**
 * [metric]'s slider between a − and a + button, which step to the neighboring slider stop.
 * TalkBack reads [name] and [valueDescription] for the slider.
 */
@Composable
private fun EditorSlider(
    metric: Metric,
    current: Float,
    color: Color,
    name: String,
    valueDescription: String,
    onMetricChange: (Metric, Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircleIconButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "Decrease $name",
            onClick = { onMetricChange(metric, metric.stepDown(current)) },
            enabled = current > metric.range.start,
        )
        Spacer(Modifier.width(16.dp))
        WellnessSlider(
            value = current,
            onValueChange = { onMetricChange(metric, it) },
            valueRange = metric.range,
            color = color,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = name },
            steps = metric.sliderSteps,
            valueDescription = valueDescription,
        )
        Spacer(Modifier.width(16.dp))
        CircleIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Increase $name",
            onClick = { onMetricChange(metric, metric.stepUp(current)) },
            enabled = current < metric.range.endInclusive,
        )
    }
}
