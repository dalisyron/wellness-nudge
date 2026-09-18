package com.mimik.wellnessnudge.data

import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.api.NudgeResponse
import kotlin.math.roundToInt

/** The user's rating of a nudge, mirroring the mim's `helpful` field. */
enum class Feedback(val wireValue: String) {
    Helpful("yes"),
    NotHelpful("no"),
    Unset("unset");

    companion object {
        fun fromWire(value: String?): Feedback = entries.firstOrNull { it.wireValue == value } ?: Unset
    }
}

val NudgeHistoryItem.feedback: Feedback get() = Feedback.fromWire(helpful)

/** The goal the user typed, or null when the nudge was generated without one. */
val NudgeHistoryItem.goal: String? get() = userGoal?.takeIf { it.isNotBlank() }

/**
 * Rebuilds the request behind a stored nudge: for "Try another" and for listing the signals
 * a nudge was grounded in. Tolerates the mim's loosely typed `metrics` JSON.
 */
fun NudgeHistoryItem.toRequest(): NudgeRequest {
    val values = metrics.orEmpty()
    fun number(key: String): Double? = when (val raw = values[key]) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull()
        else -> null
    }
    return NudgeRequest(
        sleepHours = number("sleepHours"),
        deepSleepPct = number("deepSleepPct"),
        remSleepPct = number("remSleepPct"),
        restingHR = number("restingHR")?.roundToInt(),
        hrvMs = number("hrvMs")?.roundToInt(),
        stepsYesterday = number("stepsYesterday")?.roundToInt(),
        userGoal = goal ?: (values["userGoal"] as? String)?.takeIf { it.isNotBlank() },
    )
}

/** The request's fields keyed the way the mim stores them in a record's `metrics`. */
fun NudgeRequest.toMetrics(): Map<String, Any> = buildMap {
    sleepHours?.let { put("sleepHours", it) }
    deepSleepPct?.let { put("deepSleepPct", it) }
    remSleepPct?.let { put("remSleepPct", it) }
    restingHR?.let { put("restingHR", it) }
    hrvMs?.let { put("hrvMs", it) }
    stepsYesterday?.let { put("stepsYesterday", it) }
    userGoal?.takeIf { it.isNotBlank() }?.let { put("userGoal", it) }
}

/**
 * The record the mim stores for this response, built locally so a fresh nudge needs no
 * history refetch. Ids are `nudge_<ts>`, which recovers the mim's own timestamp.
 */
internal fun NudgeResponse.toHistoryItem(
    request: NudgeRequest,
    latencyMs: Long,
    fallbackTs: Long,
): NudgeHistoryItem = NudgeHistoryItem(
    id = id,
    ts = ts ?: id.removePrefix("nudge_").toLongOrNull() ?: fallbackTs,
    metrics = metricsUsed ?: request.toMetrics(),
    userGoal = userGoal ?: request.userGoal.orEmpty(),
    category = category,
    nudge = nudge,
    model = model,
    helpful = Feedback.Unset.wireValue,
    latencyMs = latencyMs,
)
