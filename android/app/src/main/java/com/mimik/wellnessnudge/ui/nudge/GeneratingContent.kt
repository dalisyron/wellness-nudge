package com.mimik.wellnessnudge.ui.nudge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.components.Eyebrow
import com.mimik.wellnessnudge.ui.components.OrbMode
import com.mimik.wellnessnudge.ui.format.formatSeconds
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.theme.rememberAnimationsEnabled
import com.mimik.wellnessnudge.ui.theme.tabular

/**
 * The model at work: the thinking orb, which model runs where, the time so far, and the
 * signals it is reading, arriving one by one.
 */
@Composable
internal fun GeneratingContent(content: NudgeContent.Generating, orb: OrbSlot) {
    val colors = WellnessTheme.colors
    val typography = MaterialTheme.typography
    val appearance = rememberCascade(content.signals.size)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = WellnessSpacing.ScreenMargin, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        orb(ThinkingOrbSize, OrbMode.Thinking, Modifier)
        // Clear of the breathing halo, which spills 0.4 × the orb's size.
        Spacer(Modifier.height(44.dp))
        Text(
            text = "Thinking on-device",
            modifier = Modifier.semantics { heading() },
            style = typography.displaySmall.copy(lineBreak = LineBreak.Heading),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            // No-break spaces before the dots: a wrapped line ends on a dot rather than starting with one.
            text = "SmolLM2\u00A0· 360M parameters\u00A0· running locally on mimOE",
            // Balanced lines: centered text that wraps shouldn't leave one word on a line.
            style = typography.bodySmall.copy(lineBreak = LineBreak.Heading),
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = formatSeconds(content.elapsedMs),
            style = typography.labelLarge.tabular(),
            color = colors.textTertiary,
        )
        if (content.signals.isNotEmpty()) {
            Spacer(Modifier.height(40.dp))
            Eyebrow(
                text = "Reading your signals",
                modifier = Modifier.graphicsLayer { alpha = appearance(0) },
            )
            Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
            SignalChips(
                signals = content.signals,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                appearance = appearance,
            )
        }
    }
}

/**
 * How far item `index` of [count] has faded in (0 to 1): items arrive [CascadeStaggerMillis]
 * apart once the stage has settled. Read it while drawing; with animations off all are in.
 */
@Composable
private fun rememberCascade(count: Int): (index: Int) -> Float {
    val animate = rememberAnimationsEnabled()
    val total = CascadeDelayMillis + (count - 1).coerceAtLeast(0) * CascadeStaggerMillis + CascadeFadeMillis
    val elapsed = remember { Animatable(if (animate) 0f else total.toFloat()) }
    LaunchedEffect(total) {
        val remaining = (total - elapsed.value.toInt()).coerceAtLeast(0)
        elapsed.animateTo(total.toFloat(), tween(remaining, easing = LinearEasing))
    }
    return { index ->
        val progress = (elapsed.value - CascadeDelayMillis - index * CascadeStaggerMillis) / CascadeFadeMillis
        WellnessMotion.Easing.transform(progress.coerceIn(0f, 1f))
    }
}

internal val ThinkingOrbSize = 200.dp

private const val CascadeDelayMillis = 400
private const val CascadeStaggerMillis = 200
private const val CascadeFadeMillis = 300
