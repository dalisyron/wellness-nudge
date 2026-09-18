package com.mimik.wellnessnudge.ui.journal

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
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.ScreenTitle
import com.mimik.wellnessnudge.ui.components.SkeletonBlock
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing

/**
 * Journal tab. PLACEHOLDER until the Journal screen (spec 4.4) replaces it.
 *
 * @param onOpenNudge opens a saved nudge (`nudge/{id}`).
 * @param onCreateNudge switches to the Today tab (empty-state action).
 * @param contentPadding space taken by the floating tab bar; keep content above it.
 */
@Composable
fun JournalRoute(
    repository: NudgeRepository,
    onOpenNudge: (id: String) -> Unit,
    onCreateNudge: () -> Unit,
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
            ScreenTitle(eyebrow = "Placeholder", title = "JournalRoute", subtitle = "The Journal screen replaces this.")
            repeat(6) {
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth()
                        .height(104.dp),
                    WellnessShapes.Tile,
                )
            }
        }
    }
}
