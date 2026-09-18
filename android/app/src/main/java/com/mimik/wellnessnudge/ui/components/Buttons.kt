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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimik.wellnessnudge.ui.theme.Daybreak
import com.mimik.wellnessnudge.ui.theme.WellnessColors
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * The AI call to action: a 60 dp pill filled with the Daybreak gradient, set in ink (the
 * button is the light source; ink keeps 4.7:1 or more across the whole gradient) over a
 * glow in the gradient's own colors. [loading] swaps the icon for a spinner and ignores
 * taps. Disabled and loading together ("Setting up…") it reads as work in progress: a
 * quiet pill with a faint Daybreak rim. Disabled alone it turns fully quiet.
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
    val content = when {
        enabled -> OnDaybreak
        loading -> colors.textSecondary
        else -> colors.textDisabled
    }
    Box(
        modifier = modifier
            .heightIn(min = 60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (enabled) {
                    Modifier.glow(
                        brush = colors.daybreak,
                        shape = WellnessShapes.Pill,
                        alpha = colors.glowAlpha,
                        blurRadius = 24.dp,
                        offsetY = 10.dp,
                        spread = (-4).dp,
                    )
                } else {
                    Modifier
                },
            )
            .clip(WellnessShapes.Pill)
            .then(
                when {
                    enabled -> Modifier.background(colors.daybreak)
                    loading -> Modifier
                        .background(colors.controlFill)
                        .border(1.5.dp, PendingRim, WellnessShapes.Pill)
                    else -> Modifier
                        .background(colors.controlFill)
                        .border(1.dp, colors.controlBorder, WellnessShapes.Pill)
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

/**
 * Quiet 52 dp pill for everything that isn't the AI action. On ink it is a raised pill; on
 * paper it is a white pill with a fine border and a soft shadow, like the cards.
 */
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
            .heightIn(min = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .control(colors, WellnessShapes.Pill)
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
            color = if (enabled) colors.textPrimary else colors.textDisabled,
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
            .control(colors, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else colors.textDisabled,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Inline accent action, such as "Sample day" in a section header. The visible pill is 36 dp
 * tall inside a 48 dp touch target, and its 8 dp side padding lets [SectionHeader] line the
 * label up with the content edge.
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
            .minimumInteractiveComponentSize()
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
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal val TextActionPadding = 8.dp

/**
 * The shared look of pill and circle controls: [WellnessColors.controlFill] inside a
 * [WellnessColors.controlBorder], plus a soft paper shadow in the light theme.
 */
internal fun Modifier.control(colors: WellnessColors, shape: Shape): Modifier = this
    .paperShadow(colors, shape, elevation = 3.dp)
    .clip(shape)
    .background(colors.controlFill)
    .border(1.dp, colors.controlBorder, shape)

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

/** Label and icon color on the Daybreak gradient, in both themes. */
private val OnDaybreak = Color(0xFF16151D)

/** The Daybreak rim of a CTA that is setting up: the gradient at 40%. */
private val PendingRim = Brush.horizontalGradient(
    listOf(Daybreak.Iris, Daybreak.Orchid, Daybreak.Coral).map { it.copy(alpha = 0.4f) },
)
