package com.mimik.wellnessnudge.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.GradientButton
import com.mimik.wellnessnudge.ui.components.ScreenTitle
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing

/**
 * One-time setup: sign-in and deploy steps (`Step` after START_RUNTIME), model downloads
 * (`Setup`) and failures (`Failed`). PLACEHOLDER until the Setup screen (spec 4.1)
 * replaces it; it only offers Continue and Retry so the app stays usable meanwhile.
 *
 * @param onRetry restarts bootstrap after a failure.
 * @param onRetryModel retries one failed model download by id.
 * @param onContinue enters the app once every model is ready.
 */
@Composable
fun SetupRoute(
    state: BootstrapState,
    onRetry: () -> Unit,
    onRetryModel: (modelId: String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DaybreakBackground(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = WellnessSpacing.ScreenMargin, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenTitle(eyebrow = "Placeholder", title = "SetupRoute", subtitle = state.toString())
            if (state is BootstrapState.Failed) {
                SecondaryButton(text = "Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            } else {
                val ready = state is BootstrapState.Setup && state.allReady
                GradientButton(
                    text = "Start using Wellness Nudge",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ready,
                    loading = !ready,
                )
            }
        }
    }
}
