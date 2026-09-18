package com.mimik.wellnessnudge.ui.format

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
    val minutes = (hours * 60).roundToInt().coerceAtLeast(0)
    return "${minutes / 60}h ${minutes % 60}m"
}

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
