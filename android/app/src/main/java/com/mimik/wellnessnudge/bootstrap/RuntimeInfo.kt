package com.mimik.wellnessnudge.bootstrap

import com.mimik.wellnessnudge.api.HealthStatus

/**
 * What is running on the phone right now, for the runtime sheet. Each part degrades on its
 * own: an unreachable mim leaves [mimHealth] null but still reports the runtime and models.
 */
data class RuntimeInfo(
    /** Loopback port of the embedded mimOE runtime. */
    val port: Int,
    /** API root of the wellness-nudge mim, e.g. "/wellness-nudge/v1". */
    val mimApiRoot: String,
    /** The mim's healthcheck answer; null when it couldn't be reached. */
    val mimHealth: HealthStatus?,
    val models: List<RuntimeModel>,
    /** Nudges in on-device storage; null when the count couldn't be read. */
    val nudgeCount: Int?,
    /** Whether the embedded mimOE runtime reports itself up. */
    val runtimeReady: Boolean = true,
) {
    val mimHealthy: Boolean get() = mimHealth?.isHealthy == true
}

/** One on-device model and whether mILM can serve it. */
data class RuntimeModel(
    /** mILM model id, e.g. "smollm2-360m". */
    val id: String,
    /** Role in the app, e.g. "Nudge writer". */
    val displayName: String,
    /** Model name and quantization, e.g. "SmolLM2 360M · Q8_0". */
    val technicalName: String,
    val sizeBytes: Long,
    val ready: Boolean,
)
