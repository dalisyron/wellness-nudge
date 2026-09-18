package com.mimik.wellnessnudge.ui.journal

import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.data.feedback
import com.mimik.wellnessnudge.ui.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

class JournalContentTest {

    private val zone = PreviewData.zone

    @Test
    fun placeholdersShowUntilTheFirstLoadFails() {
        assertEquals(JournalContent.Loading, journalContent(null, failed = false, JournalFilter.All, zone))
        assertEquals(JournalContent.Failed, journalContent(null, failed = true, JournalFilter.All, zone))
    }

    @Test
    fun aLoadedHistoryStaysOnScreenWhenAReloadFails() {
        val content = journalContent(PreviewData.history, failed = true, JournalFilter.All, zone)

        assertEquals(PreviewData.history.size, (content as JournalContent.Loaded).total)
    }

    @Test
    fun entriesGroupIntoDaysNewestFirst() {
        val shuffled = PreviewData.history.shuffled(Random(7))

        val content = journalContent(shuffled, failed = false, JournalFilter.All, zone) as JournalContent.Loaded

        assertEquals((17 downTo 13).map { LocalDate.of(2026, 9, it) }, content.days.map { it.date })
        assertEquals(PreviewData.history.map { it.id }, content.days.flatMap { day -> day.entries.map { it.id } })
    }

    @Test
    fun filtersKeepTheTotalAndCountWhatTheyShow() {
        JournalFilter.entries.forEach { filter ->
            val content = journalContent(PreviewData.history, failed = false, filter, zone) as JournalContent.Loaded
            val expected = PreviewData.history.filter { filter.matches(it.feedback) }

            assertEquals(PreviewData.history.size, content.total)
            assertEquals(expected.size, content.shown)
            assertTrue(content.days.all { day -> day.entries.all { filter.matches(it.feedback) } })
        }
        assertTrue(JournalFilter.Unrated.matches(Feedback.Unset))
        assertTrue(!JournalFilter.Helpful.matches(Feedback.NotHelpful))
    }

    @Test
    fun daysFollowTheTimeZone() {
        // 11:30 PM in Vancouver is already the next morning in UTC.
        val lateEvening = ZonedDateTime.of(2026, 9, 16, 23, 30, 0, 0, zone).toInstant().toEpochMilli()
        val history = listOf(PreviewData.latest.copy(id = "nudge_$lateEvening", ts = lateEvening))

        val local = journalContent(history, failed = false, JournalFilter.All, zone) as JournalContent.Loaded
        val utc = journalContent(history, failed = false, JournalFilter.All, ZoneId.of("UTC")) as JournalContent.Loaded

        assertEquals(LocalDate.of(2026, 9, 16), local.days.single().date)
        assertEquals(LocalDate.of(2026, 9, 17), utc.days.single().date)
    }

    @Test
    fun entriesCarryTheGoalOnlyWhenOneWasGiven() {
        val withoutGoal = PreviewData.latest.copy(userGoal = "  ")

        val content = journalContent(listOf(withoutGoal), failed = false, JournalFilter.All, zone) as JournalContent.Loaded
        val entry = content.days.single().entries.single()

        assertEquals(null, entry.goal)
        assertEquals(withoutGoal.feedback, entry.feedback)
    }
}
