package com.mimik.wellnessnudge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import java.util.Locale

/**
 * The standard surface: 28 dp corners with a hairline border, plus a soft shadow in the
 * light theme. Clickable cards ripple within their shape. [border] replaces the hairline
 * (the nudge card uses a Daybreak gradient stroke) and [glow] washes a corner with color.
 */
@Composable
fun WellnessCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = WellnessSpacing.CardPadding,
    shape: Shape = WellnessShapes.Card,
    border: BorderStroke? = null,
    glow: Color = Color.Unspecified,
    glowAlignment: Alignment = Alignment.TopEnd,
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WellnessTheme.colors
    Column(
        modifier = modifier
            .then(
                if (colors.isDark) {
                    Modifier
                } else {
                    Modifier.shadow(10.dp, shape, clip = false, ambientColor = colors.shadowAmbient, spotColor = colors.shadowSpot)
                },
            )
            .clip(shape)
            .background(colors.surface)
            .radialGlow(glow, glowAlignment)
            .border(border ?: BorderStroke(1.dp, colors.hairline), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

/**
 * An editable metric: tinted icon, label and edit glyph, then the value and unit on a
 * shared baseline, and an optional meter with a trailing caption such as "of 10k".
 */
@Composable
fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String?,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    meterProgress: Float? = null,
    meterCaption: String? = null,
) {
    val colors = WellnessTheme.colors
    WellnessCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = WellnessSpacing.TilePadding,
        shape = WellnessShapes.Tile,
        onClickLabel = "Edit ${label.lowercase(Locale.US)}",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color)
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (onClick != null) {
                Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Text(
                text = value,
                modifier = Modifier.alignByBaseline(),
                style = WellnessTheme.type.metricL,
                color = colors.textPrimary,
                maxLines = 1,
            )
            if (unit != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    modifier = Modifier.alignByBaseline(),
                    style = WellnessTheme.type.metricUnit,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
        if (meterProgress != null) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearMeter(meterProgress, color, Modifier.weight(1f))
                if (meterCaption != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(text = meterCaption, style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                }
            }
        }
    }
}
