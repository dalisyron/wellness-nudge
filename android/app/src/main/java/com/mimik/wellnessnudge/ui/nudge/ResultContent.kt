package com.mimik.wellnessnudge.ui.nudge

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.bootstrap.modelDisplayName
import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.data.feedback
import com.mimik.wellnessnudge.ui.components.CategoryBadge
import com.mimik.wellnessnudge.ui.components.CircleIconButton
import com.mimik.wellnessnudge.ui.components.Eyebrow
import com.mimik.wellnessnudge.ui.components.OrbMode
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.components.SectionHeader
import com.mimik.wellnessnudge.ui.components.WellnessCard
import com.mimik.wellnessnudge.ui.components.glow
import com.mimik.wellnessnudge.ui.format.LocalWellnessClock
import com.mimik.wellnessnudge.ui.format.WellnessClock
import com.mimik.wellnessnudge.ui.format.dayLabel
import com.mimik.wellnessnudge.ui.format.formatSeconds
import com.mimik.wellnessnudge.ui.format.timeLabel
import com.mimik.wellnessnudge.ui.format.withTypographicQuotes
import com.mimik.wellnessnudge.ui.theme.Daybreak
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.launch

/**
 * The nudge: the settled orb with its time and category, the hero card, what it was grounded
 * in and how it was made, the rating, and the way out. Scrolls between the top bar and the
 * pinned footer when it runs long, e.g. at large font sizes. [entrance] is the stage's
 * visibility scope, which staggers the sections in. While [deleting], the actions wait.
 */
@Composable
internal fun ResultContent(
    result: NudgeContent.Result,
    orb: OrbSlot,
    entrance: AnimatedVisibilityScope,
    deleting: Boolean,
    onFeedback: (Feedback) -> Unit,
    onTryAnother: (NudgeRequest) -> Unit,
    onDone: () -> Unit,
    onRevealed: (nudgeId: String) -> Unit,
) {
    val nudge = result.nudge
    Column(
        Modifier
            .fillMaxSize()
            .semantics { paneTitle = "Your nudge" },
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val scroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxSize()
                    .scrollFades(scroll)
                    .verticalScroll(scroll)
                    // The top padding also keeps the orb's halo clear of the scroll edge.
                    .padding(horizontal = WellnessSpacing.ScreenMargin)
                    .padding(top = WellnessSpacing.SectionGap, bottom = 24.dp),
            ) {
                // The time and category caption the card, so they sit close above it.
                ResultHeader(nudge, orb, entrance.entrance(order = 0))
                Spacer(Modifier.height(WellnessSpacing.ItemGap))
                NudgeCard(
                    text = nudge.nudge,
                    reveal = result.reveal,
                    onRevealed = { onRevealed(nudge.id) },
                    modifier = entrance.entrance(order = 1, rise = 32.dp),
                )
                Spacer(Modifier.height(WellnessSpacing.SectionGap))
                Column(entrance.entrance(order = 2)) {
                    if (result.signals.isNotEmpty()) {
                        SectionHeader("Grounded in")
                        Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
                        SignalChips(result.signals)
                        Spacer(Modifier.height(16.dp))
                    }
                    GeneratedLine(latencyMs = result.latencyMs, model = nudge.model)
                }
                Spacer(Modifier.height(WellnessSpacing.SectionGap))
                FeedbackSection(
                    feedback = nudge.feedback,
                    savedNote = if (result.joinsForYou) "Saved to For you." else "Saved as helpful.",
                    enabled = !deleting,
                    onFeedback = onFeedback,
                    modifier = entrance.entrance(order = 3),
                )
            }
        }
        StageFooter(entrance.entrance(order = 4)) {
            ResultFooter(
                request = result.request,
                enabled = !deleting,
                onTryAnother = onTryAnother,
                onDone = onDone,
            )
        }
    }
}

/**
 * Try another and Done, side by side; at large text sizes Try another becomes a round
 * button, so the footer keeps one row. A nudge whose record doesn't carry its signals (an
 * older one quoted in For you) can't be generated again: Done alone.
 */
@Composable
private fun ResultFooter(request: NudgeRequest, enabled: Boolean, onTryAnother: (NudgeRequest) -> Unit, onDone: () -> Unit) {
    val done: @Composable (Modifier) -> Unit = { doneModifier ->
        SecondaryButton(text = "Done", onClick = onDone, modifier = doneModifier.heightIn(min = FooterButtonHeight), enabled = enabled)
    }
    if (request.isEmpty) {
        done(Modifier.fillMaxWidth())
        return
    }
    ButtonPair(
        first = {
            SecondaryButton(
                text = "Try another",
                onClick = { onTryAnother(request) },
                modifier = Modifier.heightIn(min = FooterButtonHeight),
                icon = Icons.Rounded.Refresh,
                enabled = enabled,
            )
        },
        second = { done(Modifier) },
        compactFirst = {
            CircleIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Try another",
                onClick = { onTryAnother(request) },
                modifier = Modifier.size(FooterButtonHeight),
                enabled = enabled,
            )
        },
    )
}

/** The settled orb, then when the nudge was written and its category. */
@Composable
private fun ResultHeader(nudge: NudgeHistoryItem, orb: OrbSlot, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        orb(ResultOrbSize, OrbMode.Idle, Modifier)
        Spacer(Modifier.width(14.dp))
        Column {
            Eyebrow(nudgeEyebrow(nudge.ts, LocalWellnessClock.current))
            Spacer(Modifier.height(6.dp))
            CategoryBadge(nudge.category)
        }
    }
}

/**
 * The hero: the nudge set large in the serif, in a card lit by the Daybreak gradient, its
 * border and the light it casts below. For a fresh nudge the light comes up as the words
 * arrive, and the text is announced to TalkBack.
 */
@Composable
private fun NudgeCard(text: String, reveal: Boolean, onRevealed: () -> Unit, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    val animate = rememberAnimationsEnabled()
    // Settled once per text, as the word reveal is.
    val light = remember(text) { Animatable(if (reveal && animate) 0f else 1f) }
    LaunchedEffect(text) {
        if (light.value < 1f) light.animateTo(1f, tween(WellnessMotion.RevealMillis, RevealDelayMillis, WellnessMotion.Easing))
    }
    val glowAlpha = if (colors.isDark) HeroGlowDark else HeroGlowLight
    WellnessCard(
        modifier = modifier
            .fillMaxWidth()
            .glow(
                brush = Daybreak.Horizontal,
                shape = WellnessShapes.Card,
                alpha = { glowAlpha * light.value },
                blurRadius = 32.dp,
                offsetY = 12.dp,
                spread = (-8).dp,
            ),
        contentPadding = HeroPadding,
        border = BorderStroke(1.5.dp, colors.daybreakBorder),
        glow = Daybreak.Orchid.copy(alpha = 0.18f),
    ) {
        WordRevealText(
            text = text.withTypographicQuotes().withoutWidow(),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            reveal = reveal,
            onRevealed = onRevealed,
            modifier = if (reveal) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
            delayMillis = RevealDelayMillis,
        )
    }
}

/**
 * "Generated on this phone in 4.2 s · SmolLM2 360M". When it doesn't fit one line, the model
 * moves to a line of its own rather than the line breaking after the dot.
 */
@Composable
private fun GeneratedLine(latencyMs: Long?, model: String?) {
    val colors = WellnessTheme.colors
    val style = MaterialTheme.typography.bodySmall
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val available = with(LocalDensity.current) { (maxWidth - IconLineIndent).roundToPx() }
        val oneLine = generatedLine(latencyMs, model)
        val fits = remember(oneLine, available, style) {
            measurer.measure(oneLine, style, softWrap = false, maxLines = 1).size.width <= available
        }
        IconLine(
            icon = Icons.Rounded.Memory,
            iconTint = colors.textTertiary,
            text = if (fits) oneLine else generatedLine(latencyMs, model, separator = "\n"),
        )
    }
}

/** A 16 dp icon leading a caption; the icon stays with the first line when the caption wraps. */
@Composable
private fun IconLine(icon: ImageVector, iconTint: Color, text: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodySmall
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    Row(modifier) {
        Box(Modifier.height(lineHeight), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(IconLineIndent - 16.dp))
        Text(text = text, style = style, color = WellnessTheme.colors.textTertiary)
    }
}

/**
 * "Was this helpful?" with its two answers at the end of the same line, as light pills that
 * move under the question when the line is too short; the way out stays in the footer.
 * After Helpful a caption confirms where the nudge went ([savedNote]).
 */
@Composable
private fun FeedbackSection(
    feedback: Feedback,
    savedNote: String,
    enabled: Boolean,
    onFeedback: (Feedback) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    Column(modifier) {
        QuestionAndAnswers(
            question = {
                Text(
                    text = "Was this helpful?",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
            },
        ) {
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RatingPill(
                    text = "Helpful",
                    icon = Icons.Outlined.ThumbUp,
                    selectedIcon = Icons.Rounded.ThumbUp,
                    selected = feedback == Feedback.Helpful,
                    selectedLook = RatingLook(
                        fill = colors.tint(colors.success).compositeOver(colors.controlFill),
                        border = colors.success,
                        content = colors.successText,
                    ),
                    enabled = enabled,
                    onClick = { onFeedback(Feedback.Helpful) },
                )
                // A mild opinion, not an error: chosen, it turns a firmer neutral, never red.
                RatingPill(
                    text = "Not really",
                    icon = Icons.Outlined.ThumbDown,
                    selectedIcon = Icons.Rounded.ThumbDown,
                    selected = feedback == Feedback.NotHelpful,
                    selectedLook = RatingLook(
                        fill = colors.textPrimary.copy(alpha = 0.08f).compositeOver(colors.controlFill),
                        border = colors.textSecondary,
                        content = colors.textPrimary,
                    ),
                    enabled = enabled,
                    onClick = { onFeedback(Feedback.NotHelpful) },
                )
            }
        }
        AnimatedVisibility(
            visible = feedback == Feedback.Helpful,
            enter = fadeIn(captionIn()) + slideInVertically(captionIn()) { it / 2 },
            exit = fadeOut(tween(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing)),
        ) {
            IconLine(
                icon = Icons.Rounded.CheckCircle,
                iconTint = colors.success,
                text = savedNote,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * [question] at the start of a line and [answers] at its end, or when both don't fit at their
 * natural widths, [answers] on a line of their own below, from the start.
 */
@Composable
private fun QuestionAndAnswers(question: @Composable () -> Unit, answers: @Composable () -> Unit) {
    Layout(contents = listOf(question, answers), modifier = Modifier.fillMaxWidth()) { (questionSlot, answersSlot), constraints ->
        val width = constraints.maxWidth
        val gap = 12.dp.roundToPx()
        val answersPlaceable = answersSlot.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val questionMeasurable = questionSlot.first()
        val oneLine = questionMeasurable.maxIntrinsicWidth(Constraints.Infinity) + gap + answersPlaceable.width <= width
        if (oneLine) {
            val questionPlaceable = questionMeasurable.measure(Constraints(maxWidth = width - gap - answersPlaceable.width))
            val height = maxOf(questionPlaceable.height, answersPlaceable.height)
            layout(width, height) {
                questionPlaceable.placeRelative(0, (height - questionPlaceable.height) / 2)
                answersPlaceable.placeRelative(width - answersPlaceable.width, (height - answersPlaceable.height) / 2)
            }
        } else {
            val questionPlaceable = questionMeasurable.measure(Constraints(maxWidth = width))
            // The answers' touch targets pad their pills; they bring the gap below the question.
            layout(width, questionPlaceable.height + answersPlaceable.height) {
                questionPlaceable.placeRelative(0, 0)
                answersPlaceable.placeRelative(0, questionPlaceable.height)
            }
        }
    }
}

/** How a chosen rating looks: its fill, border, and label and icon color. */
private class RatingLook(val fill: Color, val border: Color, val content: Color)

/**
 * One of the two ratings: a 40 dp pill in a 48 dp touch target that, once chosen, takes
 * [selectedLook] and swaps its outlined thumb for a filled one, so the choice doesn't rest on
 * color alone. A light control: a paper pill without the shadow of the footer's buttons. A tap
 * springs (0.92 back to 1) with a confirming haptic.
 */
@Composable
private fun RatingPill(
    text: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    selected: Boolean,
    selectedLook: RatingLook,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(1f) }
    val spec = tween<Color>(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing)
    val fill by animateColorAsState(if (selected) selectedLook.fill else colors.controlFill, spec, "ratingFill")
    val border by animateColorAsState(if (selected) selectedLook.border else colors.controlBorder, spec, "ratingBorder")
    val content by animateColorAsState(
        targetValue = when {
            !enabled -> colors.textDisabled
            selected -> selectedLook.content
            else -> colors.textPrimary
        },
        animationSpec = spec,
        label = "ratingContent",
    )
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = RatingHeight)
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
            }
            .clip(WellnessShapes.Pill)
            .background(fill)
            .border(1.dp, border, WellnessShapes.Pill)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = {
                    view.performHapticFeedback(ConfirmHaptic)
                    scope.launch {
                        press.snapTo(0.92f)
                        press.animateTo(1f, spring(dampingRatio = 0.5f))
                    }
                    onClick()
                },
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "Your nudge · 9:41 AM" for today's nudges; older ones lead with their day, e.g. "Yesterday · 9:32 PM". */
internal fun nudgeEyebrow(ts: Long, clock: WellnessClock): String {
    val sameDay = clock.dateTime(ts).toLocalDate() == clock.dateTime().toLocalDate()
    val day = if (sameDay) "Your nudge" else dayLabel(ts, clock)
    return "$day · ${timeLabel(ts, clock)}"
}

/**
 * Joins the last two words with a no-break space, so the text never ends on a line holding one
 * short word. A long last word is left alone: carrying another word down with it costs more.
 */
internal fun String.withoutWidow(maxLastWord: Int = 10): String {
    val text = trimEnd()
    val gap = text.indexOfLast { it.isWhitespace() }
    if (gap <= 0 || text[gap] != ' ' || text.length - gap - 1 > maxLastWord) return text
    return text.substring(0, gap) + NoBreakSpace + text.substring(gap + 1)
}

/**
 * How the nudge was made, naming the model as people know it ("SmolLM2 360M"); the time is
 * left out when it isn't known. A no-break space keeps "4.2 s" whole; [separator] joins the
 * model on, " · " on one line or a line break.
 */
internal fun generatedLine(latencyMs: Long?, model: String?, separator: String = " · "): String = buildString {
    append("Generated on this phone")
    if (latencyMs != null) append(" in ").append(formatSeconds(latencyMs).replace(' ', NoBreakSpace))
    if (!model.isNullOrBlank()) append(separator).append(modelDisplayName(model))
}

internal val ResultOrbSize = 44.dp

// The words start once the card has mostly risen in.
private const val RevealDelayMillis = 450

// The hero card: roomier than other cards, and its light, softer on paper.
private val HeroPadding = 24.dp
private const val HeroGlowDark = 0.22f
private const val HeroGlowLight = 0.12f

private val RatingHeight = 40.dp

/** From the start of an icon line to its text: the 16 dp icon and an 8 dp gap. */
private val IconLineIndent = 24.dp

private const val NoBreakSpace = ' '

private val ConfirmHaptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    HapticFeedbackConstants.CONFIRM
} else {
    HapticFeedbackConstants.VIRTUAL_KEY
}

// The caption follows the tap a beat later.
private fun <T> captionIn() = tween<T>(WellnessMotion.ScreenMillis, delayMillis = 100, easing = WellnessMotion.Easing)
