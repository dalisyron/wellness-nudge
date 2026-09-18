package com.mimik.wellnessnudge

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mimik.wellnessnudge.bootstrap.BootstrapViewModel
import com.mimik.wellnessnudge.ui.WellnessApp
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

class MainActivity : ComponentActivity() {

    private val bootstrapVm: BootstrapViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on during first-run model downloads.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        bootstrapVm.start()

        setContent {
            val darkTheme = isSystemInDarkTheme()
            // Transparent system bars whose icons follow the app theme.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme },
                )
                onDispose {}
            }
            WellnessTheme(darkTheme = darkTheme) {
                val state by bootstrapVm.state.collectAsStateWithLifecycle()
                WellnessApp(
                    bootstrap = state,
                    onRetry = bootstrapVm::retry,
                    onRetryModel = bootstrapVm::retryModel,
                    onContinue = bootstrapVm::continueToMain,
                    loadRuntimeInfo = bootstrapVm::loadRuntimeInfo,
                )
            }
        }
    }

    private companion object {
        // Scrims behind 3-button navigation, as androidx.activity uses by default.
        val LightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DarkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
