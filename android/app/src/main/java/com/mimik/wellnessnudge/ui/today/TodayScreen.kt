package com.mimik.wellnessnudge.ui.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.Eyebrow
import com.mimik.wellnessnudge.ui.components.GradientButton
import com.mimik.wellnessnudge.ui.components.IconBadge
import com.mimik.wellnessnudge.ui.components.MetricTile
import com.mimik.wellnessnudge.ui.components.NudgeOrb
import com.mimik.wellnessnudge.ui.components.OnDevicePill
import com.mimik.wellnessnudge.ui.components.RuntimeStatus
import com.mimik.wellnessnudge.ui.components.SectionHeader
import com.mimik.wellnessnudge.ui.components.SleepDuration
import com.mimik.wellnessnudge.ui.components.SleepStagesBar
import com.mimik.wellnessnudge.ui.components.SuggestionChip
import com.mimik.wellnessnudge.ui.components.TextAction
import com.mimik.wellnessnudge.ui.components.WellnessCard
import com.mimik.wellnessnudge.ui.format.LocalWellnessClock
import com.mimik.wellnessnudge.ui.format.formatSteps
import com.mimik.wellnessnudge.ui.format.fullDateLabel
import com.mimik.wellnessnudge.ui.format.greetingFor
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

/**
 * Today: last night's sleep, the body signals and a goal for the day, which the sticky
 * "Generate nudge" button turns into a nudge. Stateless; [TodayRoute] wires it to
 * [TodayViewModel]. Shows the editor sheet for [TodayUiState.editor].
 *
 * The content scrolls under a footer that fades it out above the button. [contentPadding]
 * is the room the floating tab bar takes at the bottom, or the keyboard while it is taller;
 * it is read during layout, so the footer rides the keyboard without recomposing.
 *
 * @param scrollState the screen's scroll position; hoisted so callers can move it.
 * @param goalFocusRequester moves focus to the goal input.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    onGoalChange: (String) -> Unit,
    onSampleDay: () -> Unit,
    onOpenEditor: (TodayEditor) -> Unit,
    onMetricChange: (Metric, Float) -> Unit,
    onCloseEditor: () -> Unit,
    onGenerate: () -> Unit,
    onOpenRuntime: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    goalFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    val focusManager = LocalFocusManager.current
    var goalFocused by remember { mutableStateOf(false) }

    // Every action away from the goal puts the keyboard away first.
    fun leaveGoal(then: () -> Unit) {
        focusManager.clearFocus()
        then()
    }

    // While the goal is typed, keep the end of the content (the field and its suggestions) in
    // view above the footer, following the keyboard as it opens and pads the bottom.
    LaunchedEffect(goalFocused, scrollState) {
        if (goalFocused) {
            snapshotFlow { scrollState.maxValue }.collectLatest { scrollState.animateScrollTo(it) }
        }
    }

    DaybreakBackground(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                // A tap on empty space puts the keyboard away.
                .pointerInput(focusManager) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .statusBarsPadding()
                .padding(top = HeaderTopPadding, bottom = FooterHeight),
        ) {
            Column(Modifier.padding(horizontal = WellnessSpacing.ScreenMargin)) {
                Header(runtime = state.runtime, onOpenRuntime = { leaveGoal(onOpenRuntime) })
                Spacer(Modifier.height(WellnessSpacing.SectionGap))
                SectionHeader(
                    title = "Last night",
                    action = {
                        TextAction(text = "Sample day", icon = Icons.Rounded.Shuffle, onClick = { leaveGoal(onSampleDay) })
                    },
                )
                Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
                SleepCard(metrics = state.metrics, onClick = { leaveGoal { onOpenEditor(TodayEditor.Sleep) } })
                Spacer(Modifier.height(WellnessSpacing.SectionGap))
                SectionHeader(title = "Body")
                Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
                BodyTiles(metrics = state.metrics, onOpenEditor = { editor -> leaveGoal { onOpenEditor(editor) } })
                Spacer(Modifier.height(WellnessSpacing.SectionGap))
                SectionHeader(title = "Today's focus")
                Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
                GoalField(
                    value = state.goal,
                    onValueChange = onGoalChange,
                    focusRequester = goalFocusRequester,
                    onFocusChange = { goalFocused = it },
                )
            }
            // The chips' 48 dp touch targets add 6 dp above the visible pills.
            Spacer(Modifier.height(WellnessSpacing.ItemGap - 6.dp))
            GoalSuggestions(goal = state.goal, onSelect = { suggestion -> leaveGoal { onGoalChange(suggestion) } })
        }
        StatusBarScrim()
        GenerateFooter(
            onGenerate = { leaveGoal(onGenerate) },
            contentPadding = contentPadding,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    state.editor?.let { editor ->
        TodayEditorSheet(
            editor = editor,
            metrics = state.metrics,
            onMetricChange = onMetricChange,
            onDismiss = onCloseEditor,
        )
    }
}

/** The date and the On-device pill on one line, then the greeting with the orb rising after it. */
@Composable
private fun Header(runtime: RuntimeStatus, onOpenRuntime: () -> Unit) {
    val clock = LocalWellnessClock.current
    val now = clock.now()
    Column {
        // As tall as the visible pill; its 48 dp touch target overhangs the row.
        Row(Modifier.height(PillHeight), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(text = fullDateLabel(now, clock), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            OnDevicePill(status = runtime, onClick = onOpenRuntime)
        }
        Spacer(Modifier.height(4.dp))
        Greeting(greetingFor(clock.dateTime(now).hour))
    }
}

/**
 * The greeting in the display serif with a small idle orb set inline after the last word,
 * so the orb follows the text when a large font wraps it.
 */
@Composable
private fun Greeting(text: String) {
    val density = LocalDensity.current
    // Sized in dp: the orb keeps its size whatever the font scale.
    val orb = remember(density) {
        with(density) {
            InlineTextContent(
                Placeholder(
                    width = (OrbGap + OrbSize).toSp(),
                    height = OrbSize.toSp(),
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    NudgeOrb(size = OrbSize)
                }
            }
        }
    }
    Text(
        text = buildAnnotatedString {
            append(text)
            appendInlineContent(OrbId)
        },
        modifier = Modifier.clearAndSetSemantics {
            heading()
            contentDescription = text
        },
        style = MaterialTheme.typography.displayLarge,
        color = WellnessTheme.colors.textPrimary,
        inlineContent = mapOf(OrbId to orb),
    )
}

@Composable
private fun SleepCard(metrics: DayMetrics, onClick: () -> Unit) {
    val colors = WellnessTheme.colors
    val hours by animateFloatAsState(metrics.sleepHours, valueChange(), label = "sleepHours")
    WellnessCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, onClickLabel = "Edit sleep") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Bedtime, colors.sleep)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Sleep",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.height(16.dp))
        // Spoken in words: TalkBack would read the "h" and "m" of "6h 30m" as letters.
        Row(Modifier.clearAndSetSemantics { contentDescription = spokenSleep(metrics.sleepHours) }) {
            SleepDuration(hours = hours, modifier = Modifier.alignByBaseline())
            Spacer(Modifier.width(8.dp))
            Text(
                text = "asleep",
                modifier = Modifier.alignByBaseline(),
                style = WellnessTheme.type.metricUnit,
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(18.dp))
        SleepStagesBar(deepPct = metrics.deepSleepPct, remPct = metrics.remSleepPct)
    }
}

/** Resting HR and HRV side by side, then yesterday's steps across the full width. */
@Composable
private fun BodyTiles(metrics: DayMetrics, onOpenEditor: (TodayEditor) -> Unit) {
    val colors = WellnessTheme.colors
    val restingHr by animateFloatAsState(metrics.restingHr, valueChange(), label = "restingHr")
    val hrv by animateFloatAsState(metrics.hrvMs, valueChange(), label = "hrv")
    val steps by animateFloatAsState(metrics.steps, valueChange(), label = "steps")
    Column(verticalArrangement = Arrangement.spacedBy(WellnessSpacing.ItemGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(WellnessSpacing.ItemGap)) {
            MetricTile(
                icon = Icons.Rounded.Favorite,
                label = "Resting HR",
                value = restingHr.roundToInt().toString(),
                unit = "bpm",
                color = colors.restingHr,
                modifier = Modifier.weight(1f),
                onClick = { onOpenEditor(TodayEditor.RestingHr) },
                meterProgress = Metric.RestingHr.fraction(metrics.restingHr),
            )
            MetricTile(
                icon = Icons.Rounded.MonitorHeart,
                label = "HRV",
                value = hrv.roundToInt().toString(),
                unit = "ms",
                color = colors.hrv,
                modifier = Modifier.weight(1f),
                onClick = { onOpenEditor(TodayEditor.Hrv) },
                meterProgress = Metric.Hrv.fraction(metrics.hrvMs),
            )
        }
        MetricTile(
            icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
            label = "Steps yesterday",
            value = formatSteps(steps.roundToInt()),
            unit = null,
            color = colors.steps,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onOpenEditor(TodayEditor.Steps) },
            meterProgress = metrics.steps / StepsGoal,
            meterCaption = "of 10k",
            shape = WellnessShapes.Card,
        )
    }
}

/** Goals to start from, in a row that scrolls edge to edge; one is selected while it is the goal. */
@Composable
private fun GoalSuggestions(goal: String, onSelect: (String) -> Unit) {
    val current = goal.trim()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Suggestions.forEach { suggestion ->
            SuggestionChip(text = suggestion, selected = suggestion == current, onClick = { onSelect(suggestion) })
        }
    }
}

/**
 * The sticky call to action. A scrim of the canvas color fades in above the button and
 * stays solid down behind the tab bar (or the keyboard), so content scrolls away cleanly.
 */
@Composable
private fun GenerateFooter(
    onGenerate: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val bg = WellnessTheme.colors.bg
    Box(
        modifier
            .fillMaxWidth()
            .drawWithCache {
                // Eased in and out (smoothstep), so the fade has no visible start or end.
                val scrim = Brush.verticalGradient(
                    0f to bg.copy(alpha = 0f),
                    0.25f to bg.copy(alpha = 0.16f),
                    0.5f to bg.copy(alpha = 0.5f),
                    0.75f to bg.copy(alpha = 0.84f),
                    1f to bg,
                    startY = 0f,
                    endY = FooterFade.toPx(),
                )
                onDrawBehind { drawRect(scrim) }
            }
            .padding(contentPadding)
            .padding(
                start = WellnessSpacing.ScreenMargin,
                end = WellnessSpacing.ScreenMargin,
                top = FooterFade,
                bottom = FooterGap,
            ),
    ) {
        GradientButton(text = "Generate nudge", onClick = onGenerate, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * The top of the Daybreak canvas redrawn over the status bar, fading out at its lower edge,
 * so content scrolled up passes cleanly under the clock. Invisible at rest.
 */
@Composable
private fun StatusBarScrim() {
    DaybreakBackground(
        Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                val fadeFrom = ((size.height - StatusBarFade.toPx()) / size.height).coerceIn(0f, 1f)
                val mask = Brush.verticalGradient(0f to Color.Black, fadeFrom to Color.Black, 1f to Color.Transparent)
                onDrawWithContent {
                    drawContent()
                    drawRect(mask, blendMode = BlendMode.DstIn)
                }
            },
    ) {}
}

private fun valueChange() = tween<Float>(WellnessMotion.ValueMillis, easing = WellnessMotion.Easing)

/** 6.5 → "6 hours 30 minutes asleep", as TalkBack reads the sleep card. */
internal fun spokenSleep(hours: Float): String {
    val minutes = (hours * 60).roundToInt().coerceAtLeast(0)
    val h = minutes / 60
    val m = minutes % 60
    fun count(n: Int, unit: String) = if (n == 1) "1 $unit" else "$n ${unit}s"
    val parts = buildList {
        if (h > 0) add(count(h, "hour"))
        if (m > 0 || h == 0) add(count(m, "minute"))
    }
    return parts.joinToString(" ") + " asleep"
}

private val Suggestions = listOf(
    "Sleep better tonight",
    "Lower stress",
    "More energy",
    "Recover from training",
    "Move more today",
)

private const val StepsGoal = 10_000f
private const val OrbId = "orb"
private val OrbSize = 22.dp
private val OrbGap = 10.dp
private val HeaderTopPadding = 12.dp

// OnDevicePill's visible height, inside its 48 dp touch target.
private val PillHeight = 36.dp
private val StatusBarFade = 16.dp

// The footer: a fade above the 60 dp button, and a gap between the button and the tab bar.
private val FooterFade = 44.dp
private val FooterGap = 16.dp
private val GenerateButtonHeight = 60.dp

// Room at the end of the content. The last suggestions stop in the top quarter of the fade,
// where it is still too faint to see (the chips' touch targets reach lower than the pills).
private val FooterHeight = FooterFade * 0.75f + GenerateButtonHeight + FooterGap
