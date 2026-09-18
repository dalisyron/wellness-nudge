package com.mimik.wellnessnudge.ui.foryou

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.components.BottomBarScrim
import com.mimik.wellnessnudge.ui.components.EmptyState
import com.mimik.wellnessnudge.ui.components.Eyebrow
import com.mimik.wellnessnudge.ui.components.FooterFade
import com.mimik.wellnessnudge.ui.components.IconBadge
import com.mimik.wellnessnudge.ui.components.ListEdgeFade
import com.mimik.wellnessnudge.ui.components.ListTopScrim
import com.mimik.wellnessnudge.ui.components.ScreenTitle
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.components.SkeletonBlock
import com.mimik.wellnessnudge.ui.components.WellnessCard
import com.mimik.wellnessnudge.ui.components.scrolledFraction
import com.mimik.wellnessnudge.ui.format.CategoryStyle
import com.mimik.wellnessnudge.ui.format.LocalWellnessClock
import com.mimik.wellnessnudge.ui.format.dayLabel
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * The For you tab: the nudges the user found helpful, gathered on one card per goal they
 * have been asking about lately, most recent focus first. Tapping a quote opens its nudge;
 * pulling down reloads. Stateless: [ForYouRoute] connects it to [ForYouViewModel]. The list
 * dissolves at both ends instead of being cut: into the edge under the status bar once
 * scrolled, and into the canvas above the floating tab bar.
 *
 * @param contentPadding bottom space taken by the floating tab bar; the list scrolls under it.
 * @param listState the scroll position, hoisted so tests can render the list scrolled.
 * @param onMessageShown [ForYouUiState.message] has been shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForYouScreen(
    state: ForYouUiState,
    onOpenNudge: (id: String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onCreateNudge: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onMessageShown: () -> Unit = {},
) {
    val colors = WellnessTheme.colors
    val pullState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val latestOnMessageShown by rememberUpdatedState(onMessageShown)
    state.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            latestOnMessageShown()
        }
    }
    val edgeFade = with(LocalDensity.current) { ListEdgeFade.toPx() }
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
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
                    ForYouContent.Loading -> {
                        item(key = TitleKey) { ForYouTitle() }
                        items(PlaceholderCards.size, key = { "placeholder-$it" }, contentType = { "placeholder" }) {
                            PlaceholderCard(PlaceholderCards[it], Modifier.cardSpacing())
                        }
                        end()
                    }
                    ForYouContent.Failed -> {
                        item(key = TitleKey) { ForYouTitle() }
                        item(key = "error") { LoadErrorCard(onRetry, Modifier.cardSpacing()) }
                        end()
                    }
                    is ForYouContent.Loaded -> if (content.areas.isEmpty()) {
                        item(key = "empty") {
                            val bottom = contentPadding.calculateBottomPadding()
                            EmptyForYou(onCreateNudge, Modifier.heightIn(min = viewportHeight - bottom))
                        }
                    } else {
                        item(key = TitleKey) { ForYouTitle() }
                        items(content.areas, key = { it.category }, contentType = { "card" }) { area ->
                            FocusCard(area, onOpenNudge, Modifier.cardSpacing())
                        }
                        end()
                    }
                }
            }
        }
        ListTopScrim(progress = { listState.scrolledFraction(edgeFade) })
        BottomBarScrim(contentPadding, Modifier.align(Alignment.BottomCenter))
        // Material's snackbar pads itself by 12 dp; this lines it up with the screen margin.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(contentPadding)
                .padding(horizontal = WellnessSpacing.ScreenMargin - 12.dp),
        )
    }
}

/** The tab's title; the empty state leaves out the subtitle, which its own copy repeats. */
@Composable
private fun ForYouTitle(modifier: Modifier = Modifier, showSubtitle: Boolean = true) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(top = TitleTop, bottom = TitleBottom),
    ) {
        ScreenTitle(title = "For you", eyebrow = "From your ratings, on this phone")
        if (showSubtitle) {
            // ScreenTitle's subtitle, with balanced lines: it wraps without leaving a word alone.
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Nudges you found helpful, grouped by what you’ve been focusing on lately.",
                style = MaterialTheme.typography.bodyMedium.copy(lineBreak = LineBreak.Heading),
                color = WellnessTheme.colors.textSecondary,
            )
        }
    }
}

private fun Modifier.cardSpacing() = this
    .padding(horizontal = WellnessSpacing.ScreenMargin)
    .padding(bottom = WellnessSpacing.ItemGap)

/** Room between the last card and the tab bar, clear of the fade above it. */
private fun LazyListScope.end() {
    item(key = "end") { Spacer(Modifier.height(FooterFade)) }
}

/**
 * One goal: its badge, how many nudges helped and how often it came up lately, then each
 * helpful nudge as a quote. The card is washed with the category's color.
 */
@Composable
private fun FocusCard(area: FocusArea, onOpenNudge: (id: String) -> Unit, modifier: Modifier = Modifier) {
    val colors = WellnessTheme.colors
    val style = CategoryStyle.of(area.category)
    WellnessCard(
        modifier = modifier.fillMaxWidth(),
        glow = style.color.copy(alpha = colors.tintAlpha * GlowStrength),
        glowAlignment = Alignment.TopStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(style.icon, style.color, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = style.label,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = focusSummary(area),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
        area.helpful.forEachIndexed { index, nudge ->
            Spacer(Modifier.height(if (index == 0) FirstQuoteGap else QuoteGap))
            HelpfulQuote(nudge, style.color, onClick = { onOpenNudge(nudge.id) })
        }
    }
}

/** "3 helpful · asked 4× lately". */
private fun focusSummary(area: FocusArea): String = buildString {
    append("${area.helpful.size} helpful")
    when (area.recentMentions) {
        0 -> Unit
        1 -> append(" · asked once lately")
        else -> append(" · asked ${area.recentMentions}× lately")
    }
}

/** A helpful nudge in the serif italic, on a bar of its category's color, dated below. */
@Composable
private fun HelpfulQuote(nudge: HelpfulNudge, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(QuoteShape)
            .clickable(onClickLabel = "Open nudge", role = Role.Button, onClick = onClick),
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(nudge.text, style = WellnessTheme.type.nudgeQuote, color = WellnessTheme.colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            // Named as the Journal names its days: "Yesterday", "Mon, Sep 14".
            Eyebrow(dayLabel(nudge.ts, LocalWellnessClock.current))
        }
    }
}

/** A loading card: the same frame and rhythm as a focus card, with shimmering lines. */
@Composable
private fun PlaceholderCard(text: PlaceholderText, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val type = MaterialTheme.typography
    val title = with(density) { type.titleMedium.lineHeight.toDp() }
    val meta = with(density) { type.bodySmall.lineHeight.toDp() }
    val quote = with(density) { WellnessTheme.type.nudgeQuote.lineHeight.toDp() }
    val date = with(density) { type.labelSmall.lineHeight.toDp() }
    WellnessCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(Modifier.size(40.dp), CircleShape)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                PlaceholderLine(title, 0.34f)
                PlaceholderLine(meta, 0.56f, thickness = 10.dp)
            }
        }
        Spacer(Modifier.height(FirstQuoteGap))
        Row(Modifier.height(IntrinsicSize.Min)) {
            SkeletonBlock(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight(),
                CircleShape,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                text.quote.forEach { PlaceholderLine(quote, it) }
                Spacer(Modifier.height(8.dp))
                PlaceholderLine(date, 0.16f, thickness = 10.dp)
            }
        }
    }
}

/** Line lengths of a loading card's quote, so the cards don't repeat. */
private class PlaceholderText(val quote: List<Float>)

private val PlaceholderCards = listOf(
    PlaceholderText(quote = listOf(1f, 0.96f, 0.58f)),
    PlaceholderText(quote = listOf(1f, 0.92f, 0.76f, 0.4f)),
    PlaceholderText(quote = listOf(0.95f, 1f, 0.5f)),
)

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
    WellnessCard(modifier.fillMaxWidth()) {
        IconBadge(Icons.Rounded.ErrorOutline, colors.warning, size = 40.dp)
        Spacer(Modifier.height(16.dp))
        Text("Couldn’t load your helpful nudges", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "The on-device service didn’t answer. It may still be starting, so give it a moment and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(20.dp))
        SecondaryButton(text = "Try again", onClick = onRetry, icon = Icons.Rounded.Refresh)
    }
}

@Composable
private fun EmptyForYou(onCreateNudge: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        ForYouTitle(showSubtitle = false)
        // Spacers share whatever height is left, setting the state just above center; on a
        // short screen they collapse and the list scrolls instead.
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.weight(1f))
        EmptyState(
            title = "Nothing here yet",
            body = "Mark a nudge as helpful and it will collect here, grouped by the goals you’ve been asking about.",
            actionText = "Create a nudge",
            onAction = onCreateNudge,
        )
        Spacer(Modifier.weight(1.4f))
        Spacer(Modifier.height(32.dp))
    }
}

private const val TitleKey = "title"

/** A shade stronger than a badge's tint, so the category's wash reads on ink as well as paper. */
private const val GlowStrength = 1.25f

/** From the status bar to the screen title. */
private val TitleTop = 12.dp

/** From the title to the first card. */
private val TitleBottom = 24.dp

/** From a card's header to its first quote, and between quotes. */
private val FirstQuoteGap = 16.dp
private val QuoteGap = 20.dp
private val QuoteShape = RoundedCornerShape(8.dp)
