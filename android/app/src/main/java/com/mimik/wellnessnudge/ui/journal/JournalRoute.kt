package com.mimik.wellnessnudge.ui.journal

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.ui.format.LocalWellnessClock

/**
 * Journal tab: [JournalScreen] backed by a [JournalViewModel]. The history reloads quietly
 * each time the tab comes back into view: switching tabs, backing out of a nudge, or
 * returning to the app.
 *
 * @param onOpenNudge opens a saved nudge (`nudge/saved/{id}`).
 * @param onCreateNudge switches to the Today tab (empty-state action).
 * @param contentPadding bottom space taken by the floating tab bar and the navigation bar
 *   under it (or the keyboard, while it is taller); the list scrolls under it.
 */
@Composable
fun JournalRoute(
    repository: NudgeRepository,
    onOpenNudge: (id: String) -> Unit,
    onCreateNudge: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val zone = LocalWellnessClock.current.zone
    val viewModel: JournalViewModel = viewModel(
        factory = viewModelFactory { initializer { JournalViewModel(repository, zone) } },
    )
    // The clock follows time zone changes; the days regroup with it.
    LaunchedEffect(zone) { viewModel.setZone(zone) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.reload() }
    JournalScreen(
        state = state,
        onSelectFilter = viewModel::selectFilter,
        onOpenNudge = onOpenNudge,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::reload,
        onCreateNudge = onCreateNudge,
        contentPadding = contentPadding,
        modifier = modifier,
        onMessageShown = viewModel::onMessageShown,
    )
}
