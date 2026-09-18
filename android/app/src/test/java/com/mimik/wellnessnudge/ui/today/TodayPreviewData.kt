package com.mimik.wellnessnudge.ui.today

import com.mimik.wellnessnudge.demo.MockNudgeInputs
import com.mimik.wellnessnudge.ui.components.RuntimeStatus

/** Today screen states for previews and snapshot tests. */
internal object TodayPreviewData {

    /** As the screen opens: the default signals, no goal, the runtime up. */
    val default = TodayUiState(runtime = RuntimeStatus.Ready)

    /** After the first "Sample day": a short night, and a goal that matches a suggestion. */
    val sampleDay: TodayUiState = MockNudgeInputs.ALL.first().let { sample ->
        TodayUiState(metrics = sample.toDayMetrics(), goal = sample.userGoal, runtime = RuntimeStatus.Ready)
    }

    /** A goal of the user's own, being typed. */
    val typing = default.copy(goal = "Stay focused through a long workday")
}
