package com.mimik.wellnessnudge.ui.today

import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.demo.MockNudgeInputs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodayUiStateTest {

    @Test
    fun requestRoundsAwayFloatNoise() {
        val state = TodayUiState(
            metrics = DayMetrics(
                sleepHours = 5.7f,
                deepSleepPct = 15.4f,
                remSleepPct = 17.6f,
                restingHr = 63.6f,
                hrvMs = 45.2f,
                steps = 7_049.9f,
            ),
            goal = "  Sleep better tonight ",
        )

        val request = state.toNudgeRequest()

        // 5.7f.toDouble() would send 5.699999809265137.
        assertEquals(NudgeRequest(5.7, 15.0, 18.0, 64, 45, 7_050, "Sleep better tonight"), request)
    }

    @Test
    fun blankGoalIsLeftOut() {
        assertNull(TodayUiState(goal = "   ").toNudgeRequest().userGoal)
    }

    @Test
    fun firstSampleDaysAreTheDemoScenarios() {
        val requests = MockNudgeInputs.ALL.take(4).map {
            TodayUiState(metrics = it.toDayMetrics(), goal = it.userGoal).toNudgeRequest()
        }

        assertEquals(
            listOf(
                NudgeRequest(5.2, 12.0, 15.0, 68, 29, 3_400, "Sleep better tonight"),
                NudgeRequest(6.1, 15.0, 18.0, 66, 34, 5_200, "Feel less stressed before my big presentation"),
                NudgeRequest(7.8, 21.0, 22.0, 63, 44, 23_500, "Recover from yesterday's long hike"),
                NudgeRequest(7.9, 20.0, 23.0, 55, 68, 11_200, "Train for my first 10K"),
            ),
            requests,
        )
    }

    @Test
    fun stepsMoveToTheNeighboringSliderStops() {
        assertEquals(6.75f, Metric.SleepHours.stepUp(6.5f))
        assertEquals(6.25f, Metric.SleepHours.stepDown(6.5f))
        // A sample day's 5.2 h sits between stops.
        assertEquals(5.25f, Metric.SleepHours.stepUp(5.2f))
        assertEquals(5.0f, Metric.SleepHours.stepDown(5.2f))
        // Slider noise still counts as the stop it is on.
        assertEquals(6.5f, Metric.SleepHours.stepUp(6.2500005f))
        assertEquals(6.0f, Metric.SleepHours.stepDown(6.2500005f))
        assertEquals(7_100f, Metric.Steps.stepUp(7_000f))
        assertEquals(6_900f, Metric.Steps.stepDown(7_000f))
    }

    @Test
    fun stepsStopAtTheEndsOfTheRange() {
        assertEquals(0f, Metric.SleepHours.stepDown(0f))
        assertEquals(12f, Metric.SleepHours.stepUp(12f))
        assertEquals(40f, Metric.RestingHr.stepDown(40f))
        assertEquals(25_000f, Metric.Steps.stepUp(24_950f))
    }

    @Test
    fun snapCleansSliderValues() {
        assertEquals(6.25f, Metric.SleepHours.snap(6.2500005f))
        assertEquals(64f, Metric.RestingHr.snap(64.00001f))
        assertEquals(110f, Metric.RestingHr.snap(140f))
    }

    @Test
    fun slidersStopAtEveryStep() {
        assertEquals(47, Metric.SleepHours.sliderSteps)
        assertEquals(39, Metric.DeepSleep.sliderSteps)
        assertEquals(69, Metric.RestingHr.sliderSteps)
        assertEquals(109, Metric.Hrv.sliderSteps)
        assertEquals(249, Metric.Steps.sliderSteps)
    }

    @Test
    fun metersPlaceValuesInTheirRange() {
        assertEquals(24f / 70f, Metric.RestingHr.fraction(64f), 1e-6f)
        assertEquals(1f, Metric.Hrv.fraction(150f))
    }

    @Test
    fun sleepIsSpokenInWords() {
        assertEquals("6 hours 30 minutes asleep", spokenSleep(6.5f))
        assertEquals("7 hours asleep", spokenSleep(7f))
        assertEquals("1 hour 1 minute asleep", spokenSleep(61 / 60f))
        assertEquals("15 minutes asleep", spokenSleep(0.25f))
        assertEquals("0 minutes asleep", spokenSleep(0f))
    }
}
