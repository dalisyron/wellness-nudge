package com.mimik.wellnessnudge.ui.nudge

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.data.GenerationState
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.data.feedback
import com.mimik.wellnessnudge.data.toRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Nudge screen's state and events. On `nudge/new` ([nudgeId] null) it follows the
 * repository's generation; on `nudge/saved/{id}` it loads that nudge, cache first. Ratings
 * and deletes go through the repository's caches, so the Journal and For you see them at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NudgeViewModel(
    private val repository: NudgeRepository,
    private val nudgeId: String?,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {

    private val session = MutableStateFlow(Session())
    private val loadAttempts = MutableStateFlow(0)

    val uiState: StateFlow<NudgeUiState> =
        combine(if (nudgeId == null) generationContent() else savedContent(nudgeId), session) { content, current ->
            present(content, current)
        }.stateIn(
            scope = viewModelScope,
            // The elapsed time only ticks while the screen is watching.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = present(initialContent(), session.value),
        )

    /** Rates the nudge on screen. The choice shows at once and rolls back if it can't be saved. */
    fun setFeedback(feedback: Feedback) {
        val nudge = shownNudge() ?: return
        if (nudge.feedback == feedback) return
        viewModelScope.launch {
            try {
                repository.setFeedback(nudge.id, feedback)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                session.update { it.copy(message = "Couldn't save your feedback.") }
            }
        }
    }

    fun requestDelete() {
        if (shownNudge() != null) session.update { it.copy(confirmingDelete = true) }
    }

    fun dismissDelete() = session.update { it.copy(confirmingDelete = false) }

    /** Deletes the nudge on screen; [NudgeUiState.deleted] then tells the screen to leave. */
    fun delete() {
        val nudge = shownNudge() ?: return
        val current = session.value
        if (current.deleting || current.deleted) return
        session.update { it.copy(confirmingDelete = false, deleting = true) }
        viewModelScope.launch {
            try {
                repository.delete(nudge.id)
                session.update { it.copy(deleting = false, deleted = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                session.update { it.copy(deleting = false, message = "Couldn't delete this nudge.") }
            }
        }
    }

    /** Loads the saved nudge again after [NudgeContent.LoadFailed]. */
    fun retryLoad() = loadAttempts.update { it + 1 }

    /** The fresh nudge [id] has been revealed: show it whole from now on, e.g. after recreation. */
    fun onRevealed(id: String) = session.update { it.copy(revealed = it.revealed + id) }

    fun onMessageShown() = session.update { it.copy(message = null) }

    private fun shownNudge(): NudgeHistoryItem? = (uiState.value.content as? NudgeContent.Result)?.nudge

    /** `nudge/new`: the generation, with the elapsed time ticking while the model writes. */
    private fun generationContent(): Flow<NudgeContent> = repository.generation
        // Idle: the shell is closing the screen. Keep the last stage on screen while it leaves.
        .filterNot { it is GenerationState.Idle }
        .transformLatest { generation ->
            if (generation is GenerationState.Running) {
                val signals = generation.request.toSignals()
                while (true) {
                    emit(NudgeContent.Generating(signals, elapsedMs = elapsedRealtime() - generation.startedAtMs))
                    delay(ElapsedTickMillis)
                }
            } else {
                generation.toContent()?.let { emit(it) }
            }
        }

    /**
     * `nudge/saved/{id}`: the saved nudge, kept in step with the caches so a rating shows at
     * once. A delete drops it from the caches; the last copy stays on screen while it leaves.
     */
    private fun savedContent(id: String): Flow<NudgeContent> = loadAttempts.transformLatest { attempt ->
        if (attempt > 0) emit(NudgeContent.Loading)
        val loaded = try {
            repository.nudge(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(NudgeContent.LoadFailed)
            return@transformLatest
        }
        if (loaded == null) {
            emit(NudgeContent.Missing)
            return@transformLatest
        }
        emitAll(
            combine(repository.historyFlow, repository.generation) { history, generation ->
                cached(id, history, generation)
            }
                .filterNotNull()
                .onStart { emit(loaded) }
                .distinctUntilChanged()
                .map { it.toResult(reveal = false) },
        )
    }

    /** What to show before the flows first answer: whatever the caches already hold. */
    private fun initialContent(): NudgeContent {
        val generation = repository.generation.value
        return if (nudgeId == null) {
            generation.toContent()
        } else {
            cached(nudgeId, repository.historyFlow.value, generation)?.toResult(reveal = false)
        } ?: NudgeContent.Loading
    }

    private fun GenerationState.toContent(): NudgeContent? = when (this) {
        GenerationState.Idle -> null
        is GenerationState.Running -> NudgeContent.Generating(request.toSignals(), elapsedRealtime() - startedAtMs)
        is GenerationState.Success ->
            item.toResult(reveal = true, latencyMs = item.latencyMs ?: latencyMs, request = request)
        is GenerationState.Failed -> NudgeContent.GenerationFailed(message, request)
    }

    private fun present(content: NudgeContent, session: Session) = NudgeUiState(
        content = if (content is NudgeContent.Result && content.nudge.id in session.revealed) {
            content.copy(reveal = false)
        } else {
            content
        },
        confirmingDelete = session.confirmingDelete && content is NudgeContent.Result,
        deleted = session.deleted,
        message = session.message,
    )

    /** What happened on this screen, apart from the nudge itself. */
    private data class Session(
        val revealed: Set<String> = emptySet(),
        val confirmingDelete: Boolean = false,
        val deleting: Boolean = false,
        val deleted: Boolean = false,
        val message: String? = null,
    )

    private companion object {
        const val ElapsedTickMillis = 100L
    }
}

/**
 * A saved or fresh nudge as the screen shows it, grounded in the signals of its record. A
 * saved nudge regenerates from its record; a fresh one from the exact request it came from.
 */
internal fun NudgeHistoryItem.toResult(
    reveal: Boolean,
    latencyMs: Long? = this.latencyMs,
    request: NudgeRequest? = null,
): NudgeContent.Result {
    val recorded = toRequest()
    return NudgeContent.Result(
        nudge = this,
        signals = recorded.toSignals(),
        latencyMs = latencyMs?.takeIf { it > 0 },
        request = request ?: recorded,
        reveal = reveal,
    )
}

/** The nudge [id] from the history cache, or the fresh result that hasn't reached it. */
private fun cached(id: String, history: List<NudgeHistoryItem>?, generation: GenerationState): NudgeHistoryItem? =
    history?.firstOrNull { it.id == id } ?: (generation as? GenerationState.Success)?.item?.takeIf { it.id == id }
