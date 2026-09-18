package com.mimik.wellnessnudge.data

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers how long each nudge took to generate. The mim's records don't carry latency,
 * so the app keeps its own measurements keyed by nudge id.
 */
interface LatencyStore {
    fun get(id: String): Long?
    fun put(id: String, latencyMs: Long)
    fun remove(id: String)

    /** Process-lifetime store, for previews and tests. */
    class InMemory : LatencyStore {
        private val values = ConcurrentHashMap<String, Long>()
        override fun get(id: String): Long? = values[id]
        override fun put(id: String, latencyMs: Long) {
            values[id] = latencyMs
        }
        override fun remove(id: String) {
            values.remove(id)
        }
    }
}

/** [LatencyStore] backed by SharedPreferences, so "generated in 4.2 s" survives app restarts. */
class SharedPrefsLatencyStore(context: Context) : LatencyStore {
    private val prefs = context.applicationContext.getSharedPreferences("nudge_latency", Context.MODE_PRIVATE)

    override fun get(id: String): Long? = if (prefs.contains(id)) prefs.getLong(id, 0L) else null

    override fun put(id: String, latencyMs: Long) {
        prefs.edit().putLong(id, latencyMs).apply()
    }

    override fun remove(id: String) {
        prefs.edit().remove(id).apply()
    }
}
