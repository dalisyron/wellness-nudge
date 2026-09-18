package com.mimik.wellnessnudge.ui.nudge

import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.ui.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun generatedLineLeavesOutWhatIsUnknown() {
        assertEquals("Generated on this phone in 4.2 s · smollm2-360m", generatedLine(4_213, "smollm2-360m").spaced())
        assertEquals("Generated on this phone · smollm2-360m", generatedLine(null, "smollm2-360m").spaced())
        assertEquals("Generated on this phone in 4.2 s", generatedLine(4_213, null).spaced())
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
