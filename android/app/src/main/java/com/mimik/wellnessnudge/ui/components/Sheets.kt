package com.mimik.wellnessnudge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessColors
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * The Daybreak bottom sheet, for the sleep and metric editors and the runtime sheet:
 * Material's ModalBottomSheet on the surface color with 32 dp top corners (Material's
 * default would be 28) and a quiet drag handle. On ink, where there are no shadows, a
 * hairline traces its top edge against the dimmed screen; on paper the scrim is lighter than
 * Material's. The sheet already pads its content for the navigation bar and the keyboard;
 * pad the sides with `WellnessSpacing.ScreenMargin`. To close it from a button with the
 * slide-down animation, hide [sheetState] first, then call your dismiss handler (Material's
 * usual pattern; [SheetState] needs ExperimentalMaterial3Api). Previews and snapshot tests
 * can't show its dialog window: use [WellnessBottomSheetFrame].
 */
@ExperimentalMaterial3Api
@Composable
fun WellnessBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WellnessTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.topEdgeHairline(colors),
        sheetState = sheetState,
        shape = WellnessShapes.SheetTop,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        scrimColor = sheetScrim(colors),
        dragHandle = { SheetDragHandle() },
    ) {
        val scope = this
        CompositionLocalProvider(LocalOnSurface provides true) { scope.content() }
    }
}

/**
 * [WellnessBottomSheet] as it looks when open, drawn in place: the scrim over the screen
 * and the sheet anchored to the bottom with the same corners, color, edge, handle and inset.
 * For previews and snapshot tests of sheet content, since they can't show dialog windows.
 */
@Composable
fun WellnessBottomSheetFrame(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = WellnessTheme.colors
    Box(
        modifier
            .fillMaxSize()
            .background(sheetScrim(colors)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.textPrimary, LocalOnSurface provides true) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .topEdgeHairline(colors)
                    .clip(WellnessShapes.SheetTop)
                    .background(colors.surface)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            ) {
                SheetDragHandle()
                content()
            }
        }
    }
}

/** A 36 × 4 dp pill centered at the top of a sheet. The sheet adds its expand and dismiss actions. */
@Composable
private fun SheetDragHandle() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 16.dp)
            .semantics { contentDescription = "Drag handle" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(WellnessTheme.colors.textDisabled.copy(alpha = 0.5f), WellnessShapes.Pill),
        )
    }
}

/** Material's modal dim on ink; lighter on paper, where it would turn the page muddy. */
@Composable
private fun sheetScrim(colors: WellnessColors): Color =
    MaterialTheme.colorScheme.scrim.copy(alpha = if (colors.isDark) 0.32f else 0.24f)

/**
 * On ink, a hairlineStrong line along the sheet's top edge and its rounded corners, inside
 * the sheet, as other floating surfaces have. Nothing on paper, where the sheet sits on a scrim.
 */
private fun Modifier.topEdgeHairline(colors: WellnessColors): Modifier {
    if (!colors.isDark) return this
    return drawWithCache {
        val stroke = 1.dp.toPx()
        val inset = stroke / 2f
        val radius = SheetCorner.toPx() - inset
        val path = Path().apply {
            moveTo(inset, SheetCorner.toPx())
            arcTo(Rect(inset, inset, inset + radius * 2, inset + radius * 2), 180f, 90f, forceMoveTo = false)
            lineTo(size.width - inset - radius, inset)
            arcTo(Rect(size.width - inset - radius * 2, inset, size.width - inset, inset + radius * 2), 270f, 90f, forceMoveTo = false)
        }
        onDrawWithContent {
            drawContent()
            drawPath(path, colors.hairlineStrong, style = Stroke(stroke))
        }
    }
}

// WellnessShapes.SheetTop's corner radius.
private val SheetCorner = 32.dp
