package com.mimik.wellnessnudge.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import java.util.Locale

/** Small uppercase tracked label (labelSmall) for section headers, dates and meta lines. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = WellnessTheme.colors.textTertiary,
) {
    Text(
        text = text.uppercase(Locale.US),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Section eyebrow with an optional trailing [action] (usually a [TextAction]). The header
 * is only as tall as its label, so the 12 dp gap to the content stays exact; the action is
 * centered on the label and overhangs it, keeping its full touch target, with its label
 * lined up with the content edge.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Eyebrow(title, color = WellnessTheme.colors.textSecondary)
            if (action != null) Box { action() }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val overhang = TextActionPadding.roundToPx()
        val actionPlaceable = measurables.getOrNull(1)?.measure(Constraints(maxWidth = width))
        val reserved = actionPlaceable?.let { it.width - overhang + 12.dp.roundToPx() } ?: 0
        val titlePlaceable = measurables[0].measure(Constraints(maxWidth = (width - reserved).coerceAtLeast(0)))
        val height = maxOf(titlePlaceable.height, constraints.minHeight)
        layout(width, height) {
            titlePlaceable.placeRelative(0, (height - titlePlaceable.height) / 2)
            actionPlaceable?.placeRelative(
                x = width - actionPlaceable.width + overhang,
                y = (height - actionPlaceable.height) / 2,
            )
        }
    }
}

/** Title block for the tab screens: eyebrow, serif title and a secondary subtitle. */
@Composable
fun ScreenTitle(
    eyebrow: String? = null,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    Column(modifier) {
        if (eyebrow != null) {
            Eyebrow(eyebrow)
            Spacer(Modifier.height(10.dp))
        }
        Text(text = title, style = MaterialTheme.typography.displayMedium, color = colors.textPrimary)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}
