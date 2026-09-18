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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/** Where a fresh nudge generation stands; the Nudge screen renders it on `nudge/new`. */
sealed interface GenerationState {

    data object Idle : GenerationState

    /**
     * [startedAtMs] is on the [SystemClock.elapsedRealtime] timeline: when the model took the
     * request, or while [waiting], when it was made. Drive the live timer from it once the
     * request is no longer [waiting] for another nudge to leave the model.
     */
    data class Running(
        val request: NudgeRequest,
        val startedAtMs: Long,
        val waiting: Boolean = false,
    ) : GenerationState

    /** [latencyMs] is the model's time measured by the app, or the mim's own figure when it reports one. */
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
 * Local changes survive a history load that crosses them: a rating still on its way, a nudge
 * made or deleted while the list was loading.
 */
class NudgeRepository(
    private val api: NudgeApi,
    private val latencies: LatencyStore = LatencyStore.InMemory(),
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    // mILM serves one inference at a time: a second call alongside another loses and comes
    // back as a 502. Nudges therefore reach the model one at a time.
    private val inference = Mutex()

    // Feedback writes go out one at a time, so quick Helpful / Not really taps reach the mim
    // in order.
    private val feedbackLock = Mutex()

    // Guards the bookkeeping below and keeps it in step with the caches it describes.
    private val lock = Any()

    // Counts local changes, so a history load can tell which ones it may have missed.
    private var changes = 0L
    private val ratings = HashMap<String, Rating>()
    private val made = ArrayList<Made>()
    private val deleted = HashSet<String>()

    // The newest generate() call, and the lock that keeps it in step with the generation shown.
    private val generationLock = Any()
    private var latestCall: Any? = null

    private val _generation = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generation: StateFlow<GenerationState> = _generation.asStateFlow()

    private val _history = MutableStateFlow<List<NudgeHistoryItem>?>(null)

    /** Newest-first history from the last [history] call, kept in sync with local edits. Null until loaded. */
    val historyFlow: StateFlow<List<NudgeHistoryItem>?> = _history.asStateFlow()

    private val _historyTotal = MutableStateFlow<Int?>(null)

    /**
     * How many nudges the phone stores in all. [historyFlow] holds the newest of them, as many
     * as the mim lists at once (100). Null until the history first loads.
     */
    val historyTotal: StateFlow<Int?> = _historyTotal.asStateFlow()

    // Nudges opened from outside the loaded history, e.g. an older helpful nudge quoted in For you.
    private val _others = MutableStateFlow<Map<String, NudgeHistoryItem>>(emptyMap())

    private val _tips = MutableStateFlow<TipsResponse?>(null)

    /** The last [tips] response, without nudges deleted since. Null until loaded. */
    val tipsFlow: StateFlow<TipsResponse?> = _tips.asStateFlow()

    private val _runtimeUp = MutableStateFlow<Boolean?>(null)

    /**
     * Whether the on-device service answered lately: true after its healthcheck or any call
     * succeeds, false after a healthcheck fails or a call can't connect. Null until known.
     */
    val runtimeUp: StateFlow<Boolean?> = _runtimeUp.asStateFlow()

    /**
     * Generates a nudge for [request], replacing any generation on screen. A repeat of the
     * request already running is ignored (a double tap). While another nudge is still on the
     * model, the request waits its turn ([GenerationState.Running.waiting]); if it is replaced
     * while waiting, it is dropped without reaching the model. Nothing that reached the model is
     * cancelled: leaving the screen or replacing it lets the call finish, as the mim stores the
     * nudge anyway, and it lands in [historyFlow].
     */
    fun generate(request: NudgeRequest) {
        val call = Any()
        synchronized(generationLock) {
            val current = _generation.value
            if (current is GenerationState.Running && current.request == request) return
            latestCall = call
            _generation.value = GenerationState.Running(request, startedAtMs = elapsedRealtime())
        }
        scope.launch {
            if (!inference.tryLock()) {
                settle(call) { GenerationState.Running(request, startedAtMs = elapsedRealtime(), waiting = true) }
                inference.lock()
            }
            try {
                // The clock starts when the model takes the request, not while it waits.
                val startedAt = elapsedRealtime()
                if (!settle(call) { GenerationState.Running(request, startedAt) }) return@launch
                val outcome = createNudge(request, startedAt)
                settle(call) { outcome }
            } finally {
                inference.unlock()
            }
        }
    }

    /**
     * Shows [state] for [call] if it is still the newest generation (by call, not by value: an
     * equal request at the same instant is still another call); a newer request wins.
     */
    private fun settle(call: Any, state: () -> GenerationState): Boolean = synchronized(generationLock) {
        if (latestCall !== call) return false
        _generation.value = state()
        true
    }

    private suspend fun createNudge(request: NudgeRequest, startedAtMs: Long): GenerationState = try {
        val response = reach { api.createNudge(request) }.data
            ?: throw IOException("Empty response from the on-device service")
        val latency = response.latencyMs ?: (elapsedRealtime() - startedAtMs)
        latencies.put(response.id, latency)
        val item = response.toHistoryItem(request, latency, fallbackTs = wallClock())
        synchronized(lock) {
            made += Made(item, ++changes)
            _history.update { cached -> cached?.let { listOf(item) + it.filterNot { old -> old.id == item.id } } }
            _historyTotal.update { it?.plus(1) }
        }
        GenerationState.Success(item, latency, request)
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        GenerationState.Failed(t.toFriendlyMessage(), request)
    }

    /**
     * Loads the newest [limit] nudges (the mim allows 1 to 100) and refreshes [historyFlow]
     * and [historyTotal], keeping local changes the answer may predate.
     */
    suspend fun history(limit: Int = 100): List<NudgeHistoryItem> = withContext(dispatcher) {
        val since = synchronized(lock) { changes }
        val page = reach { api.listHistory(limit) }.data
        val fetched = page?.items.orEmpty().map { it.withKnownLatency() }
        synchronized(lock) {
            // Made here after the list was asked for, so it may be missing from the answer.
            val newer = made.filter { it.change > since && fetched.none { item -> item.id == it.item.id } }.map { it.item }
            val stale = fetched.count { it.id in deleted }
            val items = (newer + fetched)
                .filterNot { it.id in deleted }
                .sortedByDescending { it.ts }
                .map { item -> ratings[item.id]?.takeIf { it.pending > 0 || it.settled > since }?.let { item.withFeedback(it.shown) } ?: item }
            // What the mim answered from now on reflects everything older than the request.
            made.removeAll { it.change <= since }
            ratings.values.removeAll { it.pending == 0 && it.settled <= since }
            _history.value = items
            _historyTotal.value = ((page?.total ?: fetched.size) - stale).coerceAtLeast(0) + newer.size
            items
        }
    }

    /**
     * The nudge with [id]: from the caches first, then the mim's history, then For you's
     * quotes (older helpful nudges the history no longer lists). Null when it no longer exists.
     */
    suspend fun nudge(id: String): NudgeHistoryItem? {
        if (synchronized(lock) { id in deleted }) return null
        cached(id)?.let { return it }
        history().firstOrNull { it.id == id }?.let { return it }
        return quoted(id)?.also { item -> _others.update { it + (id to item) } }
    }

    /** The nudge [id] if the caches hold it right now, e.g. for a screen's first frame. */
    fun peek(id: String): NudgeHistoryItem? = cached(id)

    /** The nudge [id] as the caches hold it, following local changes such as a rating; null while they don't. */
    fun observe(id: String): Flow<NudgeHistoryItem?> =
        combine(_history, _generation, _others) { history, generation, others -> find(id, history, generation, others) }
            .distinctUntilChanged()

    /**
     * Saves [feedback] for nudge [id] and returns the updated record. The caches update right
     * away. Only the answer to the newest choice updates them again; a failure rolls them back
     * to what the mim last confirmed, unless a newer choice is on its way, and is rethrown.
     */
    suspend fun setFeedback(id: String, feedback: Feedback): NudgeHistoryItem? {
        val version = synchronized(lock) {
            val rating = ratings.getOrPut(id) { Rating(confirmed = cached(id)?.feedback ?: Feedback.Unset) }
            rating.latest = feedback
            rating.pending++
            rating.version++
            changes++
            updateCached(id) { it.withFeedback(feedback) }
            rating.version
        }
        return scope.async {
            feedbackLock.withLock {
                try {
                    val saved = reach { api.updateFeedback(id, FeedbackRequest(feedback.wireValue)) }.data?.withKnownLatency()
                    synchronized(lock) {
                        val rating = ratings.getValue(id)
                        rating.pending--
                        rating.confirmed = saved?.feedback ?: feedback
                        rating.settled = ++changes
                        if (rating.version == version && saved != null) updateCached(id) { saved }
                    }
                    saved ?: cached(id)
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    synchronized(lock) {
                        val rating = ratings.getValue(id)
                        rating.pending--
                        rating.settled = ++changes
                        if (rating.version == version) updateCached(id) { it.withFeedback(rating.confirmed) }
                    }
                    throw t
                }
            }
        }.await()
    }

    /**
     * Permanently deletes nudge [id] from the phone. A nudge that is already gone counts as
     * deleted. It leaves every cache, For you's quotes included, and [nudge] no longer finds
     * it. A fresh result on `nudge/new` stays in [generation], so the screen keeps showing it
     * while it leaves; the next [generate] replaces it.
     */
    suspend fun delete(id: String) {
        scope.async {
            val response = reach { api.deleteNudge(id) }
            if (!response.isSuccessful && response.code() != 404) throw HttpException(response)
            latencies.remove(id)
            synchronized(lock) {
                deleted += id
                changes++
                made.removeAll { it.item.id == id }
                ratings.remove(id)
                _history.update { cached -> cached?.filterNot { it.id == id } }
                if (response.isSuccessful) _historyTotal.update { total -> total?.minus(1)?.coerceAtLeast(0) }
                _others.update { it - id }
            }
            _tips.update { it?.without(setOf(id)) }
        }.await()
    }

    /** Loads the personal tips (helpful nudges grouped by recent focus) and refreshes [tipsFlow]. */
    suspend fun tips(): TipsResponse = withContext(dispatcher) {
        val loaded = reach { api.getTips() }.data ?: TipsResponse(items = emptyList(), totalRecent = 0, generatedAt = null)
        val tips = loaded.without(synchronized(lock) { deleted.toSet() })
        _tips.value = tips
        tips
    }

    /**
     * The mim's healthcheck answer, or null when it can't be reached within a few seconds
     * (loopback answers at once; the API client's long timeout is sized for model output).
     * Updates [runtimeUp].
     */
    suspend fun health(): HealthStatus? {
        val health = try {
            withTimeout(HealthTimeoutMillis) { api.health().data }
        } catch (e: TimeoutCancellationException) {
            null
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            null
        }
        _runtimeUp.value = health?.isHealthy == true
        return health
    }

    /** Checks the mim's health in the background, e.g. after the runtime sheet closes. */
    fun refreshHealth() {
        scope.launch { health() }
    }

    /** Cancels in-flight work. The repository can't be used afterwards. */
    fun close() {
        scope.cancel()
    }

    /** Runs [call] against the mim, noting in [runtimeUp] that it answered, or that it can't be reached. */
    private suspend fun <T> reach(call: suspend () -> T): T {
        val result = try {
            call()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (t is ConnectException) _runtimeUp.value = false
            throw t
        }
        _runtimeUp.value = true
        return result
    }

    private fun cached(id: String): NudgeHistoryItem? = find(id, _history.value, _generation.value, _others.value)

    private fun find(
        id: String,
        history: List<NudgeHistoryItem>?,
        generation: GenerationState,
        others: Map<String, NudgeHistoryItem>,
    ): NudgeHistoryItem? {
        if (synchronized(lock) { id in deleted }) return null
        return history?.firstOrNull { it.id == id }
            ?: (generation as? GenerationState.Success)?.item?.takeIf { it.id == id }
            ?: others[id]
    }

    private fun updateCached(id: String, transform: (NudgeHistoryItem) -> NudgeHistoryItem) {
        _history.update { cached -> cached?.map { if (it.id == id) transform(it) else it } }
        _others.update { others -> others[id]?.let { others + (id to transform(it)) } ?: others }
        _generation.update { state ->
            if (state is GenerationState.Success && state.item.id == id) state.copy(item = transform(state.item)) else state
        }
    }

    /**
     * An older helpful nudge as For you quotes it: its text, time and category, without the
     * signals behind it, which only the history carries.
     */
    private fun quoted(id: String): NudgeHistoryItem? {
        _tips.value?.items.orEmpty().forEach { card ->
            val quote = card.helpfulNudges.orEmpty().firstOrNull { it.id == id } ?: return@forEach
            return NudgeHistoryItem(
                id = quote.id,
                ts = quote.ts,
                metrics = null,
                userGoal = null,
                category = card.category,
                nudge = quote.nudge,
                model = null,
                helpful = Feedback.Helpful.wireValue,
                latencyMs = latencies.get(quote.id),
            )
        }
        return null
    }

    private fun NudgeHistoryItem.withKnownLatency(): NudgeHistoryItem =
        if (latencyMs != null) this else latencies.get(id)?.let { copy(latencyMs = it) } ?: this

    /** A rating on its way to the mim, or settled lately, for one nudge. */
    private class Rating(var confirmed: Feedback) {
        /** The newest choice. */
        var latest: Feedback = confirmed

        /** Counts the choices made, so an answer can tell whether it is the newest one's. */
        var version = 0

        /** Choices sent but not answered yet. */
        var pending = 0

        /** The change count when the last answer came back. */
        var settled = 0L

        /** What the caches show: the newest choice while one is on its way, else what the mim confirmed. */
        val shown: Feedback get() = if (pending > 0) latest else confirmed
    }

    /** A nudge generated here, with the change count when it arrived. */
    private class Made(val item: NudgeHistoryItem, val change: Long)

    private companion object {
        const val HealthTimeoutMillis = 3_000L
    }
}

private fun NudgeHistoryItem.withFeedback(feedback: Feedback) = copy(helpful = feedback.wireValue)

/** These tips without the nudges in [ids]; a card left with none drops out of For you. */
private fun TipsResponse.without(ids: Set<String>): TipsResponse {
    if (ids.isEmpty()) return this
    return copy(items = items?.map { card -> card.copy(helpfulNudges = card.helpfulNudges?.filterNot { it.id in ids }) })
}

private fun Throwable.toFriendlyMessage(): String = when {
    this is SocketTimeoutException || (this is HttpException && code() == 504) ->
        "The on-device model took too long to answer. Give it another try."
    this is ConnectException ->
        "Couldn’t reach the on-device service. It may still be starting, so give it a moment and try again."
    this is HttpException && code() == 502 ->
        "The on-device model couldn’t finish that nudge. Give it another try."
    this is HttpException && code() == 400 ->
        "The on-device service couldn’t use those signals. Adjust them and try again."
    else -> "Something went wrong on the phone. Give it another try."
}
