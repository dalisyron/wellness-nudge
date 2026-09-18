package com.mimik.wellnessnudge.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.ScreenTitle
import com.mimik.wellnessnudge.ui.components.SkeletonBlock
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing

/**
 * Today tab. PLACEHOLDER until the Today screen (spec 4.2) replaces it.
 *
 * @param onGenerate starts a generation and opens `nudge/new`.
 * @param onOpenRuntime opens the runtime sheet (from the On-device pill).
 * @param contentPadding space taken by the floating tab bar; keep content above it.
 */
@Composable
fun TodayRoute(
    repository: NudgeRepository,
    onGenerate: (NudgeRequest) -> Unit,
    onOpenRuntime: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    DaybreakBackground(modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(contentPadding)
                .padding(horizontal = WellnessSpacing.ScreenMargin, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(WellnessSpacing.ItemGap),
        ) {
            ScreenTitle(eyebrow = "Placeholder", title = "TodayRoute", subtitle = "The Today screen replaces this.")
            repeat(5) {
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth()
                        .height(128.dp),
                    WellnessShapes.Card,
                )
            }
        }
    }
}
