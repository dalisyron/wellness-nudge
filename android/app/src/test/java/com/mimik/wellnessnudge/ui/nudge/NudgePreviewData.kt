package com.mimik.wellnessnudge.ui.nudge

import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.data.toMetrics
import com.mimik.wellnessnudge.ui.preview.PreviewData
import java.time.ZonedDateTime

/** Nudge screen states for snapshot tests, on [PreviewData]'s Thursday morning. */
internal object NudgePreviewData {

    /** A short night and a low HRV. */
    private val shortNight = NudgeRequest(
        sleepHours = 5.2,
        deepSleepPct = 12.0,
        remSleepPct = 16.0,
        restingHR = 68,
        hrvMs = 29,
        stepsYesterday = 4_300,
        userGoal = "Sleep better tonight",
    )

    /** The nudge the model just wrote, at 9:41 AM. */
    private val fresh = NudgeHistoryItem(
        id = "nudge_${PreviewData.now}",
        ts = PreviewData.now,
        metrics = shortNight.toMetrics(),
        userGoal = shortNight.userGoal,
        category = "improve-sleep",
        nudge = "You slept only 5.2 hours and your HRV is low at 29 ms, so tonight is for winding down early. " +
            "Put your phone on the charger outside your bedroom 30 minutes before bed.",
        model = "smollm2-360m",
        helpful = Feedback.Unset.wireValue,
        latencyMs = 4_200,
    )

    /** Yesterday evening's nudge, opened from the Journal; generated before the app kept timings. */
    private val fromJournal = run {
        val ts = ZonedDateTime.of(2026, 9, 16, 18, 5, 0, 0, PreviewData.zone).toInstant().toEpochMilli()
        val request = NudgeRequest(
            sleepHours = 6.1,
            deepSleepPct = 14.0,
            remSleepPct = 17.0,
            restingHR = 71,
            hrvMs = 34,
            stepsYesterday = 11_800,
            userGoal = "Recover from my long run",
        )
        NudgeHistoryItem(
            id = "nudge_$ts",
            ts = ts,
            metrics = request.toMetrics(),
            userGoal = request.userGoal,
            category = "improve-recovery",
            nudge = "After 11,800 steps and a resting heart rate of 71 bpm, your body is asking for an easy day. " +
                "Swap today's run for a 20-minute walk and stretch your calves and hips before bed.",
            model = "smollm2-360m",
            helpful = Feedback.NotHelpful.wireValue,
            latencyMs = null,
        )
    }

    val generating = NudgeUiState(NudgeContent.Generating(PreviewData.request.toSignals(), elapsedMs = 3_200))

    val generatingLongGoal = NudgeUiState(
        NudgeContent.Generating(
            PreviewData.request
                .copy(userGoal = "Fall asleep before midnight on work nights and stop waking up at 3 AM")
                .toSignals(),
            elapsedMs = 7_600,
        ),
    )

    /** A fresh result, its words all in. */
    val result = NudgeUiState(fresh.toResult(reveal = false, request = shortNight, fresh = true))

    /** Rated helpful: a fresh sleep nudge, so it joins For you. */
    val resultHelpful = NudgeUiState(
        fresh.copy(helpful = Feedback.Helpful.wireValue).toResult(reveal = false, request = shortNight, fresh = true),
    )

    val savedNotHelpful = NudgeUiState(fromJournal.toResult(reveal = false))

    val failed = NudgeUiState(NudgeContent.GenerationFailed(PreviewData.generationFailed.message, PreviewData.request))

    val loading = NudgeUiState(NudgeContent.Loading)
}
