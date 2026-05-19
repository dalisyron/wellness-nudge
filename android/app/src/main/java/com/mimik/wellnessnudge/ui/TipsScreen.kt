package com.mimik.wellnessnudge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.api.NudgeApi
import com.mimik.wellnessnudge.api.TipCard
import com.mimik.wellnessnudge.api.TipNudge
import com.mimik.wellnessnudge.api.TipsResponse
import com.mimik.wellnessnudge.ui.components.CategoryChip
import com.mimik.wellnessnudge.ui.components.RuntimeStatus
import com.mimik.wellnessnudge.ui.components.SectionLabel
import com.mimik.wellnessnudge.ui.components.WellnessTopBar
import com.mimik.wellnessnudge.ui.theme.LocalStatusColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsScreen(api: NudgeApi) {
    var tips by remember { mutableStateOf<TipsResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        try {
            tips = api.getTips().data
        } catch (t: Throwable) {
            error = t.message ?: "Failed to load tips"
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        load()
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        WellnessTopBar(status = RuntimeStatus.Ready)

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    load()
                    refreshing = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                loading -> Centered { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
                error != null -> Centered {
                    Text(
                        "We couldn't load your tips right now. Pull down to try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                tips == null || tips?.items.isNullOrEmpty() -> EmptyState(tips?.totalRecent ?: 0)
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { Header() }
                    items(tips!!.items.orEmpty(), key = { it.category }) { card ->
                        TipCardView(card)
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            "Personal Tips",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "What's worked for you, grouped by what you've been asking about lately. Pull down to refresh.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EmptyState(recentCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Nothing here yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (recentCount == 0) {
                "Generate a few nudges on the Nudge tab, then mark the ones that resonate. " +
                    "Tips will appear here based on what's been helpful for you."
            } else {
                "Open a past nudge from History and tap Helpful on the ones that resonated. " +
                    "Helpful nudges show up here, grouped by goal."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val DATE_FMT = SimpleDateFormat("MMM d", Locale.getDefault())

@Composable
private fun TipCardView(card: TipCard) {
    val memoryBg = LocalStatusColors.current.memoryBackground
    val memoryText = LocalStatusColors.current.memoryText
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp),
    ) {
        CategoryChip(label = humanCategory(card.category))
        Spacer(Modifier.height(12.dp))
        // Memory-styled intro panel (pale blue) — matches the "By the way" tone
        // from Stitch's nudge_with_memory_note mockup.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(memoryBg)
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = memoryText,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "PERSONALIZED TIP",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.dp.value.sp),
                    color = memoryText,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = card.intro ?: "Here are a few past nudges that helped you in this area.",
                style = MaterialTheme.typography.bodyMedium,
                color = memoryText,
            )
        }
        Spacer(Modifier.height(14.dp))
        SectionLabel(text = "What helped before")
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        card.helpfulNudges.orEmpty().forEach { nudge ->
            HelpfulNudgeRow(nudge)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun HelpfulNudgeRow(n: TipNudge) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = n.nudge,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = DATE_FMT.format(Date(n.ts)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Hack: TextUnit/Sp import not loaded for letterSpacing on labelSmall — use a
// helper. (Compose lets us multiply Dp.value by sp for tracked labels.)
private val Float.sp get() = androidx.compose.ui.unit.TextUnit(this, androidx.compose.ui.unit.TextUnitType.Sp)
