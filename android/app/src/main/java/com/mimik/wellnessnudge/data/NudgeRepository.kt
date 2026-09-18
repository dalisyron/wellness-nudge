package com.mimik.wellnessnudge.data

import android.os.SystemClock
import com.mimik.wellnessnudge.api.FeedbackRequest
import com.mimik.wellnessnudge.api.HealthStatus
import com.mimik.wellnessnudge.api.NudgeApi
import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.api.TipsResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/** Where a fresh nudge generation stands; the Nudge screen renders it on `nudge/new`. */
sealed interface GenerationState {

    data object Idle : GenerationState

    /** [startedAtMs] is on the [SystemClock.elapsedRealtime] timeline; drive the live timer from it. */
    data class Running(val request: NudgeRequest, val startedAtMs: Long) : GenerationState

    /** [latencyMs] is the round trip measured by the app, or the mim's own figure when it reports one. */
    data class Success(
        val item: NudgeHistoryItem,
        val latencyMs: Long,
        val request: NudgeRequest,
    ) : GenerationState

    /** [message] is user-facing copy; [request] lets the screen offer "Try again". */
    data class Failed(val message: String, val request: NudgeRequest) : GenerationState
}

/**
 * App-scoped access to the wellness-nudge mim running on the phone.
 *
 * Owns the generation state machine and the caches the screens share, so a rating given on
 * the Nudge screen shows up in the Journal without a refetch. Writes (feedback, delete) run
 * in the repository's own scope: leaving a screen right after tapping doesn't drop them.
 */
class NudgeRepository(
    private val api: NudgeApi,
    private val latencies: LatencyStore = LatencyStore.InMemory(),
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var generationJob: Job? = null

    private val _generation = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generation: StateFlow<GenerationState> = _generation.asStateFlow()

    private val _history = MutableStateFlow<List<NudgeHistoryItem>?>(null)

    /** Newest-first history from the last [history] call, kept in sync with local edits. Null until loaded. */
    val historyFlow: StateFlow<List<NudgeHistoryItem>?> = _history.asStateFlow()

    private val _tips = MutableStateFlow<TipsResponse?>(null)

    /** The last [tips] response. Null until loaded. */
    val tipsFlow: StateFlow<TipsResponse?> = _tips.asStateFlow()

    /**
     * Generates a nudge for [request], replacing any generation in flight. Leaving the screen
     * doesn't cancel it: the mim still stores the nudge and it lands in [historyFlow].
     */
    fun generate(request: NudgeRequest) {
        generationJob?.cancel()
        val running = GenerationState.Running(request, startedAtMs = elapsedRealtime())
        _generation.value = running
        generationJob = scope.launch {
            val outcome = try {
                val response = api.createNudge(request).data
                    ?: throw IOException("Empty response from the on-device service")
                val latency = response.latencyMs ?: (elapsedRealtime() - running.startedAtMs)
                latencies.put(response.id, latency)
                val item = response.toHistoryItem(request, latency, fallbackTs = wallClock())
                _history.update { cached -> cached?.let { listOf(item) + it.filterNot { old -> old.id == item.id } } }
                GenerationState.Success(item, latency, request)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                GenerationState.Failed(t.toFriendlyMessage(), request)
            }
            // Only settle the generation this job started; a reset or a newer request wins.
            _generation.compareAndSet(running, outcome)
        }
    }

    /** Forgets the current generation. A request still in flight completes into the history. */
    fun resetGeneration() {
        _generation.value = GenerationState.Idle
    }

    /** Loads the newest [limit] nudges (the mim allows 1 to 100) and refreshes [historyFlow]. */
    suspend fun history(limit: Int = 100): List<NudgeHistoryItem> = withContext(dispatcher) {
        val items = api.listHistory(limit).data?.items.orEmpty().map { it.withKnownLatency() }
        _history.value = items
        items
    }

    /** The nudge with [id], from the caches first and the mim otherwise; null when it no longer exists. */
    suspend fun nudge(id: String): NudgeHistoryItem? = cached(id) ?: history().firstOrNull { it.id == id }

    /**
     * Saves [feedback] for nudge [id] and returns the updated record. The caches update right
     * away and roll back if the mim rejects the change.
     */
    suspend fun setFeedback(id: String, feedback: Feedback): NudgeHistoryItem? {
        val previous = cached(id)
        updateCached(id) { it.copy(helpful = feedback.wireValue) }
        return scope.async {
            try {
                val saved = api.updateFeedback(id, FeedbackRequest(feedback.wireValue)).data?.withKnownLatency()
                if (saved != null) updateCached(id) { saved }
                saved ?: cached(id)
            } catch (t: Throwable) {
                if (previous != null) updateCached(id) { previous }
                throw t
            }
        }.await()
    }

    /** Permanently deletes nudge [id] from the phone. A nudge that is already gone counts as deleted. */
    suspend fun delete(id: String) {
        scope.async {
            val response = api.deleteNudge(id)
            if (!response.isSuccessful && response.code() != 404) throw HttpException(response)
            latencies.remove(id)
            _history.update { cached -> cached?.filterNot { it.id == id } }
            _generation.update { state ->
                if (state is GenerationState.Success && state.item.id == id) GenerationState.Idle else state
            }
        }.await()
    }

    /** Loads the personal tips (helpful nudges grouped by recent focus) and refreshes [tipsFlow]. */
    suspend fun tips(): TipsResponse = withContext(dispatcher) {
        val tips = api.getTips().data ?: TipsResponse(items = emptyList(), totalRecent = 0, generatedAt = null)
        _tips.value = tips
        tips
    }

    /** The mim's healthcheck answer, or null when it can't be reached. */
    suspend fun health(): HealthStatus? = try {
        api.health().data
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        null
    }

    /** Cancels in-flight work. The repository can't be used afterwards. */
    fun close() {
        scope.cancel()
    }

    private fun cached(id: String): NudgeHistoryItem? =
        _history.value?.firstOrNull { it.id == id }
            ?: (_generation.value as? GenerationState.Success)?.item?.takeIf { it.id == id }

    private fun updateCached(id: String, transform: (NudgeHistoryItem) -> NudgeHistoryItem) {
        _history.update { cached -> cached?.map { if (it.id == id) transform(it) else it } }
        _generation.update { state ->
            if (state is GenerationState.Success && state.item.id == id) state.copy(item = transform(state.item)) else state
        }
    }

    private fun NudgeHistoryItem.withKnownLatency(): NudgeHistoryItem =
        if (latencyMs != null) this else latencies.get(id)?.let { copy(latencyMs = it) } ?: this
}

private fun Throwable.toFriendlyMessage(): String = when {
    this is SocketTimeoutException || (this is HttpException && code() == 504) ->
        "The on-device model took too long to answer. Give it another try."
    this is ConnectException ->
        "Couldn't reach the on-device service. It may still be starting, so give it a moment and try again."
    this is HttpException && code() == 502 ->
        "The on-device model couldn't finish that nudge. Give it another try."
    this is HttpException && code() == 400 ->
        "The on-device service couldn't use those signals. Adjust them and try again."
    else -> "Something went wrong on the phone. Give it another try."
}
