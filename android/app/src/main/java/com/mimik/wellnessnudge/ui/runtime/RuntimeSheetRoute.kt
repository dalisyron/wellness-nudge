package com.mimik.wellnessnudge.ui.runtime

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.bootstrap.RuntimeInfo
import com.mimik.wellnessnudge.ui.components.ScreenTitle
import com.mimik.wellnessnudge.ui.components.WellnessBottomSheet
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing

/**
 * What runs on the phone, opened from the On-device pill. PLACEHOLDER until the runtime
 * sheet (spec 4.6) replaces it.
 *
 * @param loadRuntimeInfo fetches port, mim health, models and nudge count (BootstrapViewModel).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeSheetRoute(
    loadRuntimeInfo: suspend () -> RuntimeInfo,
    onDismiss: () -> Unit,
) {
    WellnessBottomSheet(onDismissRequest = onDismiss) {
        ScreenTitle(
            eyebrow = "Placeholder",
            title = "RuntimeSheetRoute",
            subtitle = "The runtime sheet replaces this.",
            modifier = Modifier.padding(start = WellnessSpacing.ScreenMargin, end = WellnessSpacing.ScreenMargin, bottom = 24.dp),
        )
    }
}
