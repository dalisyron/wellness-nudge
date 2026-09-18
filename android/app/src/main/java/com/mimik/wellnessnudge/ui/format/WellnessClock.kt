package com.mimik.wellnessnudge.ui.format

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * "Now" and the time zone for date labels. Screens read it from [LocalWellnessClock] so
 * previews and snapshot tests can pin a fixed morning instead of the real time.
 */
@Immutable
class WellnessClock(val zone: ZoneId, private val nowMillis: () -> Long) {

    fun now(): Long = nowMillis()

    fun dateTime(ts: Long = now()): ZonedDateTime = Instant.ofEpochMilli(ts).atZone(zone)

    companion object {
        val System = WellnessClock(ZoneId.systemDefault()) { java.lang.System.currentTimeMillis() }

        fun fixed(nowMillis: Long, zone: ZoneId) = WellnessClock(zone) { nowMillis }
    }
}

val LocalWellnessClock = staticCompositionLocalOf { WellnessClock.System }
