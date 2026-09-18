package com.mimik.wellnessnudge.snapshots

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.ui.journal.JournalContent
import com.mimik.wellnessnudge.ui.journal.JournalFilter
import com.mimik.wellnessnudge.ui.journal.JournalScreen
import com.mimik.wellnessnudge.ui.journal.JournalUiState
import com.mimik.wellnessnudge.ui.navigation.TopLevelTab
import org.junit.Rule
import org.junit.Test

/** The Journal tab in each state, under the floating tab bar, in the dark and light themes. */
class JournalScreenTest {

    @get:Rule
    val paparazzi = wellnessPaparazzi()

    @Test
    fun loaded() = paparazzi.snapshotThemes("journal") { Journal(ListsPreviewData.journal()) }

    @Test
    fun helpful() = paparazzi.snapshotThemes("journal_helpful") {
        Journal(ListsPreviewData.journal(JournalFilter.Helpful))
    }

    /** Yesterday's header pinned under the status bar, with its first entry sliding beneath. */
    @Test
    fun scrolled() = paparazzi.snapshotThemes("journal_scrolled") {
        val offset = with(LocalDensity.current) { 56.dp.roundToPx() }
        // Items: title, filters, Today's header and entry, then Yesterday's header and entries.
        Journal(ListsPreviewData.journal(), rememberLazyListState(5, offset))
    }

    /** A filter that matches nothing while other nudges exist. */
    @Test
    fun noMatches() = paparazzi.snapshotThemes("journal_no_matches") {
        val rated = ListsPreviewData.history.filter { it.helpful != Feedback.NotHelpful.wireValue }
        Journal(ListsPreviewData.journal(JournalFilter.NotHelpful, rated))
    }

    @Test
    fun empty() = paparazzi.snapshotThemes("journal_empty") {
        Journal(ListsPreviewData.journal(history = emptyList()))
    }

    @Test
    fun loading() = paparazzi.snapshotThemes("journal_loading") { Journal(JournalUiState()) }

    @Test
    fun error() = paparazzi.snapshotThemes("journal_error") {
        Journal(JournalUiState(content = JournalContent.Failed))
    }

    /** 150% text on a taller frame: nothing may clip, and the filters wrap. */
    @Test
    fun largeText() {
        paparazzi.unsafeUpdateConfig(deviceConfig = Pixel9ProXL.copy(fontScale = 1.5f, screenHeight = 2800))
        paparazzi.snapshotThemes("journal_large_text") { Journal(ListsPreviewData.journal()) }
    }
}

@Composable
private fun Journal(state: JournalUiState, listState: LazyListState = rememberLazyListState()) {
    TabFrame(TopLevelTab.Journal) { contentPadding ->
        JournalScreen(
            state = state,
            onSelectFilter = {},
            onOpenNudge = {},
            onRefresh = {},
            onRetry = {},
            onCreateNudge = {},
            contentPadding = contentPadding,
            listState = listState,
        )
    }
}
