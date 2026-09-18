package com.mimik.wellnessnudge.snapshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.api.TipCard
import com.mimik.wellnessnudge.api.TipNudge
import com.mimik.wellnessnudge.api.TipsResponse
import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.data.toMetrics
import com.mimik.wellnessnudge.ui.components.FloatingNavBar
import com.mimik.wellnessnudge.ui.foryou.ForYouUiState
import com.mimik.wellnessnudge.ui.foryou.forYouContent
import com.mimik.wellnessnudge.ui.journal.JournalFilter
import com.mimik.wellnessnudge.ui.journal.JournalUiState
import com.mimik.wellnessnudge.ui.journal.journalContent
import com.mimik.wellnessnudge.ui.navigation.TopLevelTab
import com.mimik.wellnessnudge.ui.preview.PreviewData
import java.time.ZonedDateTime

/**
 * Sample data for the Journal and For you snapshots: one person's week of nudges up to
 * PreviewData's clock (Thursday, September 17, 2026, 9:41 AM), and the tips the mim builds
 * from the ones they marked helpful.
 */
internal object ListsPreviewData {

    /** Twelve nudges over six days, newest first, in every category and rating state. */
    val history: List<NudgeHistoryItem> = listOf(
        record(
            at(0, 7, 12), "improve-recovery", Feedback.Unset,
            NudgeRequest(7.8, 19.0, 21.0, 58, 52, 23_500, "Recover from yesterday's hike"),
            "You walked 23,500 steps yesterday and slept 7.8 hours, so today is for easy recovery. Keep it light " +
                "with a relaxed 20-minute walk instead of a hard workout.",
        ),
        record(
            at(1, 21, 5), "improve-sleep", Feedback.Helpful,
            NudgeRequest(6.2, 13.0, 17.0, 63, 41, 8_200, "Fall asleep before 11"),
            "Deep sleep was only 13% of your 6.2 hours last night. Dim the lights by 9:30 PM and leave your phone " +
                "outside the bedroom; a calmer last hour usually means more deep sleep.",
        ),
        record(
            at(1, 12, 40), "reduce-stress", Feedback.NotHelpful,
            NudgeRequest(6.2, 13.0, 17.0, 66, 38, 8_200, "Stay calm through a busy afternoon"),
            "Your HRV dipped to 38 ms, a sign your body is under load. Before your 2 PM meeting, take five slow " +
                "breaths, in for 4 seconds and out for 6, then walk the long way back to your desk.",
        ),
        record(
            at(2, 18, 20), "improve-fitness", Feedback.Helpful,
            NudgeRequest(7.4, 18.0, 20.0, 57, 55, 9_600, "Train for my first half marathon"),
            "A resting heart rate of 57 bpm and 7.4 hours of sleep say you're ready for a quality run. Try 6 km " +
                "with the middle 2 km at tempo pace, then walk for 10 minutes to cool down.",
        ),
        record(
            at(2, 7, 15), "reduce-fatigue", Feedback.Helpful,
            NudgeRequest(6.1, 14.0, 16.0, 64, 39, 4_200, "Beat the afternoon slump"),
            "With 6.1 hours of sleep and 4,200 steps yesterday, expect a dip around 3 PM. Do your hardest task " +
                "before noon, then swap the second coffee for a brisk 10-minute walk.",
        ),
        record(
            at(3, 20, 45), "improve-mood", Feedback.Unset,
            NudgeRequest(5.9, 12.0, 15.0, 67, 34, 5_100, "Lift my mood this week"),
            "A 5.9-hour night can flatten your mood. Get 15 minutes of daylight before 11 AM and message one " +
                "friend you haven't talked to this week.",
        ),
        record(
            at(3, 8, 10), "improve-sleep", Feedback.Helpful,
            NudgeRequest(7.1, 18.0, 19.0, 60, 48, 7_300, "Wake up more rested"),
            "Your deep sleep rose to 18% after an earlier bedtime. Keep the same 10:30 PM lights-out tonight and " +
                "skip caffeine after 1 PM to hold on to that gain.",
        ),
        record(
            at(4, 10, 30), "weight-loss", Feedback.NotHelpful,
            NudgeRequest(6.8, 16.0, 18.0, 62, 46, 6_800, "Lose 5 pounds by October"),
            "You averaged 6,800 steps this week. Add a 15-minute walk after dinner to reach 9,000 today; steady " +
                "daily movement adds up faster than occasional hard workouts.",
        ),
        record(
            at(4, 7, 50), "improve-appetite", Feedback.Unset,
            NudgeRequest(6.6, 16.0, 18.0, 61, 45, 5_900, "Eat a proper breakfast again"),
            "Your resting heart rate is back down to 61 bpm, a good sign you're recovering. Start with something " +
                "easy, like yogurt with oats and berries, and add a small snack mid-morning.",
        ),
        record(
            at(5, 16, 15), "reduce-stress", Feedback.Helpful,
            NudgeRequest(7.5, 19.0, 20.0, 59, 52, 11_400, "Unwind after a long week"),
            "Your HRV climbed back to 52 ms after a quieter week. Protect it this weekend: take a long walk " +
                "without your phone and keep the last hour before bed screen-free.",
        ),
        record(
            at(5, 9, 20), "other", Feedback.Unset,
            NudgeRequest(6.4, 15.0, 17.0, 63, 43, 6_100, null),
            "On 6.4 hours of sleep, swap your second coffee for a glass of water and five minutes outside. " +
                "Daylight and movement lift your energy without the afternoon crash.",
        ),
        record(
            at(5, 7, 5), "improve-sleep", Feedback.Unset,
            NudgeRequest(5.5, 11.0, 14.0, 68, 31, 3_900, "Sleep better tonight"),
            "You slept 5.5 hours and your HRV is 31 ms. Keep caffeine to the morning, get outside before noon " +
                "and aim for lights out by 10:30 PM tonight.",
        ),
    )

    /**
     * The tips for [history], as the mim builds them: goals asked about lately, most recent
     * focus first, each with the nudges marked helpful for it.
     */
    val tips = TipsResponse(
        items = listOf(
            tipCard("improve-sleep", "improving sleep", recentMentions = 3, history[1], history[6]),
            tipCard("reduce-stress", "reducing stress", recentMentions = 2, history[9]),
            tipCard("improve-fitness", "training and fitness", recentMentions = 1, history[3]),
        ),
        totalRecent = history.size,
        generatedAt = PreviewData.now,
    )

    fun journal(filter: JournalFilter = JournalFilter.All, history: List<NudgeHistoryItem> = this.history) =
        JournalUiState(journalContent(history, failed = false, filter, PreviewData.zone), filter)

    fun forYou(tips: TipsResponse = this.tips) = ForYouUiState(forYouContent(tips, failed = false))

    private fun at(daysAgo: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(2026, 9, 17, hour, minute, 0, 0, PreviewData.zone)
            .minusDays(daysAgo.toLong())
            .toInstant()
            .toEpochMilli()

    private fun record(ts: Long, category: String, feedback: Feedback, request: NudgeRequest, nudge: String) =
        NudgeHistoryItem(
            id = "nudge_$ts",
            ts = ts,
            metrics = request.toMetrics(),
            userGoal = request.userGoal.orEmpty(),
            category = category,
            nudge = nudge,
            model = "smollm2-360m",
            helpful = feedback.wireValue,
        )

    // The intro is the mim's own template.
    private fun tipCard(category: String, label: String, recentMentions: Int, vararg helpful: NudgeHistoryItem) =
        TipCard(
            category = category,
            categoryLabel = label,
            intro = "Your recent nudges suggest you've been focused on $label. Here are a few past nudges " +
                "that you marked as helpful for this goal.",
            recentMentions = recentMentions,
            lastFocusedAt = history.first { it.category == category }.ts,
            helpfulNudges = helpful.map { TipNudge(it.id, it.ts, it.nudge) },
        )
}

/**
 * A tab as the app shell shows it: the screen padded for the floating tab bar, which
 * floats over it with [selected] highlighted.
 */
@Composable
internal fun TabFrame(selected: TopLevelTab, content: @Composable (contentPadding: PaddingValues) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        content(tabBarPadding())
        FloatingNavBar(
            items = TopLevelTab.entries.map { it.item },
            selectedIndex = selected.ordinal,
            onSelect = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
