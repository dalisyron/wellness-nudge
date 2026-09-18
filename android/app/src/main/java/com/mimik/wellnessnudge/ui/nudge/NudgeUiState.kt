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
    /** The nudge is being deleted: its actions wait until that settles. */
    val deleting: Boolean = false,
    /** The nudge is gone: the screen leaves, still showing it. */
    val deleted: Boolean = false,
    /** A one-off snackbar message, e.g. a rating that couldn't be saved. */
    val message: String? = null,
)

/** The main area of the Nudge screen: one stage at a time. */
sealed interface NudgeContent {

    /** A saved nudge on its way from the mim. */
    data object Loading : NudgeContent

    /**
     * The model is writing from [signals]; [elapsedMs] ticks every 100 ms. While [waiting],
     * another nudge is still on the model and this one waits its turn, so no time is counted.
     */
    @Immutable
    data class Generating(val signals: List<Signal>, val elapsedMs: Long, val waiting: Boolean = false) : NudgeContent

    /**
     * A nudge, fresh or saved: [signals] it was grounded in, [latencyMs] it took to write (null
     * when unknown) and the [request] "Try another" sends again (empty when the record doesn't
     * carry its signals). [reveal] brings its words in one by one, for a fresh nudge seen for
     * the first time. [joinsForYou]: rating it helpful puts it in For you, which gathers the
     * helpful nudges of the goals asked about lately; that holds for a fresh nudge with a goal
     * category, so the caption only promises it then.
     */
    @Immutable
    data class Result(
        val nudge: NudgeHistoryItem,
        val signals: List<Signal>,
        val latencyMs: Long?,
        val request: NudgeRequest,
        val reveal: Boolean,
        val joinsForYou: Boolean = false,
    ) : NudgeContent

    /** The generation failed: [message] is user-facing, [request] powers "Try again". */
    @Immutable
    data class GenerationFailed(val message: String, val request: NudgeRequest) : NudgeContent

    /** The saved nudge couldn't be loaded. */
    data object LoadFailed : NudgeContent

    /** The saved nudge no longer exists. */
    data object Missing : NudgeContent
}
