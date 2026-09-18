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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimik.wellnessnudge.ui.format.formatSleepAnnotated
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

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
            .paperShadow(colors, shape, elevation = 10.dp)
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
    ) {
        val scope = this
        CompositionLocalProvider(LocalOnSurface provides true) { scope.content() }
    }
}

/**
 * An editable metric: tinted icon, label and edit glyph, then the value and unit on a
 * shared baseline, and an optional meter with a trailing caption such as "of 10k". Tiles
 * use the 24 dp tile corners; pass [shape] = `WellnessShapes.Card` for a tile that spans
 * the full content width, so it matches the full-width cards around it.
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
    shape: Shape = WellnessShapes.Tile,
) {
    val colors = WellnessTheme.colors
    WellnessCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = WellnessSpacing.TilePadding,
        shape = shape,
        onClickLabel = "Edit $label",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color, size = TileBadgeSize)
            Spacer(Modifier.width(TileBadgeGap))
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (onClick != null) {
                Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(TileEditGlyphSize))
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
            // Exactly as tall as the meter, so every tile keeps the same rhythm; the caption
            // is centered on the meter and overflows the row instead of growing it.
            Row(Modifier.height(MeterHeight), verticalAlignment = Alignment.CenterVertically) {
                LinearMeter(meterProgress, color, Modifier.weight(1f), height = MeterHeight)
                if (meterCaption != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = meterCaption,
                        modifier = Modifier.wrapContentHeight(unbounded = true),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * The room a [MetricTile]'s label has on its line in a tile [width] wide: the tile less its
 * padding, its badge and, on an editable tile, the edit glyph. A longer label is truncated.
 */
internal fun metricTileLabelWidth(width: Dp, editable: Boolean = true): Dp =
    width - WellnessSpacing.TilePadding * 2 - TileBadgeSize - TileBadgeGap - if (editable) TileEditGlyphSize else 0.dp

/**
 * A sleep duration as the hero value, e.g. on the Today sleep card and in the sleep editor:
 * digits in `metricXL`, with smaller, quieter "h" and "m" on the same baseline, so the
 * number leads. Align a trailing "asleep" (`metricUnit`) with `Modifier.alignByBaseline()`.
 */
@Composable
fun SleepDuration(
    hours: Float,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    Text(
        text = formatSleepAnnotated(hours, SpanStyle(fontSize = 24.sp, letterSpacing = 0.sp, color = colors.textSecondary)),
        modifier = modifier,
        style = WellnessTheme.type.metricXL,
        color = colors.textPrimary,
        maxLines = 1,
    )
}

private val MeterHeight = 6.dp
private val TileBadgeSize = 32.dp
private val TileBadgeGap = 10.dp
private val TileEditGlyphSize = 14.dp
