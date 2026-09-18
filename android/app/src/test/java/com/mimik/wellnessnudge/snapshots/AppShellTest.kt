package com.mimik.wellnessnudge.snapshots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.testing.FakeNudgeApi
import com.mimik.wellnessnudge.ui.navigation.MainScaffold
import com.mimik.wellnessnudge.ui.preview.PreviewData
import org.junit.Rule
import org.junit.Test

/** The main scaffold: the floating tab bar over the start destination. */
class AppShellTest {

    @get:Rule
    val paparazzi = wellnessPaparazzi()

    @Test
    fun shell() = paparazzi.snapshotThemes("shell_today") { Shell() }
}

@Composable
private fun Shell() {
    // Paparazzi provides no ViewModelStoreOwner, which the nav host needs.
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        val repository = remember { NudgeRepository(FakeNudgeApi()) }
        MainScaffold(repository = repository, loadRuntimeInfo = { PreviewData.runtimeInfo })
    }
}
