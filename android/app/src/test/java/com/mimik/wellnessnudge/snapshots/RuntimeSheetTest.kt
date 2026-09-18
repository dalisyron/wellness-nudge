package com.mimik.wellnessnudge.snapshots

import androidx.compose.runtime.Composable
import com.mimik.wellnessnudge.ui.components.WellnessBottomSheetFrame
import com.mimik.wellnessnudge.ui.preview.PreviewData
import com.mimik.wellnessnudge.ui.runtime.RuntimeSheetContent
import com.mimik.wellnessnudge.ui.runtime.RuntimeSheetUiState
import org.junit.Rule
import org.junit.Test

/** The runtime sheet, drawn in place of its dialog window: loaded, loading and failing. */
class RuntimeSheetTest {

    @get:Rule
    val paparazzi = wellnessPaparazzi()

    @Test
    fun loaded() = paparazzi.snapshotThemes("runtime_sheet") {
        RuntimeSheet(RuntimeSheetUiState.Loaded(PreviewData.runtimeInfo))
    }

    @Test
    fun loading() = paparazzi.snapshotThemes("runtime_sheet_loading") { RuntimeSheet(RuntimeSheetUiState.Loading) }

    /** The mim stopped answering and a model is missing: those rows turn red, the rest stay live. */
    @Test
    fun degraded() = paparazzi.snapshotThemes("runtime_sheet_degraded") {
        val info = PreviewData.runtimeInfo
        RuntimeSheet(
            RuntimeSheetUiState.Loaded(
                info.copy(
                    mimHealth = null,
                    models = info.models.mapIndexed { index, model -> if (index == 1) model.copy(ready = false) else model },
                    nudgeCount = null,
                ),
            ),
        )
    }

    @Test
    fun unavailable() = paparazzi.snapshotThemes("runtime_sheet_unavailable") { RuntimeSheet(RuntimeSheetUiState.Unavailable) }
}

@Composable
internal fun RuntimeSheet(state: RuntimeSheetUiState) {
    WellnessBottomSheetFrame { RuntimeSheetContent(state = state, onRetry = {}) }
}
