package com.mimik.wellnessnudge.ui.runtime

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.bootstrap.Models
import com.mimik.wellnessnudge.bootstrap.RuntimeInfo
import com.mimik.wellnessnudge.ui.components.IconBadge
import com.mimik.wellnessnudge.ui.components.SkeletonBlock
import com.mimik.wellnessnudge.ui.components.StatusDot
import com.mimik.wellnessnudge.ui.components.TextAction
import com.mimik.wellnessnudge.ui.components.TextActionPadding
import com.mimik.wellnessnudge.ui.format.formatBytes
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * What the runtime sheet shows: the mimOE runtime, the wellness-nudge mim, AI inference, each
 * model and the stored nudges, every row with its live status, or skeletons while loading.
 * Place it in a [com.mimik.wellnessnudge.ui.components.WellnessBottomSheet].
 *
 * @param onRetry reads the runtime again after it couldn't be reached.
 */
@Composable
fun RuntimeSheetContent(
    state: RuntimeSheetUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(bottom = 20.dp),
    ) {
        Text(
            text = "Running on this phone",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Everything below happens locally. No cloud round-trips.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(16.dp))
        Crossfade(state, animationSpec = tween(WellnessMotion.SmallMillis), label = "runtimeRows") { current ->
            when (current) {
                RuntimeSheetUiState.Loading -> Rows(List(SkeletonRows) { null })
                is RuntimeSheetUiState.Loaded -> Rows(current.info.toRows())
                RuntimeSheetUiState.Unavailable -> Unavailable(onRetry)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Powered by mimik mimOE",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/** One part of the stack as a row: [status] is what the trailing dot or check says. */
@Immutable
internal data class RuntimeRow(
    val icon: ImageVector,
    val title: String,
    val detail: String,
    val status: RowStatus,
)

internal enum class RowStatus(val description: String) {
    /** A service answering on the phone: a live dot. */
    Running("Running"),

    /** Something stored on the phone: a check. */
    Ready("Ready"),

    /** A service that didn't answer, or a model that isn't there. */
    Unavailable("Unavailable"),
}

internal fun RuntimeInfo.toRows(): List<RuntimeRow> = buildList {
    add(RuntimeRow(Icons.Rounded.Memory, "mimOE runtime", "Embedded · port $port", RowStatus.Running))
    val mimState = when {
        mimHealthy -> "healthy"
        mimHealth != null -> "unhealthy"
        else -> "not responding"
    }
    add(
        RuntimeRow(
            Icons.Rounded.Hub,
            "wellness-nudge mim",
            "$mimApiRoot · $mimState",
            if (mimHealthy) RowStatus.Running else RowStatus.Unavailable,
        ),
    )
    // mILM lists the models it can serve, so a ready model means inference answers.
    add(
        RuntimeRow(
            Icons.Rounded.AutoAwesome,
            "AI inference",
            "mILM · OpenAI-compatible API",
            if (models.any { it.ready }) RowStatus.Running else RowStatus.Unavailable,
        ),
    )
    models.forEach { model ->
        add(
            RuntimeRow(
                icon = if (model.id == Models.QWEN3.download.id) Icons.Rounded.Category else Icons.Rounded.EditNote,
                title = model.displayName,
                detail = "${model.id} · ${if (model.ready) formatBytes(model.sizeBytes) else "not downloaded"}",
                status = if (model.ready) RowStatus.Ready else RowStatus.Unavailable,
            ),
        )
    }
    val stored = when (nudgeCount) {
        null -> "Kept in on-device storage"
        0 -> "No nudges stored yet"
        1 -> "1 nudge in on-device storage"
        else -> "$nudgeCount nudges in on-device storage"
    }
    add(RuntimeRow(Icons.Rounded.Lock, "Your data", stored, RowStatus.Ready))
}

/** Rows between inset hairlines; a null row is a loading placeholder of the same height. */
@Composable
private fun Rows(rows: List<RuntimeRow?>) {
    Column {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = BadgeSize + BadgeGap),
                    thickness = 1.dp,
                    color = WellnessTheme.colors.hairline,
                )
            }
            if (row == null) SkeletonRow() else RuntimeRowItem(row)
        }
    }
}

@Composable
private fun RuntimeRowItem(row: RuntimeRow) {
    val colors = WellnessTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight)
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) { stateDescription = row.status.description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(row.icon, colors.accent, size = BadgeSize)
        Spacer(Modifier.width(BadgeGap))
        Column(Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(row.detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when (row.status) {
                RowStatus.Running -> StatusDot(colors.success, pulsing = true)
                RowStatus.Ready -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(20.dp),
                )
                RowStatus.Unavailable -> StatusDot(colors.danger)
            }
        }
    }
}

@Composable
private fun SkeletonRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(Modifier.size(BadgeSize), CircleShape)
        Spacer(Modifier.width(BadgeGap))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBlock(
                Modifier
                    .fillMaxWidth(0.42f)
                    .height(12.dp),
            )
            SkeletonBlock(
                Modifier
                    .fillMaxWidth(0.66f)
                    .height(10.dp),
            )
        }
    }
}

@Composable
private fun Unavailable(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Couldn't read the runtime just now.",
            style = MaterialTheme.typography.bodyMedium,
            color = WellnessTheme.colors.textSecondary,
        )
        // Its label lines up with the message above.
        TextAction(
            text = "Try again",
            icon = Icons.Rounded.Refresh,
            onClick = onRetry,
            modifier = Modifier.offset(x = -TextActionPadding),
        )
    }
}

private val BadgeSize = 36.dp
private val BadgeGap = 14.dp
private val RowHeight = 56.dp

/** Runtime, mim, inference, two models and the data row. */
private const val SkeletonRows = 6
