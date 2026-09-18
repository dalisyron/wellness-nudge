package com.mimik.wellnessnudge.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalInspectionMode
import kotlin.coroutines.CoroutineContext

/** Calm, eased motion. Springs are reserved for tactile feedback. */
object WellnessMotion {
    val Easing: Easing = FastOutSlowInEasing

    /** Small state changes: colors, chips, toggles. */
    const val SmallMillis = 200

    /** Screen transitions. */
    const val ScreenMillis = 350

    /** Content reveals. */
    const val RevealMillis = 600

    /** Crossfade between tabs. */
    const val TabFadeMillis = 220

    /** Number changes, e.g. when "Sample day" swaps the metrics. */
    const val ValueMillis = 500
}

/**
 * False in previews and snapshot tests, and while the user has animations turned off
 * (animator duration scale 0). Decorative loops render a static pose instead. It follows
 * the setting live: turning animations off stops loops already on screen.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    if (LocalInspectionMode.current) return false
    return rememberCoroutineScope().coroutineContext.animationsEnabled()
}

/**
 * Compose mirrors the system animator duration scale into the [MotionDurationScale] of every
 * window's coroutine context. The value is snapshot state, so reading it here is observed.
 */
internal fun CoroutineContext.animationsEnabled(): Boolean = (this[MotionDurationScale]?.scaleFactor ?: 1f) != 0f
