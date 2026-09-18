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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.data.feedback
import com.mimik.wellnessnudge.ui.components.CategoryBadge
import com.mimik.wellnessnudge.ui.components.Eyebrow
import com.mimik.wellnessnudge.ui.components.OrbMode
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.components.SectionHeader
import com.mimik.wellnessnudge.ui.components.WellnessCard
import com.mimik.wellnessnudge.ui.components.paperShadow
import com.mimik.wellnessnudge.ui.format.LocalWellnessClock
import com.mimik.wellnessnudge.ui.format.WellnessClock
import com.mimik.wellnessnudge.ui.format.dayLabel
import com.mimik.wellnessnudge.ui.format.formatSeconds
import com.mimik.wellnessnudge.ui.format.timeLabel
import com.mimik.wellnessnudge.ui.theme.Daybreak
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import kotlinx.coroutines.launch

/**
 * The nudge: the settled orb with its time and category, the hero card, what it was grounded
 * in and how it was made, the rating, and the way out. Scrolls between the top bar and the
 * pinned footer when it runs long, e.g. at large font sizes. [entrance] is the stage's
 * visibility scope, which staggers the sections in.
 */
@Composable
internal fun ResultContent(
    result: NudgeContent.Result,
    orb: OrbSlot,
    entrance: AnimatedVisibilityScope,
    snackbarHostState: SnackbarHostState,
    onFeedback: (Feedback) -> Unit,
    onTryAnother: (NudgeRequest) -> Unit,
    onDone: () -> Unit,
    onRevealed: (nudgeId: String) -> Unit,
) {
    val nudge = result.nudge
    Column(Modifier.fillMaxSize()) {
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
                    .padding(top = 20.dp, bottom = 24.dp),
            ) {
                ResultHeader(nudge, orb, entrance.entrance(order = 0))
                Spacer(Modifier.height(20.dp))
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
                FeedbackSection(nudge.feedback, onFeedback, entrance.entrance(order = 3))
            }
            // Material's snackbar pads itself by 12 dp; this lines it up with the screen margin.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = WellnessSpacing.ScreenMargin - 12.dp),
            )
        }
        StageFooter(entrance.entrance(order = 4)) {
            ButtonPair(
                first = {
                    SecondaryButton(
                        text = "Try another",
                        onClick = { onTryAnother(result.request) },
                        modifier = Modifier.heightIn(min = FooterButtonHeight),
                        icon = Icons.Rounded.Refresh,
                    )
                },
                second = {
                    SecondaryButton(
                        text = "Done",
                        onClick = onDone,
                        modifier = Modifier.heightIn(min = FooterButtonHeight),
                    )
                },
            )
        }
    }
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
 * The hero: the nudge set large in the serif, in a card lit by the Daybreak gradient. A fresh
 * nudge is announced to TalkBack as it arrives.
 */
@Composable
private fun NudgeCard(text: String, reveal: Boolean, onRevealed: () -> Unit, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    WellnessCard(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.5.dp, colors.daybreakBorder),
        glow = Daybreak.Orchid.copy(alpha = 0.18f),
    ) {
        WordRevealText(
            text = text.withoutWidow(),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            reveal = reveal,
            onRevealed = onRevealed,
            modifier = if (reveal) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
            delayMillis = RevealDelayMillis,
        )
    }
}

/** "Generated on this phone in 4.2 s · smollm2-360m". */
@Composable
private fun GeneratedLine(latencyMs: Long?, model: String?) {
    val colors = WellnessTheme.colors
    IconLine(icon = Icons.Rounded.Memory, iconTint = colors.textTertiary, text = generatedLine(latencyMs, model))
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
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = style, color = WellnessTheme.colors.textTertiary)
    }
}

@Composable
private fun FeedbackSection(feedback: Feedback, onFeedback: (Feedback) -> Unit, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    Column(modifier) {
        Text(
            text = "Was this helpful?",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(12.dp))
        ButtonPair(
            modifier = Modifier.selectableGroup(),
            first = {
                FeedbackButton(
                    text = "Helpful",
                    icon = Icons.Outlined.ThumbUp,
                    selectedIcon = Icons.Rounded.ThumbUp,
                    selected = feedback == Feedback.Helpful,
                    hue = colors.success,
                    selectedText = colors.successText,
                    onClick = { onFeedback(Feedback.Helpful) },
                )
            },
            second = {
                FeedbackButton(
                    text = "Not really",
                    icon = Icons.Outlined.ThumbDown,
                    selectedIcon = Icons.Rounded.ThumbDown,
                    selected = feedback == Feedback.NotHelpful,
                    hue = colors.danger,
                    selectedText = colors.dangerText,
                    onClick = { onFeedback(Feedback.NotHelpful) },
                )
            },
        )
        AnimatedVisibility(
            visible = feedback == Feedback.Helpful,
            enter = fadeIn(captionIn()) + slideInVertically(captionIn()) { it / 2 },
            exit = fadeOut(tween(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing)),
        ) {
            IconLine(
                icon = Icons.Rounded.CheckCircle,
                iconTint = colors.success,
                text = "Saved. It will show up in For you.",
                modifier = Modifier
                    .padding(top = 12.dp)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * One of the two ratings: a quiet pill that, once chosen, takes the status's tint, border and
 * text color, and swaps its outlined thumb for a filled one, so the choice doesn't rest on
 * color alone. A tap springs (0.92 back to 1) with a confirming haptic.
 */
@Composable
private fun FeedbackButton(
    text: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    selected: Boolean,
    hue: Color,
    selectedText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(1f) }
    val spec = tween<Color>(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing)
    // Opaque, so the light theme's paper shadow doesn't show through the tint.
    val fill by animateColorAsState(
        targetValue = if (selected) colors.tint(hue).compositeOver(colors.controlFill) else colors.controlFill,
        animationSpec = spec,
        label = "feedbackFill",
    )
    val border by animateColorAsState(if (selected) hue else colors.controlBorder, spec, "feedbackBorder")
    val content by animateColorAsState(if (selected) selectedText else colors.textPrimary, spec, "feedbackContent")
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
            }
            .paperShadow(colors, WellnessShapes.Pill, elevation = 3.dp)
            .clip(WellnessShapes.Pill)
            .background(fill)
            .border(1.dp, border, WellnessShapes.Pill)
            .selectable(
                selected = selected,
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
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
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
 * How the nudge was made; the time is left out when it isn't known. No-break spaces keep "4.2 s"
 * whole and a wrapped line from starting with the dot.
 */
internal fun generatedLine(latencyMs: Long?, model: String?): String = buildString {
    append("Generated on this phone")
    if (latencyMs != null) append(" in ").append(formatSeconds(latencyMs).replace(' ', NoBreakSpace))
    if (!model.isNullOrBlank()) append(NoBreakSpace).append("· ").append(model)
}

internal val ResultOrbSize = 44.dp

// The words start once the card has mostly risen in.
private const val RevealDelayMillis = 450

private const val NoBreakSpace = '\u00A0'

private val ConfirmHaptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    HapticFeedbackConstants.CONFIRM
} else {
    HapticFeedbackConstants.VIRTUAL_KEY
}

// The caption follows the tap a beat later.
private fun <T> captionIn() = tween<T>(WellnessMotion.ScreenMillis, delayMillis = 100, easing = WellnessMotion.Easing)
