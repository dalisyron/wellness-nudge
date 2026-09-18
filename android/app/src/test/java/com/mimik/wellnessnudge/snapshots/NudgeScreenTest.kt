package com.mimik.wellnessnudge.snapshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.nudge.DeleteNudgeDialogContent
import com.mimik.wellnessnudge.ui.nudge.NudgePreviewData
import com.mimik.wellnessnudge.ui.nudge.NudgeScreen
import com.mimik.wellnessnudge.ui.nudge.NudgeUiState
import org.junit.Rule
import org.junit.Test

/** The Nudge screen in each stage, as the phone shows it, in the dark and light themes. */
class NudgeScreenTest {

    @get:Rule
    val paparazzi = wellnessPaparazzi()

    /** Every signal chip has arrived. */
    @Test
    fun generating() = paparazzi.snapshotThemes("nudge_generating") { Nudge(NudgePreviewData.generating) }

    /** A goal too long for one line wraps inside its chip. */
    @Test
    fun generatingLongGoal() = paparazzi.snapshotThemes("nudge_generating_long_goal") {
        Nudge(NudgePreviewData.generatingLongGoal)
    }

    /** A fresh nudge once its words have all arrived. */
    @Test
    fun result() = paparazzi.snapshotThemes("nudge_result") { Nudge(NudgePreviewData.result) }

    @Test
    fun resultHelpful() = paparazzi.snapshotThemes("nudge_result_helpful") { Nudge(NudgePreviewData.resultHelpful) }

    /** Opened from the Journal: rated "Not really", made before its timing was kept. */
    @Test
    fun savedNotHelpful() = paparazzi.snapshotThemes("nudge_saved_not_helpful") {
        Nudge(NudgePreviewData.savedNotHelpful)
    }

    /**
     * The confirmation drawn in place of its dialog window, over the dim the window casts: black
     * at the platform Material theme's backgroundDimAmount (0.6), in both themes.
     */
    @Test
    fun deleteDialog() = paparazzi.snapshotThemes("nudge_delete_dialog") {
        Box(Modifier.fillMaxSize()) {
            Nudge(NudgePreviewData.result)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                DeleteNudgeDialogContent(
                    onConfirm = {},
                    onDismiss = {},
                    modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
                )
            }
        }
    }

    @Test
    fun error() = paparazzi.snapshotThemes("nudge_error") { Nudge(NudgePreviewData.failed) }

    @Test
    fun loading() = paparazzi.snapshotThemes("nudge_loading") { Nudge(NudgePreviewData.loading) }

    /** At 150% font size the result scrolls between the top bar and a stacked footer; nothing clips. */
    @Test
    fun resultLargeText() = paparazzi.snapshotThemes("nudge_result_large_text") {
        LargeText { Nudge(NudgePreviewData.resultHelpful) }
    }

    @Test
    fun generatingLargeText() = paparazzi.snapshotThemes("nudge_generating_large_text") {
        LargeText { Nudge(NudgePreviewData.generating) }
    }
}

@Composable
private fun Nudge(state: NudgeUiState) {
    NudgeScreen(
        state = state,
        onBack = {},
        onDone = {},
        onTryAnother = {},
        onRetryLoad = {},
        onFeedback = {},
        onDeleteRequest = {},
        onDeleteConfirm = {},
        onDeleteDismiss = {},
        onRevealed = {},
        onMessageShown = {},
    )
}

/** The system font size at 150%, on the same phone. */
@Composable
private fun LargeText(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f), content = content)
}
