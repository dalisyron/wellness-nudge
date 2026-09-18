package com.mimik.wellnessnudge.ui.nudge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.ui.components.CircleIconButton
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.ScreenTitle
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing

/**
 * Full-screen nudge. PLACEHOLDER until the Nudge screen (spec 4.3) replaces it.
 *
 * @param nudgeId the saved nudge to show (`nudge/saved/{id}`), or null on `nudge/new`: render
 *   [NudgeRepository.generation] (Running: generating, Success: the result, Failed: the error).
 *   Idle means there is nothing to show and the shell pops the screen, so keep rendering the
 *   last non-Idle state while it leaves. Deleting doesn't reset the generation: after
 *   [NudgeRepository.delete] returns, call [onBack] and the result stays put while it exits.
 * @param onTryAnother regenerates with a request. On `nudge/saved/{id}` the shell swaps this
 *   screen for `nudge/new`; on `nudge/new` the generation simply turns Running again. The
 *   shell ignores it while the screen is entering or leaving.
 * @param onDone leaves the screen, like [onBack].
 */
@Composable
fun NudgeRoute(
    repository: NudgeRepository,
    nudgeId: String?,
    onBack: () -> Unit,
    onTryAnother: (NudgeRequest) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DaybreakBackground(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = WellnessSpacing.ScreenMargin, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = onBack)
            ScreenTitle(
                eyebrow = "Placeholder",
                title = "NudgeRoute",
                subtitle = nudgeId?.let { "Saved nudge $it" } ?: "New nudge (nudge/new)",
            )
        }
    }
}
