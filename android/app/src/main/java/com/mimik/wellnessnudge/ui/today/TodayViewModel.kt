package com.mimik.wellnessnudge.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.demo.MockNudgeInput
import com.mimik.wellnessnudge.ui.components.RuntimeStatus
import kotlinx.coroutines.launch

/**
 * Holds what the user sets on Today (last night's signals and a goal), which editor is
 * open, and whether the on-device service answered its healthcheck.
 *
 * @param nextSampleDay the next "Sample day" scenario.
 */
class TodayViewModel(
    private val repository: NudgeRepository,
    private val nextSampleDay: () -> MockNudgeInput,
) : ViewModel() {

    // Compose state rather than a StateFlow: the goal field needs each keystroke applied
    // synchronously, or fast typing can drop characters.
    var state by mutableStateOf(TodayUiState())
        private set

    init {
        viewModelScope.launch {
            val health = repository.health()
            state = state.copy(runtime = if (health?.isHealthy == true) RuntimeStatus.Ready else RuntimeStatus.Error)
        }
    }

    fun setGoal(goal: String) {
        state = state.copy(goal = goal.take(GoalMaxLength))
    }

    /** Slider and −/+ changes, kept on the metric's steps and within its range. */
    fun setMetric(metric: Metric, value: Float) {
        state = state.copy(metrics = state.metrics.with(metric, metric.snap(value)))
    }

    /** Fills in the next sample scenario, metrics and goal together. */
    fun loadSampleDay() {
        val sample = nextSampleDay()
        state = state.copy(metrics = sample.toDayMetrics(), goal = sample.userGoal)
    }

    fun openEditor(editor: TodayEditor) {
        state = state.copy(editor = editor)
    }

    fun closeEditor() {
        state = state.copy(editor = null)
    }

    fun request(): NudgeRequest = state.toNudgeRequest()
}

// The goal ends up in a small model's prompt: a sentence is plenty.
private const val GoalMaxLength = 120
