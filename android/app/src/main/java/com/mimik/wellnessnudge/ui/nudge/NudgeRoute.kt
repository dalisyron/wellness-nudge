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
 * @param nudgeId the saved nudge to show (`nudge/{id}`), or null for the generation in
 *   progress (`nudge/new`, observe [NudgeRepository.generation]).
 * @param onTryAnother regenerates with a request; the shell swaps this screen for `nudge/new`.
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
