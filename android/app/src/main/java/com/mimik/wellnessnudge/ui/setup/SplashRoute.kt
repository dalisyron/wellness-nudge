package com.mimik.wellnessnudge.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.NudgeOrb
import com.mimik.wellnessnudge.ui.components.OrbMode
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * Shown while the mimOE runtime starts (`NotStarted`, `Step(START_RUNTIME)`): the thinking
 * orb at the very center of the screen, where the system splash showed the app icon, with
 * the wordmark and a caption below it. Stateless.
 */
@Composable
fun SplashRoute(modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    DaybreakBackground(modifier) {
        OrbAboveCaption(
            orb = { NudgeOrb(size = 120.dp, mode = OrbMode.Thinking) },
            caption = {
                Column(
                    Modifier.padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Wellness Nudge",
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Starting the on-device runtime…",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            },
        )
    }
}

/** Centers [orb] on the screen and hangs [caption] below it, clear of the thinking halo. */
@Composable
private fun OrbAboveCaption(orb: @Composable () -> Unit, caption: @Composable () -> Unit) {
    Layout(
        content = {
            orb()
            caption()
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val orbPlaceable = measurables[0].measure(loose)
        val captionPlaceable = measurables[1].measure(loose)
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            val orbTop = (height - orbPlaceable.height) / 2
            orbPlaceable.placeRelative((width - orbPlaceable.width) / 2, orbTop)
            captionPlaceable.placeRelative(
                x = (width - captionPlaceable.width) / 2,
                y = orbTop + orbPlaceable.height + CaptionGap.roundToPx(),
            )
        }
    }
}

private val CaptionGap = 44.dp
