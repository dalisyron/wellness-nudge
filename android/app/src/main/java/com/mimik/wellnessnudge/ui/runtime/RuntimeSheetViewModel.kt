package com.mimik.wellnessnudge.ui.runtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mimik.wellnessnudge.bootstrap.RuntimeInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface RuntimeSheetUiState {
    data object Loading : RuntimeSheetUiState

    data class Loaded(val info: RuntimeInfo) : RuntimeSheetUiState

    /** The runtime couldn't be read at all. */
    data object Unavailable : RuntimeSheetUiState
}

/**
 * Reads what runs on the phone for the runtime sheet. The sheet refreshes it each time it
 * opens; the last answer stays on screen meanwhile, so reopening never flashes skeletons.
 */
class RuntimeSheetViewModel(
    private val loadRuntimeInfo: suspend () -> RuntimeInfo,
) : ViewModel() {

    private val _state = MutableStateFlow<RuntimeSheetUiState>(RuntimeSheetUiState.Loading)
    val state: StateFlow<RuntimeSheetUiState> = _state.asStateFlow()

    private var loading: Job? = null

    fun refresh() {
        if (loading?.isActive == true) return
        loading = viewModelScope.launch {
            if (_state.value !is RuntimeSheetUiState.Loaded) _state.value = RuntimeSheetUiState.Loading
            _state.value = try {
                // Loopback calls answer in well under a second; the API client's 60 s read
                // timeout is sized for model output, not for a status sheet.
                RuntimeSheetUiState.Loaded(withTimeout(LoadTimeoutMillis) { loadRuntimeInfo() })
            } catch (e: TimeoutCancellationException) {
                RuntimeSheetUiState.Unavailable
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                RuntimeSheetUiState.Unavailable
            }
        }
    }

    private companion object {
        const val LoadTimeoutMillis = 10_000L
    }
}
