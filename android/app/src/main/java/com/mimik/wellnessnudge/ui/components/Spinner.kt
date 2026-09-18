package com.mimik.wellnessnudge.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * Small indeterminate spinner for buttons and checklist rows. Previews and snapshot tests
 * show a still arc instead of the first frame's dot.
 */
@Composable
fun Spinner(
    modifier: Modifier = Modifier,
    color: Color = WellnessTheme.colors.accent,
    size: Dp = 18.dp,
    strokeWidth: Dp = 2.dp,
) {
    if (LocalInspectionMode.current) {
        CircularProgressIndicator(
            progress = { 0.7f },
            modifier = modifier.size(size),
            color = color,
            strokeWidth = strokeWidth,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
        )
    } else {
        CircularProgressIndicator(
            modifier = modifier.size(size),
            color = color,
            strokeWidth = strokeWidth,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
        )
    }
}
