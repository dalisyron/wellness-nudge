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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.mimik.wellnessnudge.ui.theme.Daybreak
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * How the orb moves: a slow orbit at rest, a quick breathing orbit while the model works, a
 * still pose, or [Dimmed]: still and unlit, without its halo, for when the model couldn't
 * finish (it fades there over a reveal's length, e.g. as it lands above an error).
 */
enum class OrbMode { Idle, Thinking, Still, Dimmed }

/**
 * The on-device AI, drawn as a light source: Daybreak-colored light orbiting inside a
 * sphere, a specular highlight and a soft halo. [size] is the sphere's diameter.
 *
 * The halo spills past the bounds without taking layout space: 0.25 × [size] at rest and up
 * to 0.4 × [size] while [OrbMode.Thinking] breathes. Keep that much room between the orb and
 * any clipping ancestor (scroll containers, lazy lists, clipped cards, AnimatedContent), or
 * the glow ends in a hard edge; e.g. keep the generating orb outside the scrolling content.
 *
 * One draw pass with brushes built once per size; frames only move them, so animating
 * allocates nothing, and the orb has its own layer, so frames redraw nothing else. The
 * orbit runs at about 30 frames a second, thinking included, which lets the display lower
 * its refresh rate: the model runs on the same CPU, and on a Pixel 9 Pro XL drawing the
 * thinking orb at 120 Hz made nudges take nearly twice as long. The orbit rests while the orb
 * is scrolled out of view. Shows a still pose for [OrbMode.Still] and [OrbMode.Dimmed], in
 * previews and snapshot tests, and when the user turned animations off.
 */
@Composable
fun NudgeOrb(
    size: Dp,
    modifier: Modifier = Modifier,
    mode: OrbMode = OrbMode.Idle,
) {
    var onScreen by remember { mutableStateOf(true) }
    val animate = (mode == OrbMode.Idle || mode == OrbMode.Thinking) && onScreen && rememberAnimationsEnabled()
    val energy = animateFloatAsState(
        targetValue = if (mode == OrbMode.Thinking) 1f else 0f,
        animationSpec = tween(900, easing = WellnessMotion.Easing),
        label = "orbEnergy",
    )
    val dim = animateFloatAsState(
        targetValue = if (mode == OrbMode.Dimmed) 1f else 0f,
        animationSpec = tween(WellnessMotion.RevealMillis, easing = WellnessMotion.Easing),
        label = "orbDim",
    )
    val clock = remember { OrbClock() }
    if (animate) {
        LaunchedEffect(clock) {
            var last = withFrameNanos { it }
            while (true) {
                delay(SlowFrameMillis)
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
        dim = { dim.value },
        modifier = modifier.onScreenChanged { onScreen = it },
    )
}

/**
 * One orb frame. The providers are read while drawing, so animating them only redraws.
 * [dim] (0 to 1) takes the halo away and fades and desaturates the sphere.
 */
@Composable
internal fun OrbCanvas(
    size: Dp,
    orbit: () -> Float,
    breath: () -> Float,
    energy: () -> Float,
    modifier: Modifier = Modifier,
    dim: () -> Float = { 0f },
) {
    val haloAlpha = WellnessTheme.colors.haloAlpha
    Spacer(
        modifier
            .size(size)
            // No clip: the halo draws past the bounds.
            .graphicsLayer()
            .drawWithCache {
                val brushes = OrbBrushes(radius = this.size.minDimension / 2f, haloAlpha = haloAlpha)
                onDrawBehind { drawOrb(brushes, orbit(), breath(), energy(), dim()) }
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
 * turning "warm axis" (coral and honey on one side, iris opposite, orchid on both sides
 * between them) and sway around it, so the orb always reads as one sunrise gradient rather
 * than loose spots, and warm light never meets iris without orchid in between (the direct
 * mix is a dusty mauve). Sizes and distances are fractions of the radius; angles are
 * radians, clockwise from 3 o'clock.
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
private val BodyEdge = Color(0xFF6E5EF5)

// Every period divides 48 s, and the orbit wraps at a multiple of that, so the loop is seamless.
private val Blobs = arrayOf(
    Blob(Daybreak.Iris, Daybreak.Iris, 0.9f, 1.35f, 0.46f, offset = 3.3f, sway = 0.35f, swayPeriod = 48f, drift = 0.12f, driftPeriod = 16f),
    Blob(Daybreak.Orchid, Daybreak.Orchid, 0.8f, 1.15f, 0.42f, offset = -1.6f, sway = 0.45f, swayPeriod = 24f, drift = 0.15f, driftPeriod = 12f),
    // Mirrors the orchid light across the warm axis, bridging honey and iris.
    Blob(Daybreak.Orchid, Daybreak.Orchid, 0.7f, 1.0f, 0.45f, offset = 2.0f, sway = 0.35f, swayPeriod = 24f, drift = 0.15f, driftPeriod = 12f),
    Blob(Daybreak.Coral, Daybreak.Coral, 0.9f, 1.1f, 0.38f, offset = 0f, sway = 0.2f, swayPeriod = 16f, drift = 0.18f, driftPeriod = 9.6f),
    Blob(Daybreak.Honey, HoneyCore, 0.85f, 0.72f, 0.5f, offset = 0.4f, sway = 0.5f, swayPeriod = 12f, drift = 0.2f, driftPeriod = 8f),
)

/** Everything the orb draws, built once per size around the origin and moved with translations. */
private class OrbBrushes(val radius: Float, haloAlpha: Float) {
    /** Cool bloom around the whole sphere. */
    val halo = Brush.radialGradient(
        0f to Daybreak.Iris.copy(alpha = haloAlpha),
        EdgeStop to Daybreak.Iris.copy(alpha = haloAlpha),
        haloStop(0.21f) to Daybreak.Iris.copy(alpha = haloAlpha * 0.6f),
        haloStop(0.49f) to Daybreak.Iris.copy(alpha = haloAlpha * 0.26f),
        haloStop(0.77f) to Daybreak.Iris.copy(alpha = haloAlpha * 0.07f),
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

private fun DrawScope.drawOrb(brushes: OrbBrushes, orbit: Float, breath: Float, energy: Float, dim: Float) {
    val r = brushes.radius
    val pulse = 1f + BreathAmplitude * sin(TwoPi * breath / BreathSeconds) * energy
    val warmAxis = WarmAxisStart + TwoPi * orbit / IdleLoopSeconds
    val lit = 1f - dim
    translate(center.x, center.y) {
        scale(pulse, pivot = Offset.Zero) {
            if (lit > 0f) {
                scale(1f + 0.12f * energy, pivot = Offset.Zero) {
                    drawCircle(brushes.halo, radius = r * HaloScale, center = Offset.Zero, alpha = lit)
                }
                val wx = cos(warmAxis) * 0.55f * r
                val wy = sin(warmAxis) * 0.55f * r
                translate(wx, wy) { drawCircle(brushes.warmHalo, radius = r * WarmHaloScale, center = Offset.Zero, alpha = lit) }
            }
            if (dim > 0f) {
                // Unlit: the sphere drawn into a layer that drains most of its color and fades it.
                val paint = Paint().apply {
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1f - DimDesaturation * dim) })
                    alpha = 1f - DimFade * dim
                }
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(-r, -r, r, r), paint)
                    drawSphere(brushes, orbit, warmAxis)
                    canvas.restore()
                }
            } else {
                drawSphere(brushes, orbit, warmAxis)
            }
        }
    }
}

/** The sphere itself, centered on the origin: body, orbiting lights, highlight and rim. */
private fun DrawScope.drawSphere(brushes: OrbBrushes, orbit: Float, warmAxis: Float) {
    val r = brushes.radius
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

private const val TwoPi = (2 * PI).toFloat()
private const val IdleLoopSeconds = 12f

// All light periods divide 48 s, so wrapping at a multiple of it is seamless.
private const val OrbitWrapSeconds = 480f
private const val BreathSeconds = 3.2f
private const val BreathWrapSeconds = BreathSeconds * 100
private const val BreathAmplitude = 0.06f

// Dimmed: saturation falls to a quarter, the sphere to 70% opacity.
private const val DimDesaturation = 0.75f
private const val DimFade = 0.3f
private const val HaloScale = 1.5f
private const val WarmHaloScale = 1.05f

// Where the orb's edge falls within the halo gradient.
private const val EdgeStop = 1f / HaloScale
private const val HighlightX = -0.36f
private const val HighlightY = -0.42f

// Still pose: warm light low on the right, iris upper left, like a sunrise.
private const val WarmAxisStart = 0.85f
private const val StillPoseSeconds = 0f

/** A halo stop [fraction] of the way from the orb's edge to the end of the halo. */
private fun haloStop(fraction: Float) = EdgeStop + (1f - EdgeStop) * fraction
