package com.mimik.wellnessnudge.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.demo.MockNudgeInput
import com.mimik.wellnessnudge.ui.components.RuntimeStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Holds what the user sets on Today (last night's signals and a goal), which editor is
 * open, and whether the on-device service answers. The signals and goal outlive the process,
 * as the screen does. The runtime is checked again each time Today comes into view, retrying
 * with a growing pause until it answers, and follows what every other call to it finds.
 *
 * @param nextSampleDay the next "Sample day" scenario.
 */
class TodayViewModel(
    private val repository: NudgeRepository,
    private val nextSampleDay: () -> MockNudgeInput,
    private val saved: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    // Compose state rather than a StateFlow: the goal field needs each keystroke applied
    // synchronously, or fast typing can drop characters.
    var state by mutableStateOf(saved.restore())
        private set

    private val created = TimeSource.Monotonic.markNow()
    private var everUp = false
    private var checking: Job? = null

    init {
        viewModelScope.launch {
            repository.runtimeUp.filterNotNull().collect(::showRuntime)
        }
    }

    /** Today is in view: check the runtime, and keep checking, less and less often, until it answers. */
    fun onStart() {
        checking?.cancel()
        checking = viewModelScope.launch {
            var pause = FirstRetry
            while (repository.health()?.isHealthy != true) {
                // runtimeUp doesn't repeat an unchanged answer, but the startup grace may have run out.
                showRuntime(up = false)
                delay(pause)
                pause = (pause * 2).coerceAtMost(LongestRetry)
            }
        }
    }

    /** Today is out of view: stop checking. */
    fun onStop() {
        checking?.cancel()
    }

    fun setGoal(goal: String) {
        update(state.copy(goal = goal.take(GoalMaxLength)))
    }

    /** Slider and −/+ changes, kept on the metric's steps and within its range. */
    fun setMetric(metric: Metric, value: Float) {
        update(state.copy(metrics = state.metrics.with(metric, metric.snap(value))))
    }

    /** Fills in the next sample scenario, metrics and goal together. */
    fun loadSampleDay() {
        val sample = nextSampleDay()
        update(state.copy(metrics = sample.toDayMetrics(), goal = sample.userGoal))
    }

    fun openEditor(editor: TodayEditor) {
        state = state.copy(editor = editor)
    }

    fun closeEditor() {
        state = state.copy(editor = null)
    }

    fun request(): NudgeRequest = state.toNudgeRequest()

    private fun update(new: TodayUiState) {
        if (new.goal != state.goal) saved[KeyGoal] = new.goal
        if (new.metrics != state.metrics) saved[KeyMetrics] = new.metrics.toArray()
        state = new
    }

    /**
     * Ready once the runtime answers. Until it first does, a miss reads as still starting for a
     * few seconds (it may just have been deployed); after that, or once it had answered, as
     * unavailable.
     */
    private fun showRuntime(up: Boolean) {
        if (up) everUp = true
        val status = when {
            up -> RuntimeStatus.Ready
            !everUp && created.elapsedNow() < StartupGrace -> RuntimeStatus.Starting
            else -> RuntimeStatus.Error
        }
        if (status != state.runtime) state = state.copy(runtime = status)
    }
}

private fun SavedStateHandle.restore() = TodayUiState(
    metrics = get<FloatArray>(KeyMetrics)?.toDayMetrics() ?: DayMetrics(),
    goal = get<String>(KeyGoal).orEmpty(),
)

private fun DayMetrics.toArray() = Metric.entries.map { this[it] }.toFloatArray()

private fun FloatArray.toDayMetrics(): DayMetrics? {
    if (size != Metric.entries.size) return null
    return Metric.entries.foldIndexed(DayMetrics()) { index, metrics, metric -> metrics.with(metric, this[index]) }
}

private const val KeyGoal = "goal"
private const val KeyMetrics = "metrics"

// The goal ends up in a small model's prompt: a sentence is plenty.
private const val GoalMaxLength = 120

private val StartupGrace = 10.seconds
private val FirstRetry = 2.seconds
private val LongestRetry = 30.seconds
