package com.mimik.wellnessnudge.ui.format

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * "Now" and the time zone for date labels. Screens read it from [LocalWellnessClock] so
 * previews and snapshot tests can pin a fixed morning instead of the real time.
 */
@Immutable
class WellnessClock(val zone: ZoneId, private val nowMillis: () -> Long) {

    fun now(): Long = nowMillis()

    fun dateTime(ts: Long = now()): ZonedDateTime = Instant.ofEpochMilli(ts).atZone(zone)

    companion object {
        /** The phone's clock, in its time zone as it is now. */
        val System: WellnessClock get() = WellnessClock(ZoneId.systemDefault()) { java.lang.System.currentTimeMillis() }

        fun fixed(nowMillis: Long, zone: ZoneId) = WellnessClock(zone) { nowMillis }
    }
}

val LocalWellnessClock = staticCompositionLocalOf { WellnessClock.System }

/**
 * The phone's clock for [LocalWellnessClock], renewed whenever what it shows may have moved
 * on: at the top of each hour (greetings change at noon and 5 PM, dates at midnight), when
 * the app comes back to the foreground (the hourly wait doesn't run while the phone sleeps),
 * and when the date, time or time zone is changed. A renewed clock recomposes whatever reads
 * it, so Today's greeting and date and the Journal's day names stay current.
 */
@Composable
fun rememberSystemClock(): WellnessClock {
    val context = LocalContext.current
    var taken by remember { mutableStateOf(TakenClock()) }
    fun renew() {
        val next = TakenClock()
        // Only a new hour (or zone) changes anything on screen: otherwise keep the clock, so
        // nothing recomposes.
        if (next.hour != taken.hour) taken = next
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { renew() }
    LaunchedEffect(taken) {
        // A second past each hour; checks again should the hour not have turned (the time was set back).
        while (true) {
            val now = ZonedDateTime.now(taken.clock.zone)
            delay(ChronoUnit.MILLIS.between(now, now.truncatedTo(ChronoUnit.HOURS).plusHours(1)) + 1_000)
            renew()
        }
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = renew()
        }
        val changes = IntentFilter().apply {
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, changes, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return taken.clock
}

/**
 * The phone's clock and the hour it was taken in. The hour is zoned, so a new time zone
 * counts as a new hour.
 */
private class TakenClock(val clock: WellnessClock = WellnessClock.System) {
    val hour: ZonedDateTime = clock.dateTime().truncatedTo(ChronoUnit.HOURS)
}
