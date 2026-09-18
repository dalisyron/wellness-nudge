package com.mimik.wellnessnudge.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * The AI call to action: a 60 dp pill filled with the Daybreak gradient over a soft iris
 * glow. Disabled, it turns quiet (no gradient, no glow). [loading] swaps the icon for a
 * spinner and ignores taps.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Rounded.AutoAwesome,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = WellnessTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val scale by pressScale(interactionSource)
    val content = if (enabled) Color.White else colors.textTertiary
    Box(
        modifier = modifier
            .height(60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glow(if (enabled) colors.glow else Color.Transparent, WellnessShapes.Pill, blurRadius = 24.dp, offsetY = 10.dp)
            .clip(WellnessShapes.Pill)
            .then(
                if (enabled) {
                    Modifier.background(colors.daybreak)
                } else {
                    Modifier
                        .background(colors.surfaceRaised)
                        .border(1.dp, colors.hairline, WellnessShapes.Pill)
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonContent(
            text = text,
            icon = icon,
            color = content,
            loading = loading,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
        )
    }
}

/** Quiet 52 dp pill for everything that isn't the AI action. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = WellnessTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val scale by pressScale(interactionSource)
    Box(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(WellnessShapes.Pill)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.hairline, WellnessShapes.Pill)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonContent(
            text = text,
            icon = icon,
            color = if (enabled) colors.textPrimary else colors.textTertiary,
            loading = false,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** 44 dp round button for icon-only actions such as back and delete. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = WellnessTheme.colors.textPrimary,
) {
    val colors = WellnessTheme.colors
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.hairline, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else colors.textTertiary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Inline accent action, such as "Sample day" in a section header. 36 dp tall; its 8 dp
 * side padding lets [SectionHeader] line the label up with the content edge.
 */
@Composable
fun TextAction(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = WellnessTheme.colors.accent
    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(WellnessShapes.Pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = TextActionPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = accent, maxLines = 1)
    }
}

internal val TextActionPadding = 8.dp

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    color: Color,
    loading: Boolean,
    style: TextStyle,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (loading) {
            Spinner(color = color)
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        if (loading || icon != null) Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 0.98 while pressed, springing back on release. */
@Composable
private fun pressScale(interactionSource: MutableInteractionSource) = animateFloatAsState(
    targetValue = if (interactionSource.collectIsPressedAsState().value) 0.98f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "pressScale",
)
