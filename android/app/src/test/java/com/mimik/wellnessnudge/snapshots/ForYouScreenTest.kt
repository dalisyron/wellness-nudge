package com.mimik.wellnessnudge.snapshots

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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

    /** The first card scrolled partly away: it dissolves into the edge under the status bar. */
    @Test
    fun scrolled() = paparazzi.snapshotThemes("for_you_scrolled") {
        val offset = with(LocalDensity.current) { 120.dp.roundToPx() }
        ForYou(ListsPreviewData.forYou(), rememberLazyListState(1, offset))
    }

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
private fun ForYou(state: ForYouUiState, listState: LazyListState = rememberLazyListState()) {
    TabFrame(TopLevelTab.ForYou) { contentPadding ->
        ForYouScreen(
            state = state,
            onOpenNudge = {},
            onRefresh = {},
            onRetry = {},
            onCreateNudge = {},
            contentPadding = contentPadding,
            listState = listState,
        )
    }
}
