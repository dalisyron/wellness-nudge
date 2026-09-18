package com.mimik.wellnessnudge.ui.nudge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.NudgeRepository

/**
 * The full-screen nudge (spec 4.3): [NudgeScreen] driven by a [NudgeViewModel].
 *
 * @param nudgeId the saved nudge to show (`nudge/saved/{id}`), or null on `nudge/new`: render
 *   [NudgeRepository.generation] (Running: generating, Success: the result, Failed: the error).
 *   Idle means there is nothing to show and the shell pops the screen, so the last non-Idle
 *   state stays on screen while it leaves. Deleting doesn't reset the generation: once
 *   [NudgeRepository.delete] returns this calls [onBack], and the result stays put as it exits.
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
    val viewModel: NudgeViewModel = viewModel(
        factory = viewModelFactory { initializer { NudgeViewModel(repository, nudgeId) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leave by rememberUpdatedState(onBack)
    LaunchedEffect(state.deleted) {
        if (state.deleted) leave()
    }
    NudgeScreen(
        state = state,
        onBack = onBack,
        onDone = onDone,
        onTryAnother = onTryAnother,
        onRetryLoad = viewModel::retryLoad,
        onFeedback = viewModel::setFeedback,
        onDeleteRequest = viewModel::requestDelete,
        onDeleteConfirm = viewModel::delete,
        onDeleteDismiss = viewModel::dismissDelete,
        onRevealed = viewModel::onRevealed,
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
    )
}
