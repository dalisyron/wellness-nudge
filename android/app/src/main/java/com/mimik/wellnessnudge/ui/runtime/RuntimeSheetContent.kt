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
import androidx.compose.material.icons.rounded.ErrorOutline
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.bootstrap.Models
import com.mimik.wellnessnudge.bootstrap.RuntimeInfo
import com.mimik.wellnessnudge.ui.components.IconBadge
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.components.SkeletonBlock
import com.mimik.wellnessnudge.ui.components.StatusDot
import com.mimik.wellnessnudge.ui.format.formatBytes
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * What the runtime sheet shows: the mimOE runtime, the wellness-nudge mim, AI inference, each
 * model and the stored nudges, every row with its live status, or skeletons while loading.
 * When parts don't answer, a line sums up what that means and a button checks again; when
 * the runtime can't be read at all, the sheet says so in place of the rows, at their height.
 * Place it in a [com.mimik.wellnessnudge.ui.components.WellnessBottomSheet].
 *
 * @param onRetry reads the runtime again.
 */
@Composable
fun RuntimeSheetContent(
    state: RuntimeSheetUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    val rows = (state as? RuntimeSheetUiState.Loaded)?.info?.toRows()
    val broken = rows.orEmpty().filter { it.status == RowStatus.Unavailable }
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
        // Nothing below to vouch for when the runtime can't be read.
        if (state !is RuntimeSheetUiState.Unavailable) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Everything below happens locally. No cloud round-trips.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        if (broken.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = brokenSummary(broken),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.dangerText,
            )
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
        Crossfade(state, animationSpec = tween(WellnessMotion.SmallMillis), label = "runtimeRows") { current ->
            when (current) {
                RuntimeSheetUiState.Loading -> Rows(List(SkeletonRows) { null })
                is RuntimeSheetUiState.Loaded -> Rows(current.info.toRows())
                RuntimeSheetUiState.Unavailable -> Unavailable(onRetry)
            }
        }
        if (broken.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            SecondaryButton(text = "Check again", onClick = onRetry, modifier = Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh)
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

/**
 * One part of the stack as a row: [status] is what the trailing mark says, and [problem]
 * what is wrong with it, if anything ("Not responding"). [blocksNudges]: new nudges need it.
 */
@Immutable
internal data class RuntimeRow(
    val icon: ImageVector,
    val title: String,
    val detail: String,
    val status: RowStatus,
    val problem: String? = null,
    val blocksNudges: Boolean = false,
)

internal enum class RowStatus(val description: String?) {
    /** A service answering on the phone: a live dot. */
    Running("Running"),

    /** Something stored on the phone: a check. */
    Ready("Ready"),

    /** A service that didn't answer, or a model that isn't there: an error mark. */
    Unavailable("Unavailable"),

    /** Nothing to report, e.g. the stored nudges: no mark. */
    None(null),
}

internal fun RuntimeInfo.toRows(): List<RuntimeRow> = buildList {
    add(
        RuntimeRow(
            Icons.Rounded.Memory,
            "mimOE runtime",
            if (runtimeReady) "Embedded · port $port" else "Embedded",
            if (runtimeReady) RowStatus.Running else RowStatus.Unavailable,
            problem = if (runtimeReady) null else "Not running",
            blocksNudges = true,
        ),
    )
    add(
        RuntimeRow(
            Icons.Rounded.Hub,
            "wellness-nudge mim",
            if (mimHealthy) "$mimApiRoot · healthy" else mimApiRoot,
            if (mimHealthy) RowStatus.Running else RowStatus.Unavailable,
            problem = when {
                mimHealthy -> null
                mimHealth != null -> "Unhealthy"
                else -> "Not responding"
            },
            blocksNudges = true,
        ),
    )
    // mILM lists the models it can serve, so a ready model means inference answers.
    val serving = models.any { it.ready }
    add(
        RuntimeRow(
            Icons.Rounded.AutoAwesome,
            "AI inference",
            "mILM · OpenAI-compatible API",
            if (serving) RowStatus.Running else RowStatus.Unavailable,
            problem = if (serving) null else "No model ready",
            blocksNudges = true,
        ),
    )
    models.forEach { model ->
        val classifier = model.id == Models.QWEN3.download.id
        add(
            RuntimeRow(
                icon = if (classifier) Icons.Rounded.Category else Icons.Rounded.EditNote,
                title = model.displayName,
                detail = if (model.ready) "${model.id} · ${formatBytes(model.sizeBytes)}" else model.id,
                status = if (model.ready) RowStatus.Ready else RowStatus.Unavailable,
                problem = if (model.ready) null else "Not downloaded",
                // Nudges are written by the nudge writer alone.
                blocksNudges = !classifier,
            ),
        )
    }
    val stored = when (nudgeCount) {
        null -> "Kept in on-device storage"
        0 -> "No nudges stored yet"
        1 -> "1 nudge in on-device storage"
        else -> "$nudgeCount nudges in on-device storage"
    }
    add(RuntimeRow(Icons.Rounded.Lock, "Your data", stored, RowStatus.None))
}

/** "1 part isn’t working. New nudges may fail until it’s back." for the [broken] rows. */
internal fun brokenSummary(broken: List<RuntimeRow>): String = buildString {
    val one = broken.size == 1
    append(if (one) "1 part isn’t working." else "${broken.size} parts aren’t working.")
    if (broken.any { it.blocksNudges }) {
        append(if (one) " New nudges may fail until it’s back." else " New nudges may fail until they’re back.")
    }
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
            .semantics(mergeDescendants = true) { row.status.description?.let { stateDescription = it } },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(row.icon, colors.accent, size = BadgeSize)
        Spacer(Modifier.width(BadgeGap))
        Column(Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(
                text = buildAnnotatedString {
                    append(row.detail)
                    row.problem?.let { problem ->
                        append(" · ")
                        withStyle(SpanStyle(color = colors.dangerText)) { append(problem) }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        if (row.status != RowStatus.None) {
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
                    // A shape of its own, so it doesn't rest on red against green.
                    RowStatus.Unavailable -> Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(20.dp),
                    )
                    RowStatus.None -> Unit
                }
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

/**
 * The runtime couldn't be read at all: the app's error pattern in place of the rows, as tall
 * as they would be, so the sheet doesn't collapse.
 */
@Composable
private fun Unavailable(onRetry: () -> Unit) {
    val colors = WellnessTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = RowsHeight),
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge(Icons.Rounded.ErrorOutline, colors.warning, size = 40.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Couldn’t reach the on-device runtime",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "It may still be starting. Give it a moment and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(20.dp))
        SecondaryButton(text = "Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh)
    }
}

private val BadgeSize = 36.dp
private val BadgeGap = 14.dp
private val RowHeight = 56.dp

/** Runtime, mim, inference, two models and the data row. */
private const val SkeletonRows = 6

/** The rows' height: six rows and the hairlines between them. */
private val RowsHeight = RowHeight * SkeletonRows + 1.dp * (SkeletonRows - 1)
