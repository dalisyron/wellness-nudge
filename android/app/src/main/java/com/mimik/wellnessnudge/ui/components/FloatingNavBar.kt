package com.mimik.wellnessnudge.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * One destination in the [FloatingNavBar]: [icon] while unselected (an outlined glyph, so
 * the idle tabs stay light) and [selectedIcon] while selected (the filled glyph).
 */
@Immutable
data class NavBarItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon)

object FloatingNavBarDefaults {
    val Height = 64.dp
    val BottomMargin = 12.dp

    /** How much of the screen bottom the bar covers, navigation-bar inset included. Pad scrolling content by it. */
    @Composable
    fun occupiedHeight(): Dp =
        Height + BottomMargin + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

/**
 * The detached tab bar floating above the navigation-bar inset. The selection capsule
 * glides between tabs; it applies its own insets and margins.
 */
@Composable
fun FloatingNavBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    val indicatorColor = colors.tint(colors.accent)
    val indicator by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f),
        label = "navIndicator",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = FloatingNavBarDefaults.BottomMargin)
            .height(FloatingNavBarDefaults.Height)
            .paperShadow(colors, WellnessShapes.Pill, elevation = 14.dp)
            .clip(WellnessShapes.Pill)
            .background(if (colors.isDark) colors.surface.copy(alpha = 0.92f) else colors.surface)
            .border(1.dp, if (colors.isDark) colors.hairlineStrong else colors.hairline, WellnessShapes.Pill)
            .drawBehind {
                val inset = IndicatorInset.toPx()
                val cell = (size.width - inset * 2) / items.size
                val height = size.height - inset * 2
                val start = inset + cell * indicator
                drawRoundRect(
                    color = indicatorColor,
                    topLeft = Offset(if (layoutDirection == LayoutDirection.Rtl) size.width - start - cell else start, inset),
                    size = Size(cell, height),
                    cornerRadius = CornerRadius(height / 2f),
                )
            }
            .padding(IndicatorInset)
            .selectableGroup(),
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val tint by animateColorAsState(
                targetValue = if (selected) colors.accentContent else colors.textSecondary,
                animationSpec = tween(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing),
                label = "navTint",
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(WellnessShapes.Pill)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Crossfade(
                    targetState = if (selected) item.selectedIcon else item.icon,
                    animationSpec = tween(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing),
                    label = "navIcon",
                ) { icon ->
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(text = item.label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
            }
        }
    }
}

private val IndicatorInset = 6.dp
