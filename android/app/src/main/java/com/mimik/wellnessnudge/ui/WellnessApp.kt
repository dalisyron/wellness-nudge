package com.mimik.wellnessnudge.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mimik.wellnessnudge.api.NudgeApi
import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.bootstrap.RuntimeInfo
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.data.SharedPrefsLatencyStore
import com.mimik.wellnessnudge.ui.navigation.MainScaffold
import com.mimik.wellnessnudge.ui.setup.SetupRoute
import com.mimik.wellnessnudge.ui.setup.SplashRoute
import com.mimik.wellnessnudge.ui.theme.WellnessMotion

/**
 * Routes on the bootstrap state: the splash while the runtime starts, setup while signing
 * in, deploying and downloading models (or after a failure), and the main app once Ready.
 * Stages crossfade; updates within a stage (download progress) don't.
 */
@Composable
fun WellnessApp(
    bootstrap: BootstrapState,
    onRetry: () -> Unit,
    onRetryModel: (modelId: String) -> Unit,
    onContinue: () -> Unit,
    loadRuntimeInfo: suspend () -> RuntimeInfo,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = bootstrap,
        modifier = modifier,
        contentKey = { it.stage },
        transitionSpec = {
            fadeIn(tween(WellnessMotion.RevealMillis)) togetherWith fadeOut(tween(WellnessMotion.RevealMillis))
        },
        label = "bootstrapStage",
    ) { state ->
        when (state.stage) {
            AppStage.Splash -> SplashRoute()
            AppStage.Setup -> SetupRoute(
                state = state,
                onRetry = onRetry,
                onRetryModel = onRetryModel,
                onContinue = onContinue,
            )
            AppStage.Main -> MainApp(ready = state as BootstrapState.Ready, loadRuntimeInfo = loadRuntimeInfo)
        }
    }
}

@Composable
private fun MainApp(ready: BootstrapState.Ready, loadRuntimeInfo: suspend () -> RuntimeInfo) {
    val context = LocalContext.current.applicationContext
    val holder: NudgeRepositoryHolder = viewModel()
    val repository = remember(ready) {
        holder.repositoryFor(ready) {
            NudgeRepository(
                api = NudgeApi.create(ready.mimBaseUrl, ready.apiKey),
                latencies = SharedPrefsLatencyStore(context),
            )
        }
    }
    MainScaffold(repository = repository, loadRuntimeInfo = loadRuntimeInfo)
}

private enum class AppStage { Splash, Setup, Main }

private val BootstrapState.stage: AppStage
    get() = when (this) {
        BootstrapState.NotStarted -> AppStage.Splash
        is BootstrapState.Step -> if (phase == BootstrapState.Phase.START_RUNTIME) AppStage.Splash else AppStage.Setup
        is BootstrapState.Setup, is BootstrapState.Failed -> AppStage.Setup
        is BootstrapState.Ready -> AppStage.Main
    }

/** Keeps one app-scoped [NudgeRepository] across configuration changes such as a theme switch. */
internal class NudgeRepositoryHolder : ViewModel() {
    private var ready: BootstrapState.Ready? = null
    private var repository: NudgeRepository? = null

    fun repositoryFor(state: BootstrapState.Ready, create: () -> NudgeRepository): NudgeRepository {
        repository?.let { if (ready == state) return it }
        repository?.close()
        return create().also {
            repository = it
            ready = state
        }
    }

    override fun onCleared() {
        repository?.close()
    }
}
