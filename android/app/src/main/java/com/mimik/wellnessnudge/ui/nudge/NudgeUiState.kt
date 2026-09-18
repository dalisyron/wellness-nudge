package com.mimik.wellnessnudge.ui.nudge

import androidx.compose.runtime.Immutable
import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.api.NudgeRequest

/** Everything the Nudge screen shows, built by [NudgeViewModel]. */
@Immutable
data class NudgeUiState(
    val content: NudgeContent,
    /** The delete confirmation is open. */
    val confirmingDelete: Boolean = false,
    /** The nudge is gone: the screen leaves, still showing it. */
    val deleted: Boolean = false,
    /** A one-off snackbar message, e.g. a rating that couldn't be saved. */
    val message: String? = null,
)

/** The main area of the Nudge screen: one stage at a time. */
sealed interface NudgeContent {

    /** A saved nudge on its way from the mim. */
    data object Loading : NudgeContent

    /** The model is writing from [signals]; [elapsedMs] ticks every 100 ms. */
    @Immutable
    data class Generating(val signals: List<Signal>, val elapsedMs: Long) : NudgeContent

    /**
     * A nudge, fresh or saved: [signals] it was grounded in, [latencyMs] it took to write (null
     * when unknown) and the [request] "Try another" sends again. [reveal] brings its words in
     * one by one, for a fresh nudge seen for the first time.
     */
    @Immutable
    data class Result(
        val nudge: NudgeHistoryItem,
        val signals: List<Signal>,
        val latencyMs: Long?,
        val request: NudgeRequest,
        val reveal: Boolean,
    ) : NudgeContent

    /** The generation failed: [message] is user-facing, [request] powers "Try again". */
    @Immutable
    data class GenerationFailed(val message: String, val request: NudgeRequest) : NudgeContent

    /** The saved nudge couldn't be loaded. */
    data object LoadFailed : NudgeContent

    /** The saved nudge no longer exists. */
    data object Missing : NudgeContent
}
