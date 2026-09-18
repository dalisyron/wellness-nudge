package com.mimik.wellnessnudge.ui.today

import androidx.compose.runtime.Immutable
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.demo.MockNudgeInput
import com.mimik.wellnessnudge.ui.components.RuntimeStatus
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Everything the Today screen shows. */
@Immutable
data class TodayUiState(
    val metrics: DayMetrics = DayMetrics(),
    val goal: String = "",
    /** Health of the on-device service, for the header pill. */
    val runtime: RuntimeStatus = RuntimeStatus.Starting,
    /** The editor sheet on screen, if any. */
    val editor: TodayEditor? = null,
)

/** The signals behind a nudge, as the sliders hold them. */
@Immutable
data class DayMetrics(
    val sleepHours: Float = 6.5f,
    val deepSleepPct: Float = 15f,
    val remSleepPct: Float = 18f,
    val restingHr: Float = 64f,
    val hrvMs: Float = 45f,
    val steps: Float = 7_000f,
) {
    operator fun get(metric: Metric): Float = when (metric) {
        Metric.SleepHours -> sleepHours
        Metric.DeepSleep -> deepSleepPct
        Metric.RemSleep -> remSleepPct
        Metric.RestingHr -> restingHr
        Metric.Hrv -> hrvMs
        Metric.Steps -> steps
    }

    fun with(metric: Metric, value: Float): DayMetrics = when (metric) {
        Metric.SleepHours -> copy(sleepHours = value)
        Metric.DeepSleep -> copy(deepSleepPct = value)
        Metric.RemSleep -> copy(remSleepPct = value)
        Metric.RestingHr -> copy(restingHr = value)
        Metric.Hrv -> copy(hrvMs = value)
        Metric.Steps -> copy(steps = value)
    }
}

/** An editable signal: the range its slider spans and the step its slider and −/+ buttons move by. */
enum class Metric(val range: ClosedFloatingPointRange<Float>, val step: Float) {
    SleepHours(0f..12f, 0.25f),
    DeepSleep(0f..40f, 1f),
    RemSleep(0f..40f, 1f),
    RestingHr(40f..110f, 1f),
    Hrv(10f..120f, 1f),
    Steps(0f..25_000f, 100f),
    ;

    /** Slider ticks between the two ends of the range. */
    val sliderSteps: Int get() = (span / step).roundToInt() - 1

    /** Where [value] sits in the range, 0 to 1, e.g. for a meter. */
    fun fraction(value: Float): Float = ((value - range.start) / span).coerceIn(0f, 1f)

    /** [value] on the nearest step, within the range. */
    fun snap(value: Float): Float = (range.start + stepsFromStart(value).roundToInt() * step).coerceIn(range)

    /** One step up. A value between steps (float noise, 6.2500005 h) moves to the next step above it. */
    fun stepUp(value: Float): Float = (range.start + (floor(stepsFromStart(value) + Tolerance) + 1) * step).coerceIn(range)

    /** One step down, to the previous step below a value between steps. */
    fun stepDown(value: Float): Float = (range.start + (ceil(stepsFromStart(value) - Tolerance) - 1) * step).coerceIn(range)

    private val span: Float get() = range.endInclusive - range.start

    private fun stepsFromStart(value: Float) = (value - range.start) / step
}

// Slider values carry float noise (6.2500005); within this many steps they count as on a step.
private const val Tolerance = 1e-3f

/** The editor sheets: sleep (duration and stages), or one body metric. */
enum class TodayEditor { Sleep, RestingHr, Hrv, Steps }

/**
 * The request "Generate nudge" sends. Values are rounded to what the sliders mean, so the
 * mim, and the prompt it builds, sees 5.7 rather than float noise like 5.699999809265137.
 */
fun TodayUiState.toNudgeRequest(): NudgeRequest = NudgeRequest(
    sleepHours = (metrics.sleepHours * 100).roundToInt() / 100.0,
    deepSleepPct = metrics.deepSleepPct.roundToInt().toDouble(),
    remSleepPct = metrics.remSleepPct.roundToInt().toDouble(),
    restingHR = metrics.restingHr.roundToInt(),
    hrvMs = metrics.hrvMs.roundToInt(),
    stepsYesterday = metrics.steps.roundToInt(),
    userGoal = goal.trim().ifEmpty { null },
)

/**
 * The scenario's signals on the sliders' steps (5.2 h becomes 5.25 h), so a slider never sits
 * between stops showing one value while its thumb stands for another.
 */
internal fun MockNudgeInput.toDayMetrics() = DayMetrics(
    sleepHours = Metric.SleepHours.snap(sleepHours),
    deepSleepPct = Metric.DeepSleep.snap(deepSleepPct),
    remSleepPct = Metric.RemSleep.snap(remSleepPct),
    restingHr = Metric.RestingHr.snap(restingHR),
    hrvMs = Metric.Hrv.snap(hrvMs),
    steps = Metric.Steps.snap(stepsYesterday),
)
