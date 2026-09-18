package com.mimik.wellnessnudge.ui.format

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

// The interface copy is English, so numbers and dates are too.
private val Copy = Locale.US
private val TimeFormat = DateTimeFormatter.ofPattern("h:mm a", Copy)
private val DayFormat = DateTimeFormatter.ofPattern("EEE, MMM d", Copy)
private val FullDateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d", Copy)
private val ShortDateFormat = DateTimeFormatter.ofPattern("MMM d", Copy)

private const val MiB = 1024L * 1024
private const val GiB = MiB * 1024

/** 6.5 → "6h 30m". */
fun formatSleep(hours: Float): String {
    val minutes = sleepMinutes(hours)
    return "${minutes / 60}h ${minutes % 60}m"
}

/**
 * 6.5 → "6h 30m" for display, with [unitStyle] on "h" and "m" (e.g. smaller and quieter
 * than the digits) and a thin space between the groups, as the unit letters are smaller.
 */
fun formatSleepAnnotated(hours: Float, unitStyle: SpanStyle): AnnotatedString {
    val minutes = sleepMinutes(hours)
    return buildAnnotatedString {
        append("${minutes / 60}")
        withStyle(unitStyle) { append("h") }
        append(ThinSpace)
        append("${minutes % 60}")
        withStyle(unitStyle) { append("m") }
    }
}

private fun sleepMinutes(hours: Float): Int = (hours * 60).roundToInt().coerceAtLeast(0)

private const val ThinSpace = " "

/** 6.5 → "6.5 h", 7.0 → "7 h". */
fun formatSleepShort(hours: Float): String = "${formatDecimal(hours)} h"

/** 7000 → "7,000". */
fun formatSteps(steps: Int): String = NumberFormat.getIntegerInstance(Copy).format(steps)

/** 15.4 → "15%". */
fun formatPercent(percent: Float): String = "${percent.roundToInt()}%"

/** 4213 → "4.2 s". */
fun formatSeconds(millis: Long): String = String.format(Copy, "%.1f s", millis / 1000.0)

/** Binary units, as download sizes are usually shown: 386_400_000 → "368 MB", 1.83e9 → "1.7 GB". */
fun formatBytes(bytes: Long): String =
    if (bytes >= GiB) String.format(Copy, "%.1f GB", bytes.toDouble() / GiB) else "${bytes / MiB} MB"

/**
 * Curly quotes for display: an apostrophe inside a word (don't, today's) becomes ’, and
 * straight double quotes, taken in pairs, become “ and ”; one without a partner is left
 * alone. For model text, which arrives with typewriter quotes the serif renders as ticks.
 */
fun String.withTypographicQuotes(): String {
    val text = replace(InWordApostrophe, "\u2019")
    val pairs = text.count { it == '"' } / 2 * 2
    if (pairs == 0) return text
    var seen = 0
    return buildString(text.length) {
        for (c in text) {
            if (c == '"' && seen < pairs) {
                append(if (seen % 2 == 0) '\u201C' else '\u201D')
                seen++
            } else {
                append(c)
            }
        }
    }
}

private val InWordApostrophe = Regex("(?<=\\p{L})'(?=\\p{L})")

/** Good morning before noon, good afternoon before 5 PM, good evening after. */
fun greetingFor(hour: Int): String = when {
    hour < 12 -> "Good morning"
    hour < 17 -> "Good afternoon"
    else -> "Good evening"
}

/** "Today", "Yesterday", then "Mon, Sep 15". */
fun dayLabel(ts: Long, clock: WellnessClock = WellnessClock.System): String {
    val date = clock.dateTime(ts).toLocalDate()
    return when (ChronoUnit.DAYS.between(date, clock.dateTime().toLocalDate())) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> DayFormat.format(date)
    }
}

/** "9:41 AM". */
fun timeLabel(ts: Long, clock: WellnessClock = WellnessClock.System): String =
    TimeFormat.format(clock.dateTime(ts))

/** "Thursday, September 17". */
fun fullDateLabel(ts: Long, clock: WellnessClock = WellnessClock.System): String =
    FullDateFormat.format(clock.dateTime(ts))

/** "Sep 15". */
fun shortDateLabel(ts: Long, clock: WellnessClock = WellnessClock.System): String =
    ShortDateFormat.format(clock.dateTime(ts))

private fun formatDecimal(value: Float): String {
    val tenths = (value * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else String.format(Copy, "%.1f", tenths / 10.0)
}
