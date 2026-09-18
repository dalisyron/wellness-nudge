package com.mimik.wellnessnudge.ui.today

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.NudgeRepository

/**
 * The Today tab: [TodayScreen] wired to its [TodayViewModel], which checks the runtime again
 * each time the tab comes into view.
 *
 * @param onGenerate starts a generation and opens `nudge/new`. The shell drops repeated taps.
 * @param onOpenRuntime opens the runtime sheet (from the On-device pill).
 * @param contentPadding bottom space taken by the floating tab bar and the navigation bar
 *   under it or, while it is taller, by the keyboard (the bar hides while the keyboard is
 *   up). The screen pads its content and its sticky footer with it.
 */
@Composable
fun TodayRoute(
    repository: NudgeRepository,
    onGenerate: (NudgeRequest) -> Unit,
    onOpenRuntime: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: TodayViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TodayViewModel(repository, SampleDays(context)::next, createSavedStateHandle()) }
        },
    )
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onStart() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onStop() }
    TodayScreen(
        state = viewModel.state,
        onGoalChange = viewModel::setGoal,
        onSampleDay = viewModel::loadSampleDay,
        onOpenEditor = viewModel::openEditor,
        onMetricChange = viewModel::setMetric,
        onCloseEditor = viewModel::closeEditor,
        onGenerate = { onGenerate(viewModel.request()) },
        onOpenRuntime = onOpenRuntime,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}
