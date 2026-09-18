package com.mimik.wellnessnudge.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

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
 * False in previews and snapshot tests, and when the user turned animations off
 * (animator duration scale 0). Decorative loops render a static pose instead.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    if (LocalInspectionMode.current) return false
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}
