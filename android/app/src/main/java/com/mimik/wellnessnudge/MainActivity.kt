package com.mimik.wellnessnudge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.bootstrap.BootstrapViewModel
import com.mimik.wellnessnudge.ui.WellnessApp
import com.mimik.wellnessnudge.ui.theme.WellnessNudgeTheme

class MainActivity : ComponentActivity() {

    private val bootstrapVm: BootstrapViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during first-run model downloads.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        bootstrapVm.start()

        setContent {
            WellnessNudgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state: BootstrapState by bootstrapVm.state.collectAsState()
                    WellnessApp(bootstrap = state, vm = bootstrapVm)
                }
            }
        }
    }
}
