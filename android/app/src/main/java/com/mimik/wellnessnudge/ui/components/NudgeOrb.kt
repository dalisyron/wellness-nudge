package com.mimik.wellnessnudge.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import com.mimik.wellnessnudge.ui.theme.Daybreak
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.theme.rememberAnimationsEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** How the orb moves: a slow orbit at rest, a quick breathing orbit while the model works. */
enum class OrbMode { Idle, Thinking, Still }

/**
 * The on-device AI, drawn as a light source: Daybreak-colored light orbiting inside a
 * sphere, a specular highlight and a soft halo. [size] is the sphere's diameter; the halo
 * spills past it without taking layout space.
 *
 * One draw pass with brushes built once per size; frames only move them, so animating
 * allocates nothing. Shows a still pose for [OrbMode.Still], in previews and snapshot
 * tests, and when the user turned animations off.
 */
@Composable
fun NudgeOrb(
    size: Dp,
    mode: OrbMode = OrbMode.Idle,
    modifier: Modifier = Modifier,
) {
    val animate = mode != OrbMode.Still && rememberAnimationsEnabled()
    val energy = animateFloatAsState(
        targetValue = if (mode == OrbMode.Thinking) 1f else 0f,
        animationSpec = tween(900, easing = WellnessMotion.Easing),
        label = "orbEnergy",
    )
    val clock = remember { OrbClock() }
    if (animate) {
        LaunchedEffect(clock) {
            var last = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    clock.advance(seconds = ((now - last) / 1e9f).coerceIn(0f, 0.1f), energy = energy.value)
                    last = now
                }
            }
        }
    }
    OrbCanvas(
        size = size,
        orbit = { clock.orbit },
        breath = { clock.breath },
        energy = { energy.value },
        modifier = modifier,
    )
}

/** One orb frame. The providers are read while drawing, so animating them only redraws. */
@Composable
internal fun OrbCanvas(
    size: Dp,
    orbit: () -> Float,
    breath: () -> Float,
    energy: () -> Float,
    modifier: Modifier = Modifier,
) {
    val haloAlpha = WellnessTheme.colors.haloAlpha
    Spacer(
        modifier
            .size(size)
            .drawWithCache {
                val brushes = OrbBrushes(radius = this.size.minDimension / 2f, haloAlpha = haloAlpha)
                onDrawBehind { drawOrb(brushes, orbit(), breath(), energy()) }
            },
    )
}

/** Animation time, read only while drawing so frames never recompose. */
@Stable
private class OrbClock {
    /** Seconds along the orbit; runs 4x faster while thinking (the 12 s loop becomes 3 s). */
    var orbit by mutableFloatStateOf(StillPoseSeconds)
        private set

    /** Real seconds, for the breathing pulse. */
    var breath by mutableFloatStateOf(0f)
        private set

    fun advance(seconds: Float, energy: Float) {
        orbit = (orbit + seconds * (1f + 3f * energy)) % OrbitWrapSeconds
        breath = (breath + seconds) % BreathWrapSeconds
    }
}

/**
 * One colored light inside the sphere. The lights keep their places around a slowly
 * turning "warm axis" (coral and honey on one side, iris opposite, orchid between) and
 * sway around it, so the orb always reads as one sunrise gradient rather than loose spots.
 * Sizes and distances are fractions of the radius; angles are radians, clockwise from 3 o'clock.
 */
private class Blob(
    val color: Color,
    /** Color at the very center of the light, e.g. a paler, hotter honey. */
    val core: Color,
    val alpha: Float,
    val size: Float,
    val distance: Float,
    /** Where the light sits relative to the warm axis. */
    val offset: Float,
    /** Angular sway around [offset] and its period in seconds. */
    val sway: Float,
    val swayPeriod: Float,
    /** Relative in-and-out drift of [distance] and its period in seconds. */
    val drift: Float,
    val driftPeriod: Float,
)

private val HoneyCore = Color(0xFFFFE3AE)
private val BodyLight = Color(0xFFA395FF)
private val BodyEdge = Color(0xFF5D4CE6)

// Every period divides 48 s, and the orbit wraps at a multiple of that, so the loop is seamless.
private val Blobs = arrayOf(
    Blob(Daybreak.Iris, Daybreak.Iris, 0.9f, 1.35f, 0.46f, offset = 3.3f, sway = 0.35f, swayPeriod = 48f, drift = 0.12f, driftPeriod = 16f),
    Blob(Daybreak.Orchid, Daybreak.Orchid, 0.8f, 1.15f, 0.42f, offset = -1.6f, sway = 0.45f, swayPeriod = 24f, drift = 0.15f, driftPeriod = 12f),
    Blob(Daybreak.Coral, Daybreak.Coral, 0.9f, 1.1f, 0.38f, offset = 0f, sway = 0.2f, swayPeriod = 16f, drift = 0.18f, driftPeriod = 9.6f),
    Blob(Daybreak.Honey, HoneyCore, 0.85f, 0.72f, 0.5f, offset = 0.4f, sway = 0.5f, swayPeriod = 12f, drift = 0.2f, driftPeriod = 8f),
)

/** Everything the orb draws, built once per size around the origin and moved with translations. */
private class OrbBrushes(val radius: Float, haloAlpha: Float) {
    /** Cool bloom around the whole sphere. */
    val halo = Brush.radialGradient(
        0f to Daybreak.Iris.copy(alpha = haloAlpha),
        EdgeStop to Daybreak.Iris.copy(alpha = haloAlpha),
        0.66f to Daybreak.Iris.copy(alpha = haloAlpha * 0.6f),
        0.78f to Daybreak.Iris.copy(alpha = haloAlpha * 0.26f),
        0.9f to Daybreak.Iris.copy(alpha = haloAlpha * 0.07f),
        1f to Daybreak.Iris.copy(alpha = 0f),
        center = Offset.Zero,
        radius = radius * HaloScale,
    )

    /** Warm light spilling past the edge on the coral side; it travels with the warm axis. */
    val warmHalo = Brush.radialGradient(
        0f to Daybreak.Coral.copy(alpha = haloAlpha * 0.5f),
        0.45f to Daybreak.Coral.copy(alpha = haloAlpha * 0.22f),
        1f to Daybreak.Coral.copy(alpha = 0f),
        center = Offset.Zero,
        radius = radius * WarmHaloScale,
    )

    val body = Brush.radialGradient(
        0f to BodyLight,
        0.6f to Daybreak.Iris,
        1f to BodyEdge,
        center = Offset(-0.3f * radius, -0.35f * radius),
        radius = radius * 1.45f,
    )

    val blobs = Array(Blobs.size) { i ->
        val blob = Blobs[i]
        Brush.radialGradient(
            0f to blob.core.copy(alpha = blob.alpha),
            0.3f to blob.color.copy(alpha = blob.alpha * 0.9f),
            0.65f to blob.color.copy(alpha = blob.alpha * 0.38f),
            1f to blob.color.copy(alpha = 0f),
            center = Offset.Zero,
            radius = radius * blob.size,
        )
    }

    /** Soft specular sheen, top left. */
    val highlight = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.3f),
        0.5f to Color.White.copy(alpha = 0.1f),
        1f to Color.White.copy(alpha = 0f),
        center = Offset.Zero,
        radius = radius * 0.6f,
    )

    /**
     * Glassy rim light: centered slightly below right, the gradient only reaches the edge
     * on the upper left, drawing a thin crescent there.
     */
    val rim = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0f),
        0.86f to Color.White.copy(alpha = 0f),
        1f to Color.White.copy(alpha = 0.22f),
        center = Offset(0.05f * radius, 0.065f * radius),
        radius = radius,
    )
}

private fun DrawScope.drawOrb(brushes: OrbBrushes, orbit: Float, breath: Float, energy: Float) {
    val r = brushes.radius
    val pulse = 1f + BreathAmplitude * sin(TwoPi * breath / BreathSeconds) * energy
    val warmAxis = WarmAxisStart + TwoPi * orbit / IdleLoopSeconds
    translate(center.x, center.y) {
        scale(pulse, pivot = Offset.Zero) {
            scale(1f + 0.12f * energy, pivot = Offset.Zero) {
                drawCircle(brushes.halo, radius = r * HaloScale, center = Offset.Zero)
            }
            val wx = cos(warmAxis) * 0.55f * r
            val wy = sin(warmAxis) * 0.55f * r
            translate(wx, wy) { drawCircle(brushes.warmHalo, radius = r * WarmHaloScale, center = Offset.Zero) }
            drawCircle(brushes.body, radius = r, center = Offset.Zero)
            // Each light is the sphere filled with a gradient centered on the light's
            // position: translating moves the gradient, the counter-offset keeps the circle.
            for (i in Blobs.indices) {
                val blob = Blobs[i]
                val angle = warmAxis + blob.offset + blob.sway * sin(TwoPi * orbit / blob.swayPeriod)
                val distance = blob.distance * r * (1f + blob.drift * sin(TwoPi * orbit / blob.driftPeriod + blob.offset))
                val dx = cos(angle) * distance
                val dy = sin(angle) * distance
                translate(dx, dy) { drawCircle(brushes.blobs[i], radius = r, center = Offset(-dx, -dy)) }
            }
            val hx = HighlightX * r
            val hy = HighlightY * r
            translate(hx, hy) { drawCircle(brushes.highlight, radius = r, center = Offset(-hx, -hy)) }
            drawCircle(brushes.rim, radius = r, center = Offset.Zero)
        }
    }
}

private const val TwoPi = (2 * PI).toFloat()
private const val IdleLoopSeconds = 12f

// All light periods divide 48 s, so wrapping at a multiple of it is seamless.
private const val OrbitWrapSeconds = 480f
private const val BreathSeconds = 3.2f
private const val BreathWrapSeconds = BreathSeconds * 100
private const val BreathAmplitude = 0.06f
private const val HaloScale = 1.75f
private const val WarmHaloScale = 1.05f

// Where the orb's edge falls within the halo gradient.
private const val EdgeStop = 1f / HaloScale
private const val HighlightX = -0.36f
private const val HighlightY = -0.42f

// Still pose: warm light low on the right, iris upper left, like a sunrise.
private const val WarmAxisStart = 0.85f
private const val StillPoseSeconds = 0f
