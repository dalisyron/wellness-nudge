package com.mimik.wellnessnudge.ui.foryou

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mimik.wellnessnudge.data.NudgeRepository

/**
 * For you tab: [ForYouScreen] backed by a [ForYouViewModel]. The tips reload quietly each
 * time the tab comes back into view, so a nudge just marked helpful shows up here.
 *
 * @param onOpenNudge opens a saved nudge (`nudge/saved/{id}`) from its quote.
 * @param onCreateNudge switches to the Today tab (empty-state action).
 * @param contentPadding bottom space taken by the floating tab bar and the navigation bar
 *   under it (or the keyboard, while it is taller); the list scrolls under it.
 */
@Composable
fun ForYouRoute(
    repository: NudgeRepository,
    onOpenNudge: (id: String) -> Unit,
    onCreateNudge: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel: ForYouViewModel = viewModel(
        factory = viewModelFactory { initializer { ForYouViewModel(repository) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.reload() }
    ForYouScreen(
        state = state,
        onOpenNudge = onOpenNudge,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::reload,
        onCreateNudge = onCreateNudge,
        contentPadding = contentPadding,
        modifier = modifier,
        onMessageShown = viewModel::onMessageShown,
    )
}
