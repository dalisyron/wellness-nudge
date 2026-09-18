package com.mimik.wellnessnudge.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.Eyebrow
import com.mimik.wellnessnudge.ui.components.GradientButton
import com.mimik.wellnessnudge.ui.components.IconBadge
import com.mimik.wellnessnudge.ui.components.LinearMeter
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
import com.mimik.wellnessnudge.ui.components.horizontalScrollFades
import com.mimik.wellnessnudge.ui.components.metricTileLabelWidth
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
 * it is read during layout, so the footer rides the keyboard without recomposing. The goal
 * comes last while everything fits above the footer, and first, under the header, when it
 * doesn't (see [TodayFlow]).
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
    val unavailable = state.runtime == RuntimeStatus.Error

    // Every action away from the goal puts the keyboard away first.
    fun leaveGoal(then: () -> Unit) {
        focusManager.clearFocus()
        then()
    }

    DaybreakBackground(modifier) {
        val statusBars = WindowInsets.statusBars
        val ime = WindowInsets.ime
        val order = remember { FlowOrder() }

        // While the goal is typed at the end of the content, keep it and its suggestions in
        // view above the footer, following the keyboard as it opens and pads the bottom.
        // First, under the header, the goal is in view already.
        LaunchedEffect(goalFocused, scrollState) {
            if (goalFocused && !order.goalFirst) {
                snapshotFlow { scrollState.maxValue }.collectLatest { scrollState.animateScrollTo(it) }
            }
        }

        TodayFlow(
            order = order,
            // Above the footer's fade at rest: the screen less the status bar, the header's
            // top padding, the footer and the tab bar under it.
            room = { viewport ->
                viewport - statusBars.getTop(this) - HeaderTopPadding.roundToPx() - FooterReserve.roundToPx() -
                    contentPadding.calculateTopPadding().roundToPx() - contentPadding.calculateBottomPadding().roundToPx()
            },
            // While the goal is typed, and until the keyboard is gone, keep the order: the
            // keyboard takes its room only for a while.
            hold = { goalFocused || ime.getBottom(this) > 0 },
            // Room under the content for the footer. At the end of the page the last suggestions
            // may stop in the fade's faint top quarter, a last card (the goal first) above the
            // fade. With the notice showing, the content stops above the notice, and above the
            // fade it draws over content running under the footer.
            endSpace = { goalFirst ->
                when {
                    !unavailable -> if (goalFirst) FooterReserve else FooterHeight
                    goalFirst -> FooterReserve + FooterFade
                    else -> FooterReserve
                }
            },
            header = {
                Header(
                    runtime = state.runtime,
                    onOpenRuntime = { leaveGoal(onOpenRuntime) },
                    modifier = Modifier.padding(horizontal = WellnessSpacing.ScreenMargin),
                )
            },
            signals = {
                Column(Modifier.padding(horizontal = WellnessSpacing.ScreenMargin)) {
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
                    BodySection(metrics = state.metrics, onOpenEditor = { editor -> leaveGoal { onOpenEditor(editor) } })
                }
            },
            goal = {
                Column {
                    Column(Modifier.padding(horizontal = WellnessSpacing.ScreenMargin)) {
                        SectionHeader(title = "Today’s focus")
                        Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
                        GoalField(
                            value = state.goal,
                            onValueChange = onGoalChange,
                            focusRequester = goalFocusRequester,
                            onFocusChange = { goalFocused = it },
                        )
                    }
                    Spacer(Modifier.height(WellnessSpacing.ItemGap - ChipTargetInset))
                    GoalSuggestions(goal = state.goal, onSelect = { suggestion -> leaveGoal { onGoalChange(suggestion) } })
                }
            },
            modifier = Modifier
                .fillMaxSize()
                // A tap on empty space puts the keyboard away.
                .pointerInput(focusManager) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .statusBarsPadding()
                // Clear of a side navigation bar and the camera cutout in landscape.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(top = HeaderTopPadding),
        )
        StatusBarScrim()
        GenerateFooter(
            unavailable = unavailable,
            overContent = { order.goalFirst },
            onGenerate = { leaveGoal(onGenerate) },
            onOpenRuntime = { leaveGoal(onOpenRuntime) },
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

/**
 * Where [TodayFlow] put the goal, decided in layout. The screen reads it when the goal takes
 * focus, and the footer while drawing: with the goal first, content runs on under the footer.
 */
@Stable
private class FlowOrder {
    var goalFirst by mutableStateOf(false)
}

/**
 * The header, then the night's signals and the day's goal, scrolling in [modifier]. The goal
 * comes last, as the flow reads, while all of it, down to the suggestions, fits above the
 * footer's fade at rest ([room] for the viewport's height, give or take the fade's clear top).
 * When it doesn't (a shorter phone, larger text, landscape), the goal moves up under the
 * header: it stays in view, and a signal's card, not a lone section label, runs under the fade.
 * The order is kept while [hold] (the keyboard is up), and only the placement changes, so the
 * goal field keeps its text and focus when the order flips. [endSpace] follows the content
 * for the footer.
 */
@Composable
private fun TodayFlow(
    order: FlowOrder,
    room: Density.(viewport: Int) -> Int,
    hold: Density.() -> Boolean,
    endSpace: (goalFirst: Boolean) -> Dp,
    header: @Composable () -> Unit,
    signals: @Composable () -> Unit,
    goal: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The viewport's height, as it reaches the scroll container. Written and read in the same
    // measure pass of this node, so it needn't be state.
    val viewport = remember { IntArray(1) }
    val viewportModifier = Modifier.layout { measurable, constraints ->
        viewport[0] = constraints.maxHeight
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    Layout(contents = listOf(header, signals, goal), modifier = viewportModifier.then(modifier)) { (headerSlot, signalsSlot, goalSlot), constraints ->
        val blockConstraints = constraints.copy(minWidth = constraints.maxWidth, minHeight = 0)
        val headerBlock = headerSlot.single().measure(blockConstraints)
        val signalsBlock = signalsSlot.single().measure(blockConstraints)
        val goalBlock = goalSlot.single().measure(blockConstraints)
        val sectionGap = WellnessSpacing.SectionGap.roundToPx()
        // The suggestions' touch targets reach below the chips; the flow ends at the chips.
        val chipInset = ChipTargetInset.roundToPx()
        var goalFirst = Snapshot.withoutReadObservation { order.goalFirst }
        if (!hold()) {
            val flow = headerBlock.height + sectionGap + signalsBlock.height + sectionGap + goalBlock.height - chipInset
            goalFirst = flow > room(viewport[0]) + FadeClearance.roundToPx()
            if (goalFirst != Snapshot.withoutReadObservation { order.goalFirst }) order.goalFirst = goalFirst
        }
        val blocks = if (goalFirst) listOf(headerBlock, goalBlock, signalsBlock) else listOf(headerBlock, signalsBlock, goalBlock)
        val gaps = if (goalFirst) listOf(sectionGap, sectionGap - chipInset) else listOf(sectionGap, sectionGap)
        val height = blocks.sumOf { it.height } + gaps.sum() + endSpace(goalFirst).roundToPx()
        layout(constraints.maxWidth, height.coerceAtLeast(constraints.minHeight)) {
            var y = 0
            blocks.forEachIndexed { index, block ->
                block.placeRelative(0, y)
                y += block.height + gaps.getOrElse(index) { 0 }
            }
        }
    }
}

/** The date and the On-device pill on one line, then the greeting with the orb rising after it. */
@Composable
private fun Header(runtime: RuntimeStatus, onOpenRuntime: () -> Unit, modifier: Modifier = Modifier) {
    val clock = LocalWellnessClock.current
    val now = clock.now()
    Column(modifier) {
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

/** One body signal as the Body section shows it; [shown] is its (animating) value on screen. */
private class BodyStat(
    val spec: BodySpec,
    val shown: String,
    val spoken: String,
    val color: Color,
    val progress: Float,
)

/** A body signal's fixed parts, and its widest possible value, which sizes the card's columns. */
private enum class BodySpec(
    val editor: TodayEditor,
    val label: String,
    val tileLabel: String,
    val unit: String?,
    val widest: String,
    val caption: String?,
) {
    RestingHr(TodayEditor.RestingHr, "Resting HR", "Resting HR", "bpm", "110", null),
    Hrv(TodayEditor.Hrv, "HRV", "HRV", "ms", "120", null),
    Steps(TodayEditor.Steps, "Steps", "Steps yesterday", null, formatSteps(25_000), "of 10k"),
}

/**
 * Resting HR, HRV and yesterday's steps: side by side in one card, so the goal stays in view
 * above the Generate button, or as tiles when the card's columns can't hold them.
 */
@Composable
private fun BodySection(metrics: DayMetrics, onOpenEditor: (TodayEditor) -> Unit) {
    val colors = WellnessTheme.colors
    val restingHr by animateFloatAsState(metrics.restingHr, valueChange(), label = "restingHr")
    val hrv by animateFloatAsState(metrics.hrvMs, valueChange(), label = "hrv")
    val steps by animateFloatAsState(metrics.steps, valueChange(), label = "steps")
    val stats = listOf(
        BodyStat(
            BodySpec.RestingHr,
            shown = restingHr.roundToInt().toString(),
            spoken = "Resting heart rate, ${metrics.restingHr.roundToInt()} beats per minute",
            color = colors.restingHr,
            progress = Metric.RestingHr.fraction(metrics.restingHr),
        ),
        BodyStat(
            BodySpec.Hrv,
            shown = hrv.roundToInt().toString(),
            spoken = "Heart rate variability, ${metrics.hrvMs.roundToInt()} milliseconds",
            color = colors.hrv,
            progress = Metric.Hrv.fraction(metrics.hrvMs),
        ),
        BodyStat(
            BodySpec.Steps,
            shown = formatSteps(steps.roundToInt()),
            spoken = "Steps yesterday, ${formatSteps(metrics.steps.roundToInt())} of 10,000",
            color = colors.steps,
            progress = metrics.steps / StepsGoal,
        ),
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        when (val layout = rememberBodyLayout(maxWidth)) {
            is BodyLayout.Columns -> BodyCard(stats, layout, onOpenEditor)
            is BodyLayout.Tiles -> BodyTiles(stats, layout.sideBySide, onOpenEditor)
        }
    }
}

/** How the Body section shows the signals: in the card's columns, or as tiles. */
private sealed interface BodyLayout {
    /**
     * The card's three columns, values in [valueStyle]. Every label takes [labelLines] lines and
     * [labelHeight], so the values line up; [labelLineHeight] is one line, where the dot and
     * the edit glyph sit.
     */
    data class Columns(
        val valueStyle: TextStyle,
        val labelLines: Int,
        val labelHeight: Dp,
        val labelLineHeight: Dp,
    ) : BodyLayout

    /**
     * Tiles: resting HR and HRV [sideBySide], then steps across the full width, or all three
     * stacked when a half-width tile would cut a label short.
     */
    data class Tiles(val sideBySide: Boolean) : BodyLayout
}

/**
 * The Body layout for a section [width] wide. The card's columns must hold every value the
 * signals can take (the widest: 110 bpm, 120 ms, 25,000) in `metricM`, or else one step
 * smaller. A label too long for its line wraps onto a second one rather than truncating (a
 * word never breaks). Only when that isn't enough (very large text, a narrow phone) do the
 * signals become tiles.
 */
@Composable
private fun rememberBodyLayout(width: Dp): BodyLayout {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val type = WellnessTheme.type
    val labelStyle = MaterialTheme.typography.labelMedium
    return remember(width, density, measurer, type, labelStyle) {
        with(density) {
            fun widthOf(text: String, style: TextStyle): Int =
                measurer.measure(text, style, softWrap = false, maxLines = 1).size.width
            val columns = BodySpec.entries.size
            // In pixels, less one for how the card's row rounds its columns.
            val column = ((width - BodyInset * 2 - DividerWidth * (columns - 1)) / columns - BodyGap * 2).toPx().toInt() - 1
            val valueStyle = listOf(type.metricM, type.metricM.compact()).firstOrNull { style ->
                BodySpec.entries.all { spec ->
                    val unit = spec.unit?.let { UnitGap.roundToPx() + widthOf(it, type.metricUnit) } ?: 0
                    widthOf(spec.widest, style) + unit <= column
                }
            }
            val label = column - (DotSize + DotGap + LabelGap + EditGlyphSize).roundToPx()
            val labelLines = BodySpec.entries.maxOf { spec ->
                if (spec.label.split(' ').any { widthOf(it, labelStyle) > label }) {
                    Int.MAX_VALUE
                } else {
                    measurer.measure(spec.label, labelStyle, constraints = Constraints(maxWidth = label)).lineCount
                }
            }
            if (valueStyle == null || labelLines > MaxLabelLines) {
                val halfTile = metricTileLabelWidth((width - WellnessSpacing.ItemGap) / 2).toPx().toInt() - 1
                BodyLayout.Tiles(sideBySide = listOf(BodySpec.RestingHr, BodySpec.Hrv).all { widthOf(it.tileLabel, labelStyle) <= halfTile })
            } else {
                val labelHeight = BodySpec.entries.maxOf { spec ->
                    measurer.measure(spec.label, labelStyle, constraints = Constraints(maxWidth = label)).size.height
                }
                val lineHeight = measurer.measure(BodySpec.RestingHr.label, labelStyle, maxLines = 1).size.height
                BodyLayout.Columns(valueStyle, labelLines, labelHeight.toDp(), lineHeight.toDp())
            }
        }
    }
}

/** The Body card's values one step down from `metricM`, for columns too narrow for it. */
private fun TextStyle.compact(): TextStyle = copy(fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp)

/** The three signals in columns between hairlines; each column opens its own editor. */
@Composable
private fun BodyCard(stats: List<BodyStat>, layout: BodyLayout.Columns, onOpenEditor: (TodayEditor) -> Unit) {
    val hairline = WellnessTheme.colors.hairline
    WellnessCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
        Row(
            Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = BodyInset),
        ) {
            stats.forEachIndexed { index, stat ->
                if (index > 0) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .padding(vertical = WellnessSpacing.CardPadding)
                            .width(DividerWidth)
                            .background(hairline),
                    )
                }
                BodyColumn(stat, layout, onClick = { onOpenEditor(stat.spec.editor) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BodyColumn(stat: BodyStat, layout: BodyLayout.Columns, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    val type = WellnessTheme.type
    Column(
        modifier
            .fillMaxHeight()
            .clickable(onClickLabel = "Edit", role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = stat.spoken }
            .padding(horizontal = BodyGap, vertical = WellnessSpacing.CardPadding),
    ) {
        // The dot and the edit glyph sit on the label's first line.
        Row {
            Box(Modifier.height(layout.labelLineHeight), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(DotSize)
                        .background(stat.color, CircleShape),
                )
            }
            Spacer(Modifier.width(DotGap))
            Text(
                text = stat.spec.label,
                // A height, not minLines, which the row's intrinsic height would miss.
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = layout.labelHeight),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                maxLines = layout.labelLines,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(LabelGap))
            Box(Modifier.height(layout.labelLineHeight), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(EditGlyphSize))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Text(
                text = stat.shown,
                modifier = Modifier.alignByBaseline(),
                style = layout.valueStyle,
                color = colors.textPrimary,
                maxLines = 1,
            )
            stat.spec.unit?.let { unit ->
                Spacer(Modifier.width(UnitGap))
                Text(
                    text = unit,
                    modifier = Modifier.alignByBaseline(),
                    style = type.metricUnit,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        LinearMeter(stat.progress, stat.color)
        stat.spec.caption?.let { caption ->
            Spacer(Modifier.height(6.dp))
            Text(caption, style = MaterialTheme.typography.bodySmall, color = colors.textTertiary, maxLines = 1)
        }
    }
}

/**
 * The signals as tiles, for very large text or a narrow phone: resting HR and HRV
 * [sideBySide], then steps across the full width, or all three stacked.
 */
@Composable
private fun BodyTiles(stats: List<BodyStat>, sideBySide: Boolean, onOpenEditor: (TodayEditor) -> Unit) {
    val (restingHr, hrv, steps) = stats
    Column(verticalArrangement = Arrangement.spacedBy(WellnessSpacing.ItemGap)) {
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(WellnessSpacing.ItemGap)) {
                BodyTile(restingHr, Icons.Rounded.Favorite, onOpenEditor, Modifier.weight(1f))
                BodyTile(hrv, Icons.Rounded.MonitorHeart, onOpenEditor, Modifier.weight(1f))
            }
        } else {
            BodyTile(restingHr, Icons.Rounded.Favorite, onOpenEditor, Modifier.fillMaxWidth(), WellnessShapes.Card)
            BodyTile(hrv, Icons.Rounded.MonitorHeart, onOpenEditor, Modifier.fillMaxWidth(), WellnessShapes.Card)
        }
        BodyTile(steps, Icons.AutoMirrored.Rounded.DirectionsWalk, onOpenEditor, Modifier.fillMaxWidth(), WellnessShapes.Card)
    }
}

@Composable
private fun BodyTile(
    stat: BodyStat,
    icon: ImageVector,
    onOpenEditor: (TodayEditor) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = WellnessShapes.Tile,
) {
    MetricTile(
        icon = icon,
        label = stat.spec.tileLabel,
        value = stat.shown,
        unit = stat.spec.unit,
        color = stat.color,
        modifier = modifier,
        onClick = { onOpenEditor(stat.spec.editor) },
        meterProgress = stat.progress,
        meterCaption = stat.spec.caption,
        shape = shape,
    )
}

/**
 * Goals to start from, in a row that scrolls edge to edge and fades out at the screen edges
 * while there is more that way; one is selected while it is the goal.
 */
@Composable
private fun GoalSuggestions(goal: String, onSelect: (String) -> Unit) {
    val current = goal.trim()
    val scroll = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScrollFades(scroll)
            .horizontalScroll(scroll)
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
 * While the on-device service doesn't answer, a line just above the button says so, with the
 * way to the runtime details; the button stays live, as the service may be back by then. The
 * line hangs over the lower part of the fade instead of growing the footer, so the fade keeps
 * its top and what rests above it (the goal's suggestions) stays clear.
 */
@Composable
private fun GenerateFooter(
    unavailable: Boolean,
    overContent: () -> Boolean,
    onGenerate: () -> Unit,
    onOpenRuntime: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val bg = WellnessTheme.colors.bg
    Box(
        modifier
            .fillMaxWidth()
            .drawWithCache {
                val scrim = canvasFade(bg, startY = 0f, endY = FooterFade.toPx())
                onDrawBehind { drawRect(scrim) }
            }
            .padding(contentPadding),
    ) {
        GradientButton(
            text = "Generate nudge",
            onClick = onGenerate,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(start = WellnessSpacing.ScreenMargin, end = WellnessSpacing.ScreenMargin, top = FooterFade, bottom = FooterGap)
                .fillMaxWidth(),
        )
        AnimatedVisibility(
            visible = unavailable,
            // Takes no room: it hangs over the fade, its bottom on the button's top edge.
            modifier = Modifier.layout { measurable, constraints ->
                val notice = measurable.measure(constraints)
                layout(notice.width, 0) { notice.placeRelative(0, FooterFade.roundToPx() - notice.height) }
            },
            enter = fadeIn(tween(WellnessMotion.SmallMillis)),
            exit = fadeOut(tween(WellnessMotion.SmallMillis)),
        ) {
            RuntimeNotice(onOpenRuntime, overContent)
        }
    }
}

/**
 * "The on-device service isn’t answering." with a Details action that opens the runtime
 * sheet, across the footer's width. Its backing keeps content scrolling under the footer from
 * running behind the words: clear across the fade's top and then solid, so what rests above
 * the fade stays as it is. When content runs on under the footer at rest ([overContent]), the
 * backing fades in above the notice instead, over the fade's full height, as if the footer had
 * grown, so that content fades out softly rather than being cut.
 */
@Composable
private fun RuntimeNotice(onOpenRuntime: () -> Unit, overContent: () -> Boolean) {
    val bg = WellnessTheme.colors.bg
    Row(
        Modifier
            .fillMaxWidth()
            .drawWithCache {
                val above = FooterFade.toPx()
                val backing = if (overContent()) {
                    canvasFade(bg, startY = -above, endY = 0f)
                } else {
                    canvasFade(bg, startY = FadeClearance.toPx(), endY = (FadeClearance + NoticeFeather).toPx())
                }
                onDrawBehind { drawRect(backing, topLeft = Offset(0f, -above), size = Size(size.width, size.height + above)) }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .heightIn(min = NoticeHeight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "The on-device service isn’t answering.",
            modifier = Modifier
                .weight(1f, fill = false)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodySmall,
            color = WellnessTheme.colors.dangerText,
        )
        // Its touch target spans the notice's height and no more, clear of the button below.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides NoticeHeight) {
            TextAction(text = "Details", icon = null, onClick = onOpenRuntime)
        }
    }
}

/**
 * The canvas color fading in from [startY] to [endY], eased in and out (smoothstep), so the
 * fade has no visible start or end.
 */
private fun canvasFade(bg: Color, startY: Float, endY: Float): Brush = Brush.verticalGradient(
    0f to bg.copy(alpha = 0f),
    0.25f to bg.copy(alpha = 0.16f),
    0.5f to bg.copy(alpha = 0.5f),
    0.75f to bg.copy(alpha = 0.84f),
    1f to bg,
    startY = startY,
    endY = endY,
)

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

/** 6.5 → "6 hours 30 minutes", as TalkBack reads a sleep duration. */
internal fun spokenDuration(hours: Float): String {
    val minutes = (hours * 60).roundToInt().coerceAtLeast(0)
    val h = minutes / 60
    val m = minutes % 60
    fun count(n: Int, unit: String) = if (n == 1) "1 $unit" else "$n ${unit}s"
    val parts = buildList {
        if (h > 0) add(count(h, "hour"))
        if (m > 0 || h == 0) add(count(m, "minute"))
    }
    return parts.joinToString(" ")
}

/** 6.5 → "6 hours 30 minutes asleep", as TalkBack reads the sleep card. */
internal fun spokenSleep(hours: Float): String = spokenDuration(hours) + " asleep"

// Each maps to a goal category the mim recognizes, so a nudge from a chip joins For you.
private val Suggestions = listOf(
    "Sleep better tonight",
    "Lower stress",
    "Feel less tired",
    "Recover from training",
    "Exercise more today",
)

private const val StepsGoal = 10_000f
private const val OrbId = "orb"
private val OrbSize = 22.dp
private val OrbGap = 10.dp
private val HeaderTopPadding = 12.dp

// OnDevicePill's visible height, inside its 48 dp touch target.
private val PillHeight = 36.dp
private val StatusBarFade = 16.dp

// The Body card: 16 dp either side of the hairlines between columns, and the card's 20 dp
// padding at the outer edges (4 dp of inset plus a column's 16).
private val BodyGap = 16.dp
private val BodyInset = WellnessSpacing.CardPadding - BodyGap
private val DividerWidth = 1.dp
private val DotSize = 6.dp
private val DotGap = 6.dp
private val EditGlyphSize = 14.dp

// Between a column's label and its edit glyph, whose drawing sits a further 2 dp into its box.
private val LabelGap = 2.dp
private const val MaxLabelLines = 2
private val UnitGap = 4.dp

// A SuggestionChip's 48 dp touch target reaches 6 dp past its 36 dp pill, above and below.
private val ChipTargetInset = 6.dp

// The footer: a fade above the 60 dp button, and a gap between the button and the tab bar.
private val FooterFade = 44.dp
private val FooterGap = 16.dp
private val GenerateButtonHeight = 60.dp
private val FooterReserve = FooterFade + GenerateButtonHeight + FooterGap

// The top of the footer's fade, still too faint to see: the content may rest inside it, and
// the unavailable notice's backing starts below it.
private val FadeClearance = 4.dp

// The unavailable notice fills the fade's height, which its Details action takes as its
// touch target (the spec's 44 dp); its backing eases in over this much below the clearance.
private val NoticeHeight = FooterFade
private val NoticeFeather = 12.dp

// Room at the end of the content. The last suggestions stop in the top quarter of the fade,
// where it is still too faint to see (the chips' touch targets reach lower than the pills).
private val FooterHeight = FooterFade * 0.75f + GenerateButtonHeight + FooterGap
