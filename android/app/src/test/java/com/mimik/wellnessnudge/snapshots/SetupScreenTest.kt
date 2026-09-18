package com.mimik.wellnessnudge.snapshots

import androidx.compose.runtime.Composable
import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.ui.preview.PreviewData
import com.mimik.wellnessnudge.ui.setup.SetupScreen
import com.mimik.wellnessnudge.ui.setup.SplashRoute
import com.mimik.wellnessnudge.ui.setup.toSetupUiState
import org.junit.Rule
import org.junit.Test

/** The splash and every stage of first-run setup, from sign-in to the last model. */
class SetupScreenTest {

    @get:Rule
    val paparazzi = wellnessPaparazzi()

    @Test
    fun splash() = paparazzi.snapshotThemes("splash") { SplashRoute() }

    @Test
    fun signIn() = paparazzi.snapshotThemes("setup_sign_in") { Setup(PreviewData.stepSignIn) }

    @Test
    fun deployMim() = paparazzi.snapshotThemes("setup_deploy_mim") { Setup(PreviewData.stepDeployMim) }

    /** The nudge writer at 23% while the goal classifier waits its turn. */
    @Test
    fun downloading() = paparazzi.snapshotThemes("setup_downloading") { Setup(PreviewData.setupDownloading) }

    @Test
    fun ready() = paparazzi.snapshotThemes("setup_ready") { Setup(PreviewData.setupReady) }

    @Test
    fun failedSignIn() = paparazzi.snapshotThemes("setup_failed_sign_in") { Setup(PreviewData.failedSignIn) }

    @Test
    fun modelFailed() = paparazzi.snapshotThemes("setup_model_failed") { Setup(PreviewData.setupModelFailed) }
}

@Composable
internal fun Setup(state: BootstrapState) {
    SetupScreen(state = state.toSetupUiState(), onRetry = {}, onRetryModel = {}, onContinue = {})
}
