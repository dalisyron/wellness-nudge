package com.mimik.wellnessnudge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.api.FeedbackRequest
import com.mimik.wellnessnudge.api.NudgeApi
import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.ui.components.CategoryChip
import com.mimik.wellnessnudge.ui.components.FeedbackChip
import com.mimik.wellnessnudge.ui.components.FeedbackIntent
import com.mimik.wellnessnudge.ui.components.RuntimeStatus
import com.mimik.wellnessnudge.ui.components.WellnessTopBar
import kotlinx.coroutines.launch

@Composable
fun ResultScreen(api: NudgeApi, nudgeId: String, onDone: () -> Unit) {
    var item by remember { mutableStateOf<NudgeHistoryItem?>(null) }
    var helpful by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionInFlight by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(nudgeId) {
        loading = true
        try {
            val list = api.listHistory(50).data?.items.orEmpty()
            val match = list.firstOrNull { it.id == nudgeId }
            item = match
            helpful = match?.helpful
        } finally {
            loading = false
        }
    }

    fun submitFeedback(value: String) {
        if (actionInFlight) return
        actionInFlight = true
        actionError = null
        scope.launch {
            try {
                api.updateFeedback(nudgeId, FeedbackRequest(value))
                helpful = value
            } catch (t: Throwable) {
                actionError = "Couldn't save feedback. Tap again to retry."
            } finally {
                actionInFlight = false
            }
        }
    }

    fun deleteAndExit() {
        if (actionInFlight) return
        actionInFlight = true
        actionError = null
        scope.launch {
            try {
                val resp = api.deleteNudge(nudgeId)
                if (resp.isSuccessful || resp.code() == 404) {
                    // 404 means it's already gone — either way, exit.
                    onDone()
                } else {
                    actionError = "Couldn't delete this nudge (${resp.code()}). Try again."
                }
            } catch (t: Throwable) {
                actionError = "Couldn't delete this nudge. Tap Clear again to retry."
            } finally {
                actionInFlight = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        WellnessTopBar(status = RuntimeStatus.Ready)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                "Your nudge",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        when {
            loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            item == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "We couldn't find that nudge.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> NudgeCard(
                item = item!!,
                helpful = helpful,
                actionInFlight = actionInFlight,
                actionError = actionError,
                onHelpful = { submitFeedback("yes") },
                onNotHelpful = { submitFeedback("no") },
                onClear = { deleteAndExit() },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NudgeCard(
    item: NudgeHistoryItem,
    helpful: String?,
    actionInFlight: Boolean,
    actionError: String?,
    onHelpful: () -> Unit,
    onNotHelpful: () -> Unit,
    onClear: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            // 3dp dark-green accent on the left edge (Stitch nudge card).
            .drawBehind {
                drawRect(
                    color = accent,
                    topLeft = Offset(0f, 0f),
                    size = Size(3.dp.toPx(), size.height),
                )
            }
            .padding(16.dp),
    ) {
        if (!item.category.isNullOrEmpty() && item.category != "other") {
            CategoryChip(label = humanCategory(item.category))
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = item.nudge,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Did this help?",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FeedbackChip(
                text = "Helpful",
                icon = Icons.Filled.ThumbUp,
                selected = helpful == "yes",
                intent = FeedbackIntent.Positive,
                onClick = onHelpful,
            )
            FeedbackChip(
                text = "Not helpful",
                icon = Icons.Filled.ThumbDown,
                selected = helpful == "no",
                intent = FeedbackIntent.Negative,
                onClick = onNotHelpful,
            )
            Spacer(Modifier.weight(1f))
            // Clear deletes this nudge from local storage and pops back.
            TextButton(onClick = onClear, enabled = !actionInFlight) {
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Inline status row: spinner while an action is in flight, or a
        // friendly error if one just failed.
        if (actionInFlight) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Saving…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (actionError != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = actionError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        item.model?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Model: $it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun humanCategory(slug: String): String = when (slug) {
    "improve-sleep" -> "Sleep"
    "weight-loss" -> "Weight"
    "improve-appetite" -> "Appetite"
    "improve-mood" -> "Mood"
    "reduce-stress" -> "Stress"
    "improve-fitness" -> "Fitness"
    "improve-recovery" -> "Recovery"
    "reduce-fatigue" -> "Energy"
    else -> slug.replaceFirstChar { it.uppercase() }
}
