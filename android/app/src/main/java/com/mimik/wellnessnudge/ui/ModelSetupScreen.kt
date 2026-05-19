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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.bootstrap.ModelSetupItem
import com.mimik.wellnessnudge.ui.components.PrimaryActionButton
import com.mimik.wellnessnudge.ui.components.SectionLabel

/**
 * The model-download gate. Shown the first time the user opens the app (or
 * any time models aren't cached). The bottom-nav main app cannot be reached
 * until every model is Ready and the user taps Continue.
 */
@Composable
fun ModelSetupScreen(
    state: BootstrapState.Setup,
    onContinue: () -> Unit,
    onRetry: (modelId: String) -> Unit,
) {
    val allReady = state.allReady
    val anyFailed = state.anyFailed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = 120.dp, // room for the floating CTA
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Header() }
            item { PrivacyCallout() }
            item { Spacer(Modifier.height(4.dp)) }
            item { SectionLabel(text = "AI models") }
            items(state.items, key = { it.id }) { item ->
                ModelCard(item = item, onRetry = { onRetry(item.id) })
            }
            item { Spacer(Modifier.height(8.dp)) }
            item { FootNote(allReady = allReady, anyFailed = anyFailed) }
        }

        // Floating CTA pinned to the bottom of the page.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            PrimaryActionButton(
                text = continueButtonText(state),
                onClick = onContinue,
                enabled = allReady,
                icon = if (allReady) Icons.Filled.Check else null,
            )
        }
    }
}

private fun continueButtonText(state: BootstrapState.Setup): String = when {
    state.allReady -> "Continue to Wellness Nudge"
    state.anyFailed && !state.anyInFlight -> "Set up paused"
    else -> "Setting up…"
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Small leaf in a tinted bubble — matches the bootstrap loading
        // screen so the brand feels continuous.
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "One-time setup",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Wellness Nudge runs entirely on this phone. We just need to download two small AI models — once. After that, the app works fully offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrivacyCallout() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Models are pulled from Hugging Face, then live on your device. " +
                "Your nudges, metrics, and history never leave your phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ModelCard(item: ModelSetupItem, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${item.technicalName} · ${approxSizeLabel(item.approxBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(state = item.state)
        }

        Text(
            text = item.purpose,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (item.state) {
            ModelSetupItem.State.Pending -> {
                // Static empty bar — no animation while waiting, so the
                // active downloading card is the only thing in motion.
                ProgressBar(progress = 0f, complete = false)
                StatusLine("Waiting in queue")
            }
            ModelSetupItem.State.Downloading -> {
                val fraction = if (item.totalBytes > 0) {
                    (item.downloadedBytes.toFloat() / item.totalBytes).coerceIn(0f, 1f)
                } else null
                ProgressBar(progress = fraction, complete = false)
                val text = if (item.totalBytes > 0) {
                    val mbDone = item.downloadedBytes / 1_048_576
                    val mbTotal = item.totalBytes / 1_048_576
                    val pct = ((fraction ?: 0f) * 100).toInt()
                    "$mbDone / $mbTotal MB · $pct%"
                } else {
                    "Starting download…"
                }
                StatusLine(text)
            }
            ModelSetupItem.State.Ready -> {
                ProgressBar(progress = 1f, complete = true)
                StatusLine("Ready — ${approxSizeLabel(item.totalBytes.takeIf { it > 0 } ?: item.approxBytes)} on disk")
            }
            ModelSetupItem.State.Failed -> {
                ProgressBar(progress = 0f, complete = false, failed = true)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.errorMessage ?: "Couldn't download.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    TextButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float?, complete: Boolean, failed: Boolean = false) {
    val height = 6.dp
    val color = when {
        failed -> MaterialTheme.colorScheme.error
        complete -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
    val track = MaterialTheme.colorScheme.outlineVariant
    if (progress == null) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(8.dp)),
            color = color,
            trackColor = track,
        )
    } else {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(8.dp)),
            color = color,
            trackColor = track,
            // Hide the trailing indeterminate stop so the bar reads as a
            // pure fraction.
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun StatusLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatusPill(state: ModelSetupItem.State) {
    val (label, icon, bg, fg) = when (state) {
        ModelSetupItem.State.Pending -> StatusPillSpec("Queued", null,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant)
        ModelSetupItem.State.Downloading -> StatusPillSpec("Downloading", Icons.Outlined.Download,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer)
        ModelSetupItem.State.Ready -> StatusPillSpec("Ready", Icons.Filled.Check,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary)
        ModelSetupItem.State.Failed -> StatusPillSpec("Failed", Icons.Outlined.ErrorOutline,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer)
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
    }
}

private data class StatusPillSpec(
    val label: String,
    val icon: ImageVector?,
    val bg: Color,
    val fg: Color,
)

@Composable
private fun FootNote(allReady: Boolean, anyFailed: Boolean) {
    val (icon, text) = when {
        allReady -> Icons.Filled.Check to
            "All set. Tap Continue to enter the app."
        anyFailed -> Icons.Outlined.CloudOff to
            "One or more downloads failed. Connect to Wi-Fi and tap Retry on the affected model."
        else -> Icons.Outlined.Download to
            "Keep the app open while downloading. First-time setup takes a few minutes on Wi-Fi."
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun approxSizeLabel(bytes: Long): String {
    val mb = bytes / 1_048_576
    return if (mb >= 1024) {
        val gb = mb / 1024.0
        if (gb >= 10) "${gb.toInt()} GB" else "%.1f GB".format(gb)
    } else {
        "$mb MB"
    }
}
