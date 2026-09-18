package com.mimik.wellnessnudge.ui.journal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.ui.components.EmptyState
import com.mimik.wellnessnudge.ui.components.IconBadge
import com.mimik.wellnessnudge.ui.components.ScreenTitle
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.components.SectionHeader
import com.mimik.wellnessnudge.ui.components.SkeletonBlock
import com.mimik.wellnessnudge.ui.components.SuggestionChip
import com.mimik.wellnessnudge.ui.components.WellnessCard
import com.mimik.wellnessnudge.ui.format.CategoryStyle
import com.mimik.wellnessnudge.ui.format.LocalWellnessClock
import com.mimik.wellnessnudge.ui.format.dayLabel
import com.mimik.wellnessnudge.ui.format.timeLabel
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import java.time.LocalDate

/**
 * The Journal tab: every nudge saved on the phone, newest first, in days under pinned
 * headers and filtered by rating. Tapping an entry opens it; pulling down reloads.
 * Stateless: [JournalRoute] connects it to [JournalViewModel].
 *
 * @param contentPadding bottom space taken by the floating tab bar; the list scrolls under it.
 * @param listState the scroll position, hoisted so tests can render the list scrolled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    state: JournalUiState,
    onSelectFilter: (JournalFilter) -> Unit,
    onOpenNudge: (id: String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onCreateNudge: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val colors = WellnessTheme.colors
    val pullState = rememberPullToRefreshState()
    val pinnedDay by rememberPinnedDay(listState, (state.content as? JournalContent.Loaded)?.days.orEmpty())
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            // The list starts below the status bar: day headers pin there.
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        state = pullState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = state.refreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = colors.controlFill,
                color = colors.accent,
            )
        },
    ) {
        // The viewport's height lets the empty state fill the screen, yet scroll where it doesn't fit.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewportHeight = maxHeight
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = contentPadding) {
                when (val content = state.content) {
                    JournalContent.Loading -> {
                        item(key = TitleKey) { JournalTitle(subtitle = null, subtitlePlaceholder = true) }
                        filters(state.filter, onSelectFilter)
                        placeholders()
                        end()
                    }
                    JournalContent.Failed -> {
                        item(key = TitleKey) { JournalTitle(subtitle = null) }
                        item(key = "error") { LoadErrorCard(onRetry) }
                        end()
                    }
                    is JournalContent.Loaded -> if (content.total == 0) {
                        item(key = "empty") {
                            val bottom = contentPadding.calculateBottomPadding()
                            EmptyJournal(onCreateNudge, Modifier.heightIn(min = viewportHeight - bottom))
                        }
                    } else {
                        item(key = TitleKey) { JournalTitle(subtitle = countLabel(content, state.filter)) }
                        filters(state.filter, onSelectFilter)
                        if (content.days.isEmpty()) {
                            item(key = "no-matches") { NoMatches(state.filter) }
                        } else {
                            days(content.days, pinnedDay = { pinnedDay }, onOpenNudge)
                        }
                        end()
                    }
                }
            }
        }
    }
}

/** "12 nudges · newest first", or "5 of 12 nudges · newest first" under a filter. */
private fun countLabel(content: JournalContent.Loaded, filter: JournalFilter): String {
    val total = if (content.total == 1) "1 nudge" else "${content.total} nudges"
    val count = if (filter == JournalFilter.All) total else "${content.shown} of $total"
    return "$count · newest first"
}

@Composable
private fun JournalTitle(
    subtitle: String?,
    modifier: Modifier = Modifier,
    subtitlePlaceholder: Boolean = false,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(top = TitleTop),
    ) {
        ScreenTitle(title = "Journal", eyebrow = "Stored on this phone", subtitle = subtitle)
        if (subtitlePlaceholder) {
            // Holds the count's line, so the list doesn't shift when it arrives.
            Spacer(Modifier.height(8.dp))
            val line = with(LocalDensity.current) { MaterialTheme.typography.bodyMedium.lineHeight.toDp() }
            Box(Modifier.height(line), contentAlignment = Alignment.CenterStart) {
                SkeletonBlock(Modifier.size(width = 148.dp, height = 12.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.filters(selected: JournalFilter, onSelect: (JournalFilter) -> Unit) {
    item(key = "filters", contentType = "filters") {
        // Chips lay out 48 dp tall around a 36 dp pill, so wrapped rows need no extra spacing.
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = WellnessSpacing.ScreenMargin)
                .padding(top = 14.dp, bottom = 6.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JournalFilter.entries.forEach { filter ->
                SuggestionChip(
                    text = filter.label,
                    selected = filter == selected,
                    onClick = { onSelect(filter) },
                    checkWhenSelected = true,
                )
            }
        }
    }
}

/**
 * Each day under its sticky header. Headers are keyed by their date and entries by their id,
 * which is how [rememberPinnedDay] tells them apart.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.days(
    days: List<JournalDay>,
    pinnedDay: () -> LocalDate?,
    onOpenNudge: (id: String) -> Unit,
) {
    days.forEach { day ->
        stickyHeader(key = day.date, contentType = "day") {
            // Read here, so only the headers recompose when another one pins.
            DayHeader(day, pinned = pinnedDay() == day.date)
        }
        itemsIndexed(day.entries, key = { _, entry -> entry.id }, contentType = { _, _ -> "entry" }) { index, entry ->
            JournalEntryRow(
                entry = entry,
                connectsUp = index > 0,
                connectsDown = index < day.entries.lastIndex,
                onClick = { onOpenNudge(entry.id) },
            )
        }
    }
}

/** Room between the last entry and the tab bar. */
private fun LazyListScope.end() {
    item(key = "end") { Spacer(Modifier.height(16.dp)) }
}

/**
 * The day's eyebrow. While [pinned] it covers the entries scrolling under it with the canvas
 * itself, so it reads cleanly without looking like a bar.
 */
@Composable
private fun DayHeader(day: JournalDay, pinned: Boolean, modifier: Modifier = Modifier) {
    SectionHeader(
        title = dayLabel(day.entries.first().ts, LocalWellnessClock.current),
        modifier = modifier
            .canvasBackdrop(WellnessTheme.colors, enabled = pinned, fade = WellnessSpacing.EyebrowGap)
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(top = DayHeaderTop, bottom = WellnessSpacing.EyebrowGap),
    )
}

/**
 * The day whose header is pinned: its own place has scrolled above the top of the list, so
 * the header covers the entries passing under it. Null while every header sits in place.
 */
@Composable
private fun rememberPinnedDay(listState: LazyListState, days: List<JournalDay>): State<LocalDate?> {
    val dayOfEntry = remember(days) { days.flatMap { day -> day.entries.map { it.id to day.date } }.toMap() }
    return remember(listState, dayOfEntry) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            when (val key = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == first }?.key) {
                is LocalDate -> key.takeIf { listState.firstVisibleItemScrollOffset > 0 }
                is String -> dayOfEntry[key]
                else -> null
            }
        }
    }
}

/** An entry: its category badge on the day's timeline, then its card. */
@Composable
private fun JournalEntryRow(
    entry: JournalEntry,
    connectsUp: Boolean,
    connectsDown: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = CategoryStyle.of(entry.category)
    val badgeTop = badgeTop()
    TimelineRow(connectsUp, connectsDown, badgeTop, modifier) {
        IconBadge(style.icon, style.color, Modifier.padding(top = badgeTop), size = BadgeSize)
        Spacer(Modifier.width(12.dp))
        EntryCard(entry, style, onClick, Modifier.weight(1f))
    }
}

/** The category, time and rating, a three-line preview of the nudge and the goal it answered. */
@Composable
private fun EntryCard(entry: JournalEntry, style: CategoryStyle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    val time = timeLabel(entry.ts, LocalWellnessClock.current)
    WellnessCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = WellnessSpacing.TilePadding,
        shape = WellnessShapes.Tile,
        onClickLabel = "Open nudge",
    ) {
        Row(Modifier.heightIn(min = MarkSize), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = style.contentColor)) { append(style.label) }
                    withStyle(SpanStyle(color = colors.textTertiary)) { append(" · $time") }
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FeedbackMark(entry.feedback)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = entry.text,
            style = WellnessTheme.type.nudgePreview,
            color = colors.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.goal != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Goal · ${entry.goal}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The rating, if any: a thumb in the status hue, named for TalkBack. */
@Composable
private fun FeedbackMark(feedback: Feedback) {
    val colors = WellnessTheme.colors
    when (feedback) {
        Feedback.Helpful -> Icon(
            Icons.Rounded.ThumbUp,
            contentDescription = "Rated helpful",
            tint = colors.success,
            modifier = Modifier.size(MarkSize),
        )
        Feedback.NotHelpful -> Icon(
            Icons.Rounded.ThumbDown,
            contentDescription = "Rated not helpful",
            tint = colors.danger,
            modifier = Modifier.size(MarkSize),
        )
        Feedback.Unset -> Unit
    }
}

/**
 * A row on the day's timeline: a hairline runs through the badge column, broken around the
 * badge, into the entries above ([connectsUp]) and below ([connectsDown]). Rows carry the
 * gap to the next entry themselves, so the line crosses it unbroken.
 */
@Composable
private fun TimelineRow(
    connectsUp: Boolean,
    connectsDown: Boolean,
    badgeTop: Dp,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val line = WellnessTheme.colors.hairlineStrong
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val center = (WellnessSpacing.ScreenMargin + BadgeSize / 2).toPx()
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - center else center
                val stroke = TimelineStroke.toPx()
                val gap = TimelineGap.toPx()
                val top = badgeTop.toPx()
                if (connectsUp) drawLine(line, Offset(x, 0f), Offset(x, top - gap), stroke)
                if (connectsDown) drawLine(line, Offset(x, top + BadgeSize.toPx() + gap), Offset(x, size.height), stroke)
            }
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(bottom = WellnessSpacing.ItemGap),
        content = content,
    )
}

/** Height of a card's first line: the category and time, or the rating mark if taller. */
@Composable
private fun headerLineHeight(): Dp {
    val text = with(LocalDensity.current) { MaterialTheme.typography.labelMedium.lineHeight.toDp() }
    return max(text, MarkSize)
}

/** Centers the badge on the card's first line, however large the text is set. */
@Composable
private fun badgeTop(): Dp = WellnessSpacing.TilePadding + headerLineHeight() / 2 - BadgeSize / 2

private fun LazyListScope.placeholders() {
    item(key = "placeholder-day", contentType = "placeholder-day") {
        val eyebrow = with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() }
        Box(
            Modifier
                .padding(horizontal = WellnessSpacing.ScreenMargin)
                .padding(top = DayHeaderTop, bottom = WellnessSpacing.EyebrowGap)
                .height(eyebrow),
            contentAlignment = Alignment.CenterStart,
        ) {
            SkeletonBlock(Modifier.size(width = 64.dp, height = 10.dp))
        }
    }
    items(PlaceholderLines.size, key = { "placeholder-$it" }, contentType = { "placeholder" }) { index ->
        PlaceholderRow(
            lines = PlaceholderLines[index],
            connectsUp = index > 0,
            connectsDown = index < PlaceholderLines.lastIndex,
        )
    }
}

/**
 * A loading entry: the same frame as a real one, with its lines shimmering in place. [lines]
 * are the lengths of the first line, the three preview lines and the goal line.
 */
@Composable
private fun PlaceholderRow(lines: List<Float>, connectsUp: Boolean, connectsDown: Boolean) {
    val badgeTop = badgeTop()
    val density = LocalDensity.current
    val previewLine = with(density) { WellnessTheme.type.nudgePreview.lineHeight.toDp() }
    val goalLine = with(density) { MaterialTheme.typography.bodySmall.lineHeight.toDp() }
    TimelineRow(connectsUp, connectsDown, badgeTop) {
        SkeletonBlock(
            Modifier
                .padding(top = badgeTop)
                .size(BadgeSize),
            CircleShape,
        )
        Spacer(Modifier.width(12.dp))
        WellnessCard(
            modifier = Modifier.weight(1f),
            contentPadding = WellnessSpacing.TilePadding,
            shape = WellnessShapes.Tile,
        ) {
            PlaceholderLine(headerLineHeight(), lines[0], thickness = 10.dp)
            Spacer(Modifier.height(8.dp))
            PlaceholderLine(previewLine, lines[1])
            PlaceholderLine(previewLine, lines[2])
            PlaceholderLine(previewLine, lines[3])
            Spacer(Modifier.height(10.dp))
            PlaceholderLine(goalLine, lines[4], thickness = 10.dp)
        }
    }
}

@Composable
private fun PlaceholderLine(height: Dp, fraction: Float, thickness: Dp = 12.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.CenterStart,
    ) {
        SkeletonBlock(
            Modifier
                .fillMaxWidth(fraction)
                .height(thickness),
        )
    }
}

@Composable
private fun LoadErrorCard(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    WellnessCard(
        modifier
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(top = 24.dp)
            .fillMaxWidth(),
    ) {
        IconBadge(Icons.Rounded.ErrorOutline, colors.warning, size = 40.dp)
        Spacer(Modifier.height(16.dp))
        Text("Couldn't load your journal", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "The on-device service didn't answer. It may still be starting, so give it a moment and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(20.dp))
        SecondaryButton(text = "Retry", onClick = onRetry, icon = Icons.Rounded.Refresh)
    }
}

@Composable
private fun EmptyJournal(onCreateNudge: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        JournalTitle(subtitle = null)
        // Spacers share whatever height is left, setting the state just above center; on a
        // short screen they collapse and the list scrolls instead.
        Spacer(Modifier.height(32.dp))
        Spacer(Modifier.weight(1f))
        EmptyState(
            title = "Your journal is empty",
            body = "Every nudge you generate is saved here, on this phone.",
            actionText = "Create your first nudge",
            onAction = onCreateNudge,
        )
        Spacer(Modifier.weight(1.4f))
        Spacer(Modifier.height(32.dp))
    }
}

/** A filter that matches nothing, while other nudges exist: the filter's mark and a line on it. */
@Composable
private fun NoMatches(filter: JournalFilter, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    val copy = when (filter) {
        JournalFilter.Helpful -> NoMatchesCopy(
            Icons.Rounded.ThumbUp, colors.success,
            "No helpful nudges yet", "Rate a nudge as helpful and it will show up here.",
        )
        JournalFilter.NotHelpful -> NoMatchesCopy(
            Icons.Rounded.ThumbDown, colors.danger,
            "Nothing marked not helpful", "Nudges you rate as not helpful will show up here.",
        )
        JournalFilter.Unrated -> NoMatchesCopy(
            Icons.Rounded.DoneAll, colors.accent,
            "You've rated every nudge", "New nudges wait here until you rate them.",
        )
        // All matches every nudge; an empty journal shows the empty state instead.
        JournalFilter.All -> return
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(copy.icon, copy.tint, size = 40.dp)
        Spacer(Modifier.height(16.dp))
        Text(copy.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(copy.body, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
    }
}

private class NoMatchesCopy(val icon: ImageVector, val tint: Color, val title: String, val body: String)

private const val TitleKey = "title"

/** Four loading rows, each with its own line lengths so they read as text, not a pattern. */
private val PlaceholderLines = listOf(
    listOf(0.36f, 1f, 0.94f, 0.62f, 0.48f),
    listOf(0.30f, 0.97f, 1f, 0.44f, 0.56f),
    listOf(0.40f, 1f, 0.89f, 0.71f, 0.40f),
    listOf(0.33f, 0.95f, 1f, 0.53f, 0.52f),
)

/** From the status bar to the screen title. */
private val TitleTop = 12.dp

/** Above a day's eyebrow; with the last entry's own gap, a section gap between days. */
private val DayHeaderTop = WellnessSpacing.SectionGap - WellnessSpacing.ItemGap
private val BadgeSize = 32.dp
private val MarkSize = 16.dp
private val TimelineStroke = 1.5.dp
private val TimelineGap = 6.dp
