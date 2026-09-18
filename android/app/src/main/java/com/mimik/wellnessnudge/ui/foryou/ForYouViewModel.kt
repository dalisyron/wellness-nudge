package com.mimik.wellnessnudge.ui.foryou

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimik.wellnessnudge.api.TipCard
import com.mimik.wellnessnudge.api.TipsResponse
import com.mimik.wellnessnudge.data.NudgeRepository
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** A nudge the user marked helpful, quoted on its focus card. */
@Immutable
data class HelpfulNudge(val id: String, val ts: Long, val text: String)

/** A goal the user has been asking about lately, with the nudges that helped with it. */
@Immutable
data class FocusArea(
    val category: String,
    /** How many recent nudges asked about this goal. */
    val recentMentions: Int,
    val helpful: List<HelpfulNudge>,
)

/** What For you holds: placeholders, a failure to load, or the focus areas. */
@Immutable
sealed interface ForYouContent {

    data object Loading : ForYouContent

    data object Failed : ForYouContent

    /** Most recent focus first; empty until a nudge is marked helpful. */
    data class Loaded(val areas: List<FocusArea>) : ForYouContent
}

@Immutable
data class ForYouUiState(
    val content: ForYouContent = ForYouContent.Loading,
    /** A pull to refresh is running. Quiet reloads don't set it. */
    val refreshing: Boolean = false,
    /** A one-off snackbar message, e.g. a pull to refresh that failed over loaded cards. */
    val message: String? = null,
)

/** For you's state: the mim's tips, which it builds from the helpful nudges on the phone. */
class ForYouViewModel(private val repository: NudgeRepository) : ViewModel() {

    private val status = MutableStateFlow(LoadStatus())
    private var loading: Job? = null

    val uiState: StateFlow<ForYouUiState> =
        combine(repository.tipsFlow, status) { tips, status ->
            ForYouUiState(forYouContent(tips, status.failed), status.refreshing, status.message)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            // Tips loaded earlier show at once, without a flash of placeholders.
            initialValue = ForYouUiState(forYouContent(repository.tipsFlow.value, false)),
        )

    /** Reloads without the pull indicator: when the tab comes back into view, and on Try again. */
    fun reload() {
        load()
    }

    /**
     * Pull to refresh: the indicator stays up until the tips are back, and briefly at least.
     * If they can't be reloaded while cards are on screen, a message says so.
     */
    fun refresh() {
        if (status.value.refreshing) return
        status.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val started = TimeSource.Monotonic.markNow()
            load().join()
            delay(MinRefreshTime - started.elapsedNow())
            val stale = status.value.failed && repository.tipsFlow.value != null
            status.update { it.copy(refreshing = false, message = if (stale) "Couldn’t refresh just now." else it.message) }
        }
    }

    fun onMessageShown() = status.update { it.copy(message = null) }

    // One load at a time: a pull during a quiet reload waits for that reload.
    private fun load(): Job = loading?.takeIf { it.isActive } ?: viewModelScope.launch {
        status.update { it.copy(failed = false) }
        val failed = try {
            repository.tips()
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
 * The focus areas in [tips] (null until they first load), in the mim's order. A failure only
 * shows while there is nothing to show instead.
 */
internal fun forYouContent(tips: TipsResponse?, failed: Boolean): ForYouContent = when {
    tips != null -> ForYouContent.Loaded(tips.items.orEmpty().mapNotNull { it.toFocusArea() })
    failed -> ForYouContent.Failed
    else -> ForYouContent.Loading
}

/**
 * Null for a tip card without helpful nudges: it would have nothing to show. The card's
 * intro, the mim's template, isn't shown: the card's header already says what it would.
 */
private fun TipCard.toFocusArea(): FocusArea? {
    val helpful = helpfulNudges.orEmpty().map { HelpfulNudge(it.id, it.ts, it.nudge.trim().withTypographicQuotes()) }
    if (helpful.isEmpty()) return null
    return FocusArea(
        category = category,
        recentMentions = recentMentions ?: 0,
        helpful = helpful,
    )
}

// Long enough for the pull indicator to read as a refresh when the phone answers instantly.
private val MinRefreshTime = 600.milliseconds
