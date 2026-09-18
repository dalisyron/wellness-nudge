package com.mimik.wellnessnudge.snapshots

import androidx.compose.runtime.Composable
import com.mimik.wellnessnudge.ui.foryou.ForYouContent
import com.mimik.wellnessnudge.ui.foryou.ForYouScreen
import com.mimik.wellnessnudge.ui.foryou.ForYouUiState
import com.mimik.wellnessnudge.ui.navigation.TopLevelTab
import com.mimik.wellnessnudge.ui.preview.PreviewData
import org.junit.Rule
import org.junit.Test

/** The For you tab in each state, under the floating tab bar, in the dark and light themes. */
class ForYouScreenTest {

    @get:Rule
    val paparazzi = wellnessPaparazzi()

    @Test
    fun loaded() = paparazzi.snapshotThemes("for_you") { ForYou(ListsPreviewData.forYou()) }

    @Test
    fun empty() = paparazzi.snapshotThemes("for_you_empty") { ForYou(ListsPreviewData.forYou(PreviewData.tipsEmpty)) }

    @Test
    fun loading() = paparazzi.snapshotThemes("for_you_loading") { ForYou(ForYouUiState()) }

    @Test
    fun error() = paparazzi.snapshotThemes("for_you_error") {
        ForYou(ForYouUiState(content = ForYouContent.Failed))
    }
}

@Composable
private fun ForYou(state: ForYouUiState) {
    TabFrame(TopLevelTab.ForYou) { contentPadding ->
        ForYouScreen(
            state = state,
            onOpenNudge = {},
            onRefresh = {},
            onRetry = {},
            onCreateNudge = {},
            contentPadding = contentPadding,
        )
    }
}
