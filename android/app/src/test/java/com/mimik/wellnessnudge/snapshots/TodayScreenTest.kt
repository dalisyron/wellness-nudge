package com.mimik.wellnessnudge.snapshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.mimik.wellnessnudge.ui.components.FloatingNavBar
import com.mimik.wellnessnudge.ui.components.RuntimeStatus
import com.mimik.wellnessnudge.ui.components.WellnessBottomSheetFrame
import com.mimik.wellnessnudge.ui.navigation.TopLevelTab
import com.mimik.wellnessnudge.ui.today.TodayEditor
import com.mimik.wellnessnudge.ui.today.TodayEditorContent
import com.mimik.wellnessnudge.ui.today.TodayPreviewData
import com.mimik.wellnessnudge.ui.today.TodayScreen
import com.mimik.wellnessnudge.ui.today.TodayUiState
import org.junit.Rule
import org.junit.Test

/** The Today tab under the floating tab bar, as the app shell shows it. */
class TodayScreenTest {

    @get:Rule
    val paparazzi = wellnessPaparazzi()

    /** At rest, no goal yet: the whole flow, down to the goal's placeholder and suggestions, sits above the button. */
    @Test
    fun default() = paparazzi.snapshotThemes("today") { Today(TodayPreviewData.default) }

    /** After the first "Sample day", scrolled to the end, where its goal selects a suggestion. */
    @Test
    fun sampleDay() = paparazzi.snapshotThemes("today_sample_day") {
        Today(TodayPreviewData.sampleDay, scrolledToEnd = true)
    }

    /** The runtime hasn't answered yet. */
    @Test
    fun starting() = paparazzi.snapshotThemes("today_starting") {
        Today(TodayPreviewData.default.copy(runtime = RuntimeStatus.Starting))
    }

    /** The runtime doesn't answer: a line above the button says so, with the way to the details. */
    @Test
    fun unavailable() = paparazzi.snapshotThemes("today_unavailable") {
        Today(TodayPreviewData.default.copy(runtime = RuntimeStatus.Error))
    }

    @Test
    fun goalFocused() = paparazzi.snapshotThemes("today_goal_focused") {
        Today(TodayPreviewData.typing, scrolledToEnd = true, focusGoal = true)
    }

    @Test
    fun sleepEditor() = paparazzi.snapshotThemes("today_sleep_editor") {
        Today(TodayPreviewData.default, editor = TodayEditor.Sleep)
    }

    @Test
    fun hrvEditor() = paparazzi.snapshotThemes("today_hrv_editor") {
        Today(TodayPreviewData.default, editor = TodayEditor.Hrv)
    }

    /** Text at 130%, on a frame tall enough for the whole screen: nothing may clip, and the body signals turn into tiles. */
    @Test
    fun largeText() {
        paparazzi.unsafeUpdateConfig(deviceConfig = Pixel9ProXL.copy(fontScale = 1.3f, screenHeight = 2640))
        paparazzi.snapshotThemes("today_large_text") { Today(TodayPreviewData.sampleDay) }
    }
}

/**
 * [TodayScreen] for [state] with the tab bar over it. An [editor] is drawn in place of its
 * sheet window, which Paparazzi can't capture.
 */
@Composable
private fun Today(
    state: TodayUiState,
    scrolledToEnd: Boolean = false,
    focusGoal: Boolean = false,
    editor: TodayEditor? = null,
) {
    val goalFocus = remember { FocusRequester() }
    Box(Modifier.fillMaxSize()) {
        TodayScreen(
            state = state,
            onGoalChange = {},
            onSampleDay = {},
            onOpenEditor = {},
            onMetricChange = { _, _ -> },
            onCloseEditor = {},
            onGenerate = {},
            onOpenRuntime = {},
            contentPadding = tabBarPadding(),
            scrollState = rememberScrollState(if (scrolledToEnd) Int.MAX_VALUE else 0),
            goalFocusRequester = goalFocus,
        )
        FloatingNavBar(
            items = TopLevelTab.entries.map { it.item },
            selectedIndex = TopLevelTab.Today.ordinal,
            onSelect = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        if (editor != null) {
            WellnessBottomSheetFrame {
                TodayEditorContent(editor = editor, metrics = state.metrics, onMetricChange = { _, _ -> }, onDone = {})
            }
        }
    }
    if (focusGoal) {
        LaunchedEffect(goalFocus) { goalFocus.requestFocus() }
    }
}
