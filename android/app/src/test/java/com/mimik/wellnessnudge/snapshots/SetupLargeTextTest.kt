package com.mimik.wellnessnudge.snapshots

import com.mimik.wellnessnudge.ui.preview.PreviewData
import com.mimik.wellnessnudge.ui.runtime.RuntimeSheetUiState
import org.junit.Rule
import org.junit.Test

/**
 * Setup and the runtime sheet at 150% font size: text wraps and rows grow, nothing clips.
 * The frame is taller than the phone so a page's overflow shows instead of being cut off.
 */
class SetupLargeTextTest {

    @get:Rule
    val paparazzi = wellnessPaparazzi(Pixel9ProXL.copy(fontScale = 1.5f, screenHeight = 3300))

    @Test
    fun modelFailed() = paparazzi.snapshotThemes("setup_model_failed_large_text") { Setup(PreviewData.setupModelFailed) }

    @Test
    fun runtimeSheet() = paparazzi.snapshotThemes("runtime_sheet_large_text") {
        RuntimeSheet(RuntimeSheetUiState.Loaded(PreviewData.runtimeInfo))
    }
}
