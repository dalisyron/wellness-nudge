package com.mimik.wellnessnudge.ui.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mimik.wellnessnudge.bootstrap.BootstrapState

/**
 * One-time setup: sign-in and deploy steps (`Step` after START_RUNTIME), model downloads
 * (`Setup`) and failures (`Failed`). The activity's BootstrapViewModel is this screen's
 * state holder; the route maps its state for [SetupScreen].
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
    val uiState = remember(state) { state.toSetupUiState() }
    SetupScreen(
        state = uiState,
        onRetry = onRetry,
        onRetryModel = onRetryModel,
        onContinue = onContinue,
        modifier = modifier,
    )
}
