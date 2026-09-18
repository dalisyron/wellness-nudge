package com.mimik.wellnessnudge.bootstrap

/**
 * What stage of one-time setup we're in. The main app is gated behind
 * [Ready] — the user can't reach the bottom nav until everything they
 * need has been provisioned on-device.
 *
 * - [NotStarted] / [Step] are the brief, in-process prep phases (runtime
 *   start, login, mILM + mim deploy). Shown as a calm loading screen.
 * - [Setup] is the model-download gate. Shown as a dedicated screen with
 *   per-model progress cards. Persists until the user taps Continue.
 * - [Ready] unlocks the main app.
 * - [Failed] is a catastrophic pre-Setup failure (e.g. token expired).
 */
sealed class BootstrapState {

    data object NotStarted : BootstrapState()

    data class Step(val phase: Phase, val detail: String = "") : BootstrapState()

    /**
     * The user is on the model-setup gate. Stays in this state while
     * downloads run AND after they complete — the transition to [Ready]
     * is triggered by the user tapping Continue. While in this state the
     * ViewModel mutates [items] to reflect per-model progress.
     */
    data class Setup(val items: List<ModelSetupItem>) : BootstrapState() {
        val allReady: Boolean get() = items.isNotEmpty() && items.all { it.state == ModelSetupItem.State.Ready }
        val anyFailed: Boolean get() = items.any { it.state == ModelSetupItem.State.Failed }
        val anyInFlight: Boolean get() = items.any {
            it.state == ModelSetupItem.State.Downloading || it.state == ModelSetupItem.State.Pending
        }
    }

    data class Ready(val mimBaseUrl: String, val apiKey: String) : BootstrapState()

    data class Failed(val phase: Phase, val message: String, val cause: Throwable? = null) : BootstrapState()

    /** Setup steps in order. The setup screen owns the copy for each. */
    enum class Phase {
        START_RUNTIME,
        LOGIN,
        DEPLOY_MILM,
        DEPLOY_MIM,
        QUEUE_MODELS,
        DOWNLOAD_MODELS,
        READY,
    }
}

/**
 * UI-facing view of one downloadable model. The ViewModel mutates these
 * during [BootstrapState.Setup] as mILM streams progress events.
 */
data class ModelSetupItem(
    val id: String,
    val displayName: String,
    val technicalName: String,
    val purpose: String,
    val approxBytes: Long,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val state: State = State.Pending,
    val errorMessage: String? = null,
) {
    enum class State { Pending, Downloading, Ready, Failed }
}
