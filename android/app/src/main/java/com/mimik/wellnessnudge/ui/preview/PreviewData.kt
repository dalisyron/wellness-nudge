package com.mimik.wellnessnudge.ui.preview

import com.mimik.wellnessnudge.api.HealthStatus
import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.api.TipCard
import com.mimik.wellnessnudge.api.TipNudge
import com.mimik.wellnessnudge.api.TipsResponse
import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.bootstrap.ModelSetupItem
import com.mimik.wellnessnudge.bootstrap.Models
import com.mimik.wellnessnudge.bootstrap.RuntimeInfo
import com.mimik.wellnessnudge.bootstrap.RuntimeModel
import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.data.GenerationState
import com.mimik.wellnessnudge.data.toMetrics
import com.mimik.wellnessnudge.data.toRequest
import com.mimik.wellnessnudge.ui.format.WellnessClock
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sample data for previews and snapshot tests: one person's week of nudges, pinned to
 * Thursday, September 17, 2026 at 9:41 AM in Vancouver. Provide [clock] through
 * LocalWellnessClock so "Today" and "Yesterday" labels stay stable.
 */
object PreviewData {

    val zone: ZoneId = ZoneId.of("America/Vancouver")
    val now: Long = at(daysAgo = 0, hour = 9, minute = 41)
    val clock = WellnessClock.fixed(now, zone)

    /** Today's default inputs, as on the Today screen. */
    val request = NudgeRequest(
        sleepHours = 6.5,
        deepSleepPct = 15.0,
        remSleepPct = 18.0,
        restingHR = 64,
        hrvMs = 45,
        stepsYesterday = 7000,
        userGoal = "Sleep better tonight",
    )

    /** Twelve nudges across five days, newest first, with every feedback state. */
    val history: List<NudgeHistoryItem> = listOf(
        record(
            at(0, 9, 41), "improve-sleep", Feedback.Unset, 4200,
            NudgeRequest(5.0, 11.0, 14.0, 70, 28, 3200, "Sleep better tonight"),
            "You slept 5 hours and your HRV is 28 ms, so keep today gentle: take a 10-minute walk after " +
                "lunch and start winding down 30 minutes earlier tonight.",
        ),
        record(
            at(0, 7, 5), "reduce-fatigue", Feedback.Helpful, 3900,
            NudgeRequest(5.5, 13.0, 15.0, 68, 32, 4500, "Feel less tired this afternoon"),
            "After 5.5 hours of sleep, expect an energy dip mid-afternoon. Do your hardest task before noon, " +
                "then get 10 minutes of daylight around 2 PM instead of a second coffee.",
        ),
        record(
            at(1, 21, 32), "improve-sleep", Feedback.Helpful, 4400,
            NudgeRequest(6.0, 15.0, 18.0, 65, 38, 5500, "Wind down earlier before bedtime"),
            "Deep sleep was 15% of your 6 hours last night. Dim the lights by 9:30 PM and leave your phone " +
                "outside the bedroom; a calmer last hour usually means more deep sleep.",
        ),
        record(
            at(1, 18, 10), "reduce-stress", Feedback.Helpful, 5100,
            NudgeRequest(6.0, 14.0, 17.0, 64, 40, 6000, "Find ways to relax in the evening"),
            "Your HRV of 40 ms says your body is still carrying the day. Before dinner, try five slow breaths, " +
                "in for 4 seconds and out for 6, and keep the last 30 minutes before bed screen-free.",
        ),
        record(
            at(1, 8, 2), "improve-fitness", Feedback.NotHelpful, 3800,
            NudgeRequest(7.0, 18.0, 20.0, 60, 55, 9500, "Train consistently for my 10K"),
            "Seven hours of sleep and a resting heart rate of 60 bpm mean you're ready to train. Run 30 minutes " +
                "at an easy, conversational pace today and save the speed work for Saturday.",
        ),
        record(
            at(2, 22, 5), "improve-sleep", Feedback.Helpful, null,
            NudgeRequest(5.5, 12.0, 15.0, 69, 30, 3800, "Feel more rested this week"),
            "You've been sleeping about 5.5 hours, and a resting heart rate of 69 bpm shows it. Keep a fixed " +
                "10:30 PM bedtime for the next three nights and have your caffeine before noon.",
        ),
        record(
            at(2, 12, 40), "improve-recovery", Feedback.Helpful, 4600,
            NudgeRequest(6.0, 14.0, 17.0, 64, 40, 9800, "Recover from yesterday's hike"),
            "You covered 9,800 steps on yesterday's hike and your HRV is down to 40 ms. Keep today easy: a " +
                "20-minute walk, plenty of water and some protein with every meal.",
        ),
        record(
            at(2, 7, 30), "reduce-stress", Feedback.Helpful, 4100,
            NudgeRequest(6.2, 16.0, 19.0, 63, 42, 5800, "Reduce my work stress this week"),
            "With HRV at 42 ms, today calls for pacing rather than pushing. Block two 10-minute breaks on your " +
                "calendar and take one of them as a walk without your phone.",
        ),
        record(
            at(3, 19, 15), "weight-loss", Feedback.Unset, 3700,
            NudgeRequest(6.5, 15.0, 18.0, 62, 48, 5200, "Lose 5 pounds this month"),
            "You walked 5,200 steps yesterday. Add a brisk 15-minute walk after dinner to reach about 7,500 " +
                "today; small daily habits add up faster than occasional hard workouts.",
        ),
        record(
            at(3, 9, 12), "other", Feedback.NotHelpful, null,
            NudgeRequest(6.0, 14.0, 17.0, 65, 39, 5500, "Drink less coffee and still feel awake"),
            "On 6 hours of sleep, swap your second coffee for a glass of water and a 5-minute walk outside. " +
                "Daylight and movement lift your energy without the afternoon crash.",
        ),
        record(
            at(4, 10, 25), "improve-mood", Feedback.Unset, 4800,
            NudgeRequest(5.7, 13.0, 16.0, 67, 33, 4800, "Lift my mood today"),
            "A 5.7-hour night can flatten your mood. Get 15 minutes of sunlight before 11 AM and message one " +
                "friend you haven't talked to this week.",
        ),
        record(
            at(4, 8, 15), "improve-appetite", Feedback.Unset, 3600,
            NudgeRequest(6.7, 17.0, 20.0, 61, 49, 4500, "Get my appetite back after being sick"),
            "Your resting heart rate is back down to 61 bpm, a good sign you're recovering. Start with something " +
                "easy like yogurt with oats and fruit, and add a small snack mid-morning.",
        ),
    )

    /** The nudge generated this morning. */
    val latest: NudgeHistoryItem = history.first()

    /** A saved nudge the user rated helpful. */
    val helpful: NudgeHistoryItem = history[2]

    /** Tip cards as the mim builds them: most recently focused first, helpful nudges newest first. */
    val tips = TipsResponse(
        items = listOf(
            tipCard("improve-sleep", "improving sleep", recentMentions = 3, lastFocusedAt = history[0].ts, history[2], history[5]),
            tipCard("reduce-fatigue", "fighting fatigue", recentMentions = 1, lastFocusedAt = history[1].ts, history[1]),
            tipCard("reduce-stress", "reducing stress", recentMentions = 2, lastFocusedAt = history[3].ts, history[3], history[7]),
        ),
        totalRecent = history.size,
        generatedAt = now,
    )

    val tipsEmpty = TipsResponse(items = emptyList(), totalRecent = 2, generatedAt = now)

    val generationRunning = GenerationState.Running(request, startedAtMs = 0L)
    val generationSuccess = GenerationState.Success(latest, latencyMs = 4200L, request = latest.toRequest())
    val generationFailed = GenerationState.Failed(
        message = "The on-device model took too long to answer. Give it another try.",
        request = request,
    )

    val stepStartRuntime = BootstrapState.Step(BootstrapState.Phase.START_RUNTIME)
    val stepSignIn = BootstrapState.Step(BootstrapState.Phase.LOGIN)
    val stepDeployMim = BootstrapState.Step(BootstrapState.Phase.DEPLOY_MIM)

    /** First model downloading (88 / 386 MB), second waiting in the queue. */
    val setupDownloading = BootstrapState.Setup(
        listOf(
            model(0, ModelSetupItem.State.Downloading, downloadedBytes = 88_000_000L),
            model(1, ModelSetupItem.State.Pending),
        ),
    )

    /** First model ready, second one 41% through. */
    val setupSecondModel = BootstrapState.Setup(
        listOf(
            model(0, ModelSetupItem.State.Ready),
            model(1, ModelSetupItem.State.Downloading, downloadedBytes = (Models.QWEN3.approxBytes * 0.41).toLong()),
        ),
    )

    val setupModelFailed = BootstrapState.Setup(
        listOf(
            model(0, ModelSetupItem.State.Ready),
            model(
                1,
                ModelSetupItem.State.Failed,
                downloadedBytes = (Models.QWEN3.approxBytes * 0.62).toLong(),
                error = "Couldn’t reach the download server. Check your internet and tap Try again.",
            ),
        ),
    )

    val setupReady = BootstrapState.Setup(
        listOf(model(0, ModelSetupItem.State.Ready), model(1, ModelSetupItem.State.Ready)),
    )

    val failedSignIn = BootstrapState.Failed(
        phase = BootstrapState.Phase.LOGIN,
        message = "We couldn’t reach the mimik identity service to set up your local AI. This usually means " +
            "the phone is offline or on an IPv6-only cellular network. Connect to Wi-Fi and tap Try again.",
    )

    val runtimeInfo = RuntimeInfo(
        port = 8083,
        mimApiRoot = "/wellness-nudge/v1",
        mimHealth = HealthStatus(status = "ok", mim = "wellness-nudge", version = "1.0.0"),
        models = Models.ALL.map {
            RuntimeModel(it.download.id, it.displayName, it.technicalName, it.approxBytes, ready = true)
        },
        nudgeCount = history.size,
    )

    private fun at(daysAgo: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(2026, 9, 17, hour, minute, 0, 0, zone).minusDays(daysAgo.toLong()).toInstant().toEpochMilli()

    private fun record(
        ts: Long,
        category: String,
        feedback: Feedback,
        latencyMs: Long?,
        request: NudgeRequest,
        nudge: String,
    ) = NudgeHistoryItem(
        id = "nudge_$ts",
        ts = ts,
        metrics = request.toMetrics(),
        userGoal = request.userGoal,
        category = category,
        nudge = nudge,
        model = "smollm2-360m",
        helpful = feedback.wireValue,
        latencyMs = latencyMs,
    )

    private fun tipCard(
        category: String,
        label: String,
        recentMentions: Int,
        lastFocusedAt: Long,
        vararg helpful: NudgeHistoryItem,
    ) = TipCard(
        category = category,
        categoryLabel = label,
        intro = "Your recent nudges suggest you've been focused on $label. Here are a few past nudges " +
            "that you marked as helpful for this goal.",
        recentMentions = recentMentions,
        lastFocusedAt = lastFocusedAt,
        helpfulNudges = helpful.map { TipNudge(it.id, it.ts, it.nudge) },
    )

    private fun model(
        index: Int,
        state: ModelSetupItem.State,
        downloadedBytes: Long? = null,
        error: String? = null,
    ): ModelSetupItem {
        val spec = Models.ALL[index]
        return ModelSetupItem(
            id = spec.download.id,
            displayName = spec.displayName,
            technicalName = spec.technicalName,
            purpose = spec.purpose,
            approxBytes = spec.approxBytes,
            downloadedBytes = downloadedBytes ?: if (state == ModelSetupItem.State.Ready) spec.approxBytes else 0L,
            totalBytes = if (state == ModelSetupItem.State.Pending) 0L else spec.approxBytes,
            state = state,
            errorMessage = error,
        )
    }
}
