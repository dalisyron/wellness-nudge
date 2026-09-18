package com.mimik.wellnessnudge.ui.format

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.sp
import com.mimik.wellnessnudge.ui.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {

    private val clock = PreviewData.clock
    private val dayMillis = 24 * 60 * 60 * 1000L

    @Test
    fun sleep() {
        assertEquals("6h 30m", formatSleep(6.5f))
        assertEquals("7h 0m", formatSleep(7f))
        assertEquals("5h 45m", formatSleep(5.75f))
        assertEquals("6.5 h", formatSleepShort(6.5f))
        assertEquals("7 h", formatSleepShort(7f))
    }

    @Test
    fun sleepForDisplayStylesOnlyTheUnits() {
        val units = SpanStyle(fontSize = 24.sp)
        val text = formatSleepAnnotated(6.5f, units)

        assertEquals("6h\u200930m", text.text)
        assertEquals(listOf("h", "m"), text.spanStyles.map { text.text.substring(it.start, it.end) })
        assertTrue(text.spanStyles.all { it.item == units })
    }

    @Test
    fun numbers() {
        assertEquals("7,000", formatSteps(7000))
        assertEquals("15%", formatPercent(15.4f))
        assertEquals("4.2 s", formatSeconds(4_213))
        assertEquals("368 MB", formatBytes(386_400_000))
        assertEquals("1.7 GB", formatBytes(1_834_400_000))
    }

    @Test
    fun greetings() {
        assertEquals("Good morning", greetingFor(9))
        assertEquals("Good afternoon", greetingFor(12))
        assertEquals("Good evening", greetingFor(17))
    }

    @Test
    fun dates() {
        assertEquals("Today", dayLabel(PreviewData.now, clock))
        assertEquals("Yesterday", dayLabel(PreviewData.now - dayMillis, clock))
        assertEquals("Tue, Sep 15", dayLabel(PreviewData.now - 2 * dayMillis, clock))
        assertEquals("9:41 AM", timeLabel(PreviewData.now, clock))
        assertEquals("Thursday, September 17", fullDateLabel(PreviewData.now, clock))
        assertEquals("Sep 15", shortDateLabel(PreviewData.now - 2 * dayMillis, clock))
    }
}
