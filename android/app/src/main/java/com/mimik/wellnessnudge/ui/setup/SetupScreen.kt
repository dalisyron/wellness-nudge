package com.mimik.wellnessnudge.ui.setup

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.GradientButton
import com.mimik.wellnessnudge.ui.components.IconBadge
import com.mimik.wellnessnudge.ui.components.IndeterminateMeter
import com.mimik.wellnessnudge.ui.components.LinearMeter
import com.mimik.wellnessnudge.ui.components.NudgeOrb
import com.mimik.wellnessnudge.ui.components.OrbMode
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.components.SectionHeader
import com.mimik.wellnessnudge.ui.components.Spinner
import com.mimik.wellnessnudge.ui.components.TextAction
import com.mimik.wellnessnudge.ui.components.TextActionPadding
import com.mimik.wellnessnudge.ui.components.WellnessCard
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.theme.tabular

/**
 * First-run setup, the moment the whole stack boots on this phone: a checklist from the
 * mimOE runtime to the AI models, a card per model (shown from the start, so their size is
 * clear before the download), and a sticky action that turns into "Start using Wellness
 * Nudge" once every model is ready, or into Try again when something needs another go.
 *
 * While models download the page rests at its end, so the cards and the action stay in
 * view on phones where everything doesn't fit; it glides there when the downloads begin.
 *
 * @param onRetry restarts setup after a failure.
 * @param onRetryModel retries one failed model download by id.
 * @param onContinue enters the app once every model is ready.
 */
@Composable
fun SetupScreen(
    state: SetupUiState,
    onRetry: () -> Unit,
    onRetryModel: (modelId: String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState(initial = if (state.modelPhase) Int.MAX_VALUE else 0)
    FollowDownloads(state.modelPhase, scrollState)

    DaybreakBackground(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = WellnessSpacing.ScreenMargin)
                .padding(top = 12.dp, bottom = FooterHeight + ContentClearance),
        ) {
            Hero(
                orb = when (state.status) {
                    SetupStatus.Working -> OrbMode.Thinking
                    SetupStatus.Ready -> OrbMode.Idle
                    SetupStatus.Paused, SetupStatus.Failed -> OrbMode.Still
                },
            )
            Spacer(Modifier.height(WellnessSpacing.SectionGap))
            SectionHeader(if (state.status == SetupStatus.Ready) "Set up on this phone" else "Setting up")
            Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
            Checklist(state.steps)
            if (state.models.isNotEmpty()) {
                Spacer(Modifier.height(WellnessSpacing.SectionGap))
                SectionHeader("AI models")
                Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
                Column(verticalArrangement = Arrangement.spacedBy(WellnessSpacing.ItemGap)) {
                    state.models.forEach { model ->
                        key(model.id) { ModelCard(model, onRetry = { onRetryModel(model.id) }) }
                    }
                }
            }
        }
        StatusBarScrim(scrollState)
        Footer(
            state = state,
            onRetry = onRetry,
            onRetryModel = onRetryModel,
            onContinue = onContinue,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Brings the model cards into view once, when their downloads begin while they are below the
 * fold. Pages first composed in the model phase already start at their end.
 */
@Composable
private fun FollowDownloads(modelPhase: Boolean, scrollState: ScrollState) {
    var followed by rememberSaveable { mutableStateOf(modelPhase) }
    LaunchedEffect(modelPhase) {
        if (!modelPhase || followed) return@LaunchedEffect
        followed = true
        // By the next frame the new section is laid out, so the scroll range includes it.
        withFrameNanos {}
        scrollState.animateScrollTo(
            scrollState.maxValue,
            tween(WellnessMotion.RevealMillis, easing = WellnessMotion.Easing),
        )
    }
}

/** The title with the orb beside it, like the Today header, then what setup is for. */
@Composable
private fun Hero(orb: OrbMode) {
    val colors = WellnessTheme.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Private AI,\nset up once.",
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                style = MaterialTheme.typography.displayLarge,
                color = colors.textPrimary,
            )
            Spacer(Modifier.width(16.dp))
            NudgeOrb(size = 88.dp, mode = orb)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Wellness Nudge runs small AI models entirely on this phone through mimik’s mimOE " +
                "runtime. Your sleep, heart and activity data never leave the device.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
        )
    }
}

/** The setup steps as a timeline: status markers joined by a line, lit where steps are done. */
@Composable
private fun Checklist(steps: List<StepState>) {
    WellnessCard(Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            if (index > 0) Spacer(Modifier.height(RowGap))
            ChecklistRow(step, number = index + 1, joinsNext = index < steps.lastIndex)
        }
    }
}

@Composable
private fun ChecklistRow(state: StepState, number: Int, joinsNext: Boolean) {
    val colors = WellnessTheme.colors
    val pending = state.status == StepStatus.Pending
    // One continuous timeline: lit from each finished step down to the next.
    val connector = if (state.status == StepStatus.Done) colors.success else colors.hairlineStrong
    // The marker is centered on the title's line; at large text sizes the line is the taller one.
    val titleStyle = MaterialTheme.typography.titleSmall
    val titleLine = with(LocalDensity.current) { titleStyle.lineHeight.toDp() }
    val markerTop = ((titleLine - MarkerSize) / 2).coerceAtLeast(0.dp)
    val textTop = ((MarkerSize - titleLine) / 2).coerceAtLeast(0.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                if (joinsNext) {
                    // From this marker's edge to the next one's, with a hair of air at each end.
                    val x = MarkerSize.toPx() / 2f
                    val top = markerTop.toPx()
                    val gap = ConnectorGap.toPx()
                    drawLine(
                        color = connector,
                        start = Offset(x, top + MarkerSize.toPx() + gap),
                        end = Offset(x, size.height + RowGap.toPx() + top - gap),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Butt,
                    )
                }
            }
            .semantics(mergeDescendants = true) {
                stateDescription = state.status.description
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        StepMarker(state.status, number, Modifier.padding(top = markerTop))
        Spacer(Modifier.width(14.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(top = textTop),
        ) {
            Text(
                text = state.step.title,
                style = titleStyle,
                color = if (pending) colors.textSecondary else colors.textPrimary,
            )
            Text(
                text = state.detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (pending) colors.textTertiary else colors.textSecondary,
            )
            if (state.message != null) {
                Spacer(Modifier.height(6.dp))
                Text(state.message, style = MaterialTheme.typography.bodySmall, color = colors.dangerText)
            }
        }
    }
}

@Composable
private fun StepMarker(status: StepStatus, number: Int, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    Crossfade(status, modifier, animationSpec = tween(WellnessMotion.SmallMillis), label = "stepMarker") { current ->
        when (current) {
            StepStatus.Done -> IconBadge(Icons.Rounded.Check, colors.success, size = MarkerSize)
            StepStatus.Failed -> IconBadge(Icons.Rounded.PriorityHigh, colors.danger, size = MarkerSize)
            StepStatus.Active -> Box(
                Modifier
                    .size(MarkerSize)
                    .background(colors.tint(colors.accent), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Spinner(size = 18.dp)
            }
            StepStatus.Pending -> Box(
                Modifier
                    .size(MarkerSize)
                    .border(1.5.dp, colors.hairlineStrong, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$number", style = MaterialTheme.typography.labelMedium, color = colors.textTertiary)
            }
        }
    }
}

@Composable
private fun ModelCard(model: ModelCardState, onRetry: () -> Unit) {
    val colors = WellnessTheme.colors
    WellnessCard(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        ModelHeader(model.name, model.details)
        when (model.status) {
            // Done: the check and "Ready" say it; a full gradient bar would only compete with the action.
            ModelStatus.Ready -> Unit
            ModelStatus.Starting -> Meter { IndeterminateMeter(colors.daybreak) }
            // Paused where the download stopped; the message below says why.
            ModelStatus.Failed -> Meter { LinearMeter(model.progress, colors.textDisabled) }
            else -> Meter { LinearMeter(model.progress, colors.daybreak, progressDescription = model.progressDescription) }
        }
        Spacer(Modifier.height(10.dp))
        if (model.status == ModelStatus.Failed) {
            WithTrailingAction(action = { RetryAction(onRetry) }) { StatusLine(model) }
        } else {
            StatusLine(model)
        }
    }
}

/**
 * The model's role, with its model and size at the end of the same line on a shared
 * baseline; the details drop below the role when the two don't fit side by side.
 */
@Composable
private fun ModelHeader(name: String, details: String) {
    val colors = WellnessTheme.colors
    Layout(
        content = {
            Text(name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(details, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val gap = 12.dp.roundToPx()
        val namePlaceable = measurables[0].measure(Constraints(maxWidth = width))
        val available = width - namePlaceable.width - gap
        val sideBySide = measurables[1].maxIntrinsicWidth(Constraints.Infinity) <= available
        val detailsPlaceable = measurables[1].measure(Constraints(maxWidth = if (sideBySide) available else width))
        if (sideBySide) {
            val detailsY = namePlaceable[FirstBaseline] - detailsPlaceable[FirstBaseline]
            layout(width, maxOf(namePlaceable.height, detailsY + detailsPlaceable.height)) {
                namePlaceable.placeRelative(0, 0)
                detailsPlaceable.placeRelative(width - detailsPlaceable.width, detailsY)
            }
        } else {
            val detailsY = namePlaceable.height + 2.dp.roundToPx()
            layout(width, detailsY + detailsPlaceable.height) {
                namePlaceable.placeRelative(0, 0)
                detailsPlaceable.placeRelative(0, detailsY)
            }
        }
    }
}

@Composable
private fun StatusLine(model: ModelCardState) {
    val colors = WellnessTheme.colors
    val (icon, tint) = when (model.status) {
        ModelStatus.Upcoming, ModelStatus.Waiting -> Icons.Rounded.Schedule to colors.textTertiary
        ModelStatus.Starting, ModelStatus.Downloading -> Icons.Rounded.Download to colors.accent
        ModelStatus.Ready -> Icons.Rounded.CheckCircle to colors.success
        ModelStatus.Failed -> Icons.Rounded.ErrorOutline to colors.danger
    }
    val style = MaterialTheme.typography.bodySmall
    Row {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = model.statusLine,
            style = if (model.status == ModelStatus.Downloading) style.tabular() else style,
            color = if (model.status == ModelStatus.Failed) colors.dangerText else colors.textSecondary,
        )
    }
}

/** A meter below the model's name, with the gap it needs. */
@Composable
private fun Meter(meter: @Composable () -> Unit) {
    Spacer(Modifier.height(14.dp))
    meter()
}

/** Try again that ignores a second tap while the card is still turning back to downloading. */
@Composable
private fun RetryAction(onRetry: () -> Unit) {
    var lastTap by remember { mutableLongStateOf(0L) }
    TextAction(
        text = "Try again",
        icon = Icons.Rounded.Refresh,
        onClick = {
            val now = SystemClock.uptimeMillis()
            if (now - lastTap > RetryDebounceMillis) {
                lastTap = now
                onRetry()
            }
        },
    )
}

/**
 * [content] with [action] at its end. The row is only as tall as the content: the action is
 * centered on it and overhangs, keeping its 48 dp touch target, with its label lined up with
 * the card's content edge (as in SectionHeader).
 */
@Composable
private fun WithTrailingAction(action: @Composable () -> Unit, content: @Composable () -> Unit) {
    Layout(
        content = {
            Box { content() }
            Box { action() }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val overhang = TextActionPadding.roundToPx()
        val actionPlaceable = measurables[1].measure(Constraints(maxWidth = width))
        val reserved = actionPlaceable.width - overhang + 12.dp.roundToPx()
        val contentPlaceable = measurables[0].measure(Constraints(maxWidth = (width - reserved).coerceAtLeast(0)))
        val height = contentPlaceable.height
        layout(width, height) {
            contentPlaceable.placeRelative(0, 0)
            actionPlaceable.placeRelative(
                x = width - actionPlaceable.width + overhang,
                y = (height - actionPlaceable.height) / 2,
            )
        }
    }
}

/**
 * The sticky action over a short fade, so the page scrolls away beneath it: "Setting up…"
 * while work runs, Try again after a failure (the whole setup, or the model download that
 * stopped), then the way into the app.
 */
@Composable
private fun Footer(
    state: SetupUiState,
    onRetry: () -> Unit,
    onRetryModel: (modelId: String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    Column(modifier.fillMaxWidth()) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(ContentClearance)
                .background(Brush.verticalGradient(listOf(colors.bg.copy(alpha = 0f), colors.bg))),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                )
                .padding(horizontal = WellnessSpacing.ScreenMargin)
                .padding(bottom = FooterBottomMargin),
        ) {
            val fill = Modifier
                .fillMaxWidth()
                .heightIn(min = FooterButtonHeight)
            when (state.status) {
                SetupStatus.Failed -> SecondaryButton(
                    text = "Try again",
                    onClick = onRetry,
                    modifier = fill,
                    icon = Icons.Rounded.Refresh,
                )
                SetupStatus.Ready -> GradientButton(
                    text = "Start using Wellness Nudge",
                    onClick = onContinue,
                    modifier = fill,
                    icon = null,
                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                )
                // A download stopped and nothing else runs: the way forward is to try it again.
                SetupStatus.Paused -> {
                    val failed = state.models.firstOrNull { it.status == ModelStatus.Failed }
                    SecondaryButton(
                        text = "Try again",
                        onClick = { failed?.let { onRetryModel(it.id) } },
                        modifier = fill,
                        icon = Icons.Rounded.Refresh,
                    )
                }
                SetupStatus.Working -> GradientButton(
                    text = "Setting up…",
                    onClick = {},
                    modifier = fill,
                    icon = null,
                    enabled = false,
                    loading = true,
                )
            }
        }
    }
}

/**
 * The canvas redrawn over the status bar once the page scrolls, fading out just below it,
 * so text never runs under the clock and icons. It repeats [DaybreakBackground] rather than
 * a flat color, so the top glow stays seamless.
 */
@Composable
private fun StatusBarScrim(scrollState: ScrollState) {
    DaybreakBackground(
        Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars.add(WindowInsets(top = ScrimOverhang)))
            .graphicsLayer {
                alpha = (scrollState.value / ScrimFade.toPx()).coerceIn(0f, 1f)
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithCache {
                val solid = ((size.height - ScrimFade.toPx()) / size.height).coerceIn(0f, 1f)
                val mask = Brush.verticalGradient(0f to Color.Black, solid to Color.Black, 1f to Color.Transparent)
                onDrawWithContent {
                    drawContent()
                    drawRect(mask, blendMode = BlendMode.DstIn)
                }
            },
    ) {}
}

private val StepStatus.description: String
    get() = when (this) {
        StepStatus.Done -> "Done"
        StepStatus.Active -> "In progress"
        StepStatus.Pending -> "Not started"
        StepStatus.Failed -> "Failed"
    }

private val MarkerSize = 28.dp
private val RowGap = 14.dp
private val ConnectorGap = 2.dp

/** Above the navigation bar, as on the Nudge screen. */
private val FooterBottomMargin = 12.dp

/** Every footer action is as tall as the gradient one, so the footer doesn't jump between states. */
private val FooterButtonHeight = 60.dp

/** The action and its margin above the navigation bar. */
private val FooterHeight = FooterButtonHeight + FooterBottomMargin

/** Between the last card and the action: the fade the page scrolls away under. */
private val ContentClearance = 16.dp

private val ScrimOverhang = 8.dp
private val ScrimFade = 24.dp

private const val RetryDebounceMillis = 1_500L
