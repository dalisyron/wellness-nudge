package com.mimik.wellnessnudge.ui.journal

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.data.feedback
import com.mimik.wellnessnudge.data.goal
import com.mimik.wellnessnudge.ui.format.withTypographicQuotes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** Which saved nudges the Journal lists, by the rating they were given. */
enum class JournalFilter(val label: String, private val feedback: Feedback?) {
    All("All", null),
    Helpful("Helpful", Feedback.Helpful),
    NotHelpful("Not helpful", Feedback.NotHelpful),
    Unrated("Unrated", Feedback.Unset),
    ;

    fun matches(feedback: Feedback): Boolean = this.feedback == null || this.feedback == feedback
}

/** A saved nudge, as a Journal entry shows it. */
@Immutable
data class JournalEntry(
    val id: String,
    val ts: Long,
    val category: String?,
    val text: String,
    val goal: String?,
    val feedback: Feedback,
)

/** The entries saved on one calendar day, newest first. */
@Immutable
data class JournalDay(val date: LocalDate, val entries: List<JournalEntry>)

/** What the Journal's list holds: placeholders, a failure to load, or the saved nudges. */
@Immutable
sealed interface JournalContent {

    data object Loading : JournalContent

    data object Failed : JournalContent

    /**
     * [total] counts the nudges loaded, the newest [stored] ones on the phone (the mim lists
     * 100 at most); [days] holds the ones the filter lets through.
     */
    data class Loaded(val total: Int, val days: List<JournalDay>, val stored: Int = total) : JournalContent {
        val shown: Int get() = days.sumOf { it.entries.size }

        /** Only the newest of the stored nudges are loaded. */
        val partial: Boolean get() = stored > total
    }
}

@Immutable
data class JournalUiState(
    val content: JournalContent = JournalContent.Loading,
    val filter: JournalFilter = JournalFilter.All,
    /** A pull to refresh is running. Quiet reloads don't set it. */
    val refreshing: Boolean = false,
    /** A one-off snackbar message, e.g. a pull to refresh that failed over a loaded list. */
    val message: String? = null,
)

/**
 * The Journal's state: the repository's shared history, filtered by rating and grouped into
 * days in the time zone the screen shows ([setZone]). The history updates in place when a
 * nudge is rated or deleted elsewhere, so those changes show up here without a refetch.
 */
class JournalViewModel(
    private val repository: NudgeRepository,
    zone: ZoneId,
) : ViewModel() {

    private val filter = MutableStateFlow(JournalFilter.All)
    private val zone = MutableStateFlow(zone)
    private val status = MutableStateFlow(LoadStatus())
    private var loading: Job? = null

    val uiState: StateFlow<JournalUiState> =
        combine(repository.historyFlow, repository.historyTotal, filter, this.zone, status) { history, stored, filter, zone, status ->
            JournalUiState(journalContent(history, status.failed, filter, zone, stored), filter, status.refreshing, status.message)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            // A history loaded earlier shows at once, without a flash of placeholders.
            initialValue = JournalUiState(
                journalContent(repository.historyFlow.value, false, JournalFilter.All, zone, repository.historyTotal.value),
            ),
        )

    fun selectFilter(filter: JournalFilter) {
        this.filter.value = filter
    }

    /** Days follow the phone's time zone, which can change while the app runs. */
    fun setZone(zone: ZoneId) {
        this.zone.value = zone
    }

    /** Reloads without the pull indicator: when the tab comes back into view, and on Try again. */
    fun reload() {
        load()
    }

    /**
     * Pull to refresh: the indicator stays up until the history is back, and briefly at least.
     * If it can't be reloaded while a list is on screen, a message says so.
     */
    fun refresh() {
        if (status.value.refreshing) return
        status.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val started = TimeSource.Monotonic.markNow()
            load().join()
            delay(MinRefreshTime - started.elapsedNow())
            val stale = status.value.failed && repository.historyFlow.value != null
            status.update { it.copy(refreshing = false, message = if (stale) "Couldn’t refresh just now." else it.message) }
        }
    }

    fun onMessageShown() = status.update { it.copy(message = null) }

    // One load at a time: a pull during a quiet reload waits for that reload.
    private fun load(): Job = loading?.takeIf { it.isActive } ?: viewModelScope.launch {
        status.update { it.copy(failed = false) }
        val failed = try {
            repository.history()
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            true
        }
        status.update { it.copy(failed = failed) }
    }.also { loading = it }

    private data class LoadStatus(
        val failed: Boolean = false,
        val refreshing: Boolean = false,
        val message: String? = null,
    )
}

/**
 * The list for [history] (null until it first loads): entries matching [filter], newest
 * first, in days of [zone]. [stored] is how many nudges the phone holds in all, when known.
 * A failure only shows while there is nothing to show instead.
 */
internal fun journalContent(
    history: List<NudgeHistoryItem>?,
    failed: Boolean,
    filter: JournalFilter,
    zone: ZoneId,
    stored: Int? = null,
): JournalContent = when {
    history != null -> JournalContent.Loaded(
        total = history.size,
        days = history
            .filter { filter.matches(it.feedback) }
            .sortedByDescending { it.ts }
            .groupBy { Instant.ofEpochMilli(it.ts).atZone(zone).toLocalDate() }
            .map { (date, items) -> JournalDay(date, items.map { it.toEntry() }) },
        stored = maxOf(stored ?: 0, history.size),
    )
    failed -> JournalContent.Failed
    else -> JournalContent.Loading
}

private fun NudgeHistoryItem.toEntry() = JournalEntry(
    id = id,
    ts = ts,
    category = category,
    text = nudge.trim().withTypographicQuotes(),
    goal = goal,
    feedback = feedback,
)

// Long enough for the pull indicator to read as a refresh when the phone answers instantly.
private val MinRefreshTime = 600.milliseconds
