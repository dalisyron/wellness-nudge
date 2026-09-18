package com.mimik.wellnessnudge.ui.nudge

import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.toRequest
import com.mimik.wellnessnudge.ui.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgeTextTest {

    private val clock = PreviewData.clock
    private val dayMillis = 24 * 60 * 60 * 1000L

    @Test
    fun signalsFollowTheRequestWithTheGoalLast() {
        assertEquals(
            listOf("6h 30m sleep", "Deep 15%", "REM 18%", "Resting HR 64", "HRV 45 ms", "7,000 steps", "Goal · Sleep better tonight"),
            PreviewData.request.toSignals().map { it.label },
        )
        assertEquals(
            listOf(SignalKind.Hrv),
            NudgeRequest(hrvMs = 30, userGoal = "  ").toSignals().map { it.kind },
        )
    }

    @Test
    fun generatedLineNamesTheModelAndLeavesOutWhatIsUnknown() {
        assertEquals("Generated on this phone in 4.2 s · SmolLM2 360M", generatedLine(4_213, "smollm2-360m").spaced())
        assertEquals("Generated on this phone · SmolLM2 360M", generatedLine(null, "smollm2-360m").spaced())
        assertEquals("Generated on this phone in 4.2 s", generatedLine(4_213, null).spaced())
        // A model the app doesn't ship keeps its id.
        assertEquals("Generated on this phone · llama-3", generatedLine(null, "llama-3").spaced())
        // Too long for a line, the model moves to its own.
        assertEquals("Generated on this phone in 4.2 s\nSmolLM2 360M", generatedLine(4_213, "smollm2-360m", separator = "\n").spaced())
    }

    @Test
    fun onlyAFreshNudgeWithAGoalCategoryJoinsForYou() {
        val sleep = PreviewData.latest.copy(category = "improve-sleep")
        assertTrue(sleep.toResult(reveal = true, fresh = true).joinsForYou)
        assertFalse(sleep.toResult(reveal = false).joinsForYou)
        assertFalse(sleep.copy(category = "other").toResult(reveal = true, fresh = true).joinsForYou)
        assertFalse(sleep.copy(category = null).toResult(reveal = true, fresh = true).joinsForYou)
    }

    @Test
    fun aRecordWithoutSignalsCantBeGeneratedAgain() {
        assertTrue(NudgeRequest().isEmpty)
        assertTrue(PreviewData.latest.copy(metrics = null, userGoal = null).toRequest().isEmpty)
        assertFalse(PreviewData.request.isEmpty)
    }

    @Test
    fun signalsHaveSpokenForms() {
        val spoken = PreviewData.request.toSignals().associate { it.kind to it.spoken }
        assertEquals("6 hours 30 minutes of sleep", spoken[SignalKind.Sleep])
        assertEquals("resting heart rate 64", spoken[SignalKind.RestingHr])
        assertEquals("heart rate variability 45 milliseconds", spoken[SignalKind.Hrv])
    }

    @Test
    fun eyebrowNamesTheDayOfOlderNudges() {
        assertEquals("Your nudge · 9:41 AM", nudgeEyebrow(PreviewData.now, clock))
        assertEquals("Yesterday · 9:41 AM", nudgeEyebrow(PreviewData.now - dayMillis, clock))
        assertEquals("Tue, Sep 15 · 9:41 AM", nudgeEyebrow(PreviewData.now - 2 * dayMillis, clock))
    }

    @Test
    fun lastTwoWordsStayTogether() {
        assertEquals("Wind down before\u00A0bed.", "Wind down before bed. ".withoutWidow())
        assertEquals("Stretch before bedtime-friendly", "Stretch before bedtime-friendly".withoutWidow())
        assertEquals("Rest.", "Rest.".withoutWidow())
    }

    @Test
    fun revealStaggersEachWord() {
        assertEquals(listOf(0..2, 4..8, 11..14), wordRanges("You slept  well"))
        assertEquals(460, revealMillis(3))
        assertEquals(0, revealMillis(0))
    }

    @Test
    fun savedNudgesRegenerateFromTheirRecord() {
        val saved = PreviewData.history[4].toResult(reveal = false)

        assertEquals(9_500, saved.request.stepsYesterday)
        assertEquals("Train consistently for my 10K", saved.request.userGoal)
        assertEquals(3_800L, saved.latencyMs)
        assertNull(PreviewData.history[5].toResult(reveal = false).latencyMs)
        assertNull(PreviewData.latest.copy(latencyMs = 0).toResult(reveal = false).latencyMs)
    }

    private fun String.spaced() = replace('\u00A0', ' ')
}
