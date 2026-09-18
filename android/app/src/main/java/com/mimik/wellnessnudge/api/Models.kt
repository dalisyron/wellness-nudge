package com.mimik.wellnessnudge.api

import com.google.gson.annotations.SerializedName

/** POST /nudge body. All fields optional but at least one required. */
data class NudgeRequest(
    val sleepHours: Double? = null,
    val deepSleepPct: Double? = null,
    val remSleepPct: Double? = null,
    val restingHR: Int? = null,
    val hrvMs: Int? = null,
    val stepsYesterday: Int? = null,
    val userGoal: String? = null,
)

/** /nudge + /history items are wrapped: { "data": { ... } } */
data class Envelope<T>(val data: T?)
data class ListEnvelope<T>(val data: ListPayload<T>?)
data class ListPayload<T>(val items: List<T>?, val total: Int?)

/** Server returns this shape for /nudge. */
data class NudgeResponse(
    val id: String,
    val nudge: String,
    val category: String?,
    val model: String?,
    val finishReason: String?,
    val usage: Usage?,
    val metricsUsed: Map<String, Any?>?,
    /** Inference time in ms, when the mim reports it; the app otherwise measures it. */
    val latencyMs: Long? = null,
) {
    data class Usage(
        @SerializedName("prompt_tokens") val promptTokens: Int?,
        @SerializedName("completion_tokens") val completionTokens: Int?,
        @SerializedName("total_tokens") val totalTokens: Int?,
    )
}

/** /history list item — same as NudgeHistoryItem in the swagger. */
data class NudgeHistoryItem(
    val id: String,
    val ts: Long,
    val metrics: Map<String, Any?>?,
    val userGoal: String?,
    val category: String?,
    val nudge: String,
    val model: String?,
    val helpful: String,
    /** Generation time in ms: from the record, or measured by the app when it created it. */
    val latencyMs: Long? = null,
)

/** PUT /nudges/{id}/feedback body. */
data class FeedbackRequest(val helpful: String)

/** GET /tips response body. */
data class TipsResponse(
    val items: List<TipCard>?,
    val totalRecent: Int?,
    val generatedAt: Long?,
)

data class TipCard(
    val category: String,
    val categoryLabel: String?,
    val intro: String?,
    val recentMentions: Int?,
    val lastFocusedAt: Long?,
    val helpfulNudges: List<TipNudge>?,
)

data class TipNudge(
    val id: String,
    val ts: Long,
    val nudge: String,
)

/** GET /healthcheck response body. */
data class HealthStatus(
    val status: String?,
    val mim: String?,
    val version: String?,
) {
    val isHealthy: Boolean get() = status == "ok"
}
