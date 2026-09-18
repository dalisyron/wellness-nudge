package com.mimik.wellnessnudge.ui.runtime

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mimik.wellnessnudge.bootstrap.RuntimeInfo
import com.mimik.wellnessnudge.ui.components.WellnessBottomSheet

/**
 * What runs on the phone, opened from the On-device pill: a [WellnessBottomSheet] with the
 * runtime, the mim, AI inference, the models and the stored nudges, read afresh on each open.
 *
 * @param loadRuntimeInfo fetches port, mim health, models and nudge count (BootstrapViewModel).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeSheetRoute(
    loadRuntimeInfo: suspend () -> RuntimeInfo,
    onDismiss: () -> Unit,
) {
    val viewModel: RuntimeSheetViewModel = viewModel(
        factory = viewModelFactory { initializer { RuntimeSheetViewModel(loadRuntimeInfo) } },
    )
    LaunchedEffect(viewModel) { viewModel.refresh() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    WellnessBottomSheet(onDismissRequest = onDismiss) {
        RuntimeSheetContent(state = state, onRetry = viewModel::refresh)
    }
}
