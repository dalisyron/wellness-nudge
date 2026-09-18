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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.bootstrap.BootstrapViewModel
import com.mimik.wellnessnudge.ui.WellnessApp
import com.mimik.wellnessnudge.ui.format.LocalWellnessClock
import com.mimik.wellnessnudge.ui.format.rememberSystemClock
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

class MainActivity : ComponentActivity() {

    private val bootstrapVm: BootstrapViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Starts setup on first launch and after process death; a no-op once it is under way.
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
            val state by bootstrapVm.state.collectAsStateWithLifecycle()
            // Keep the screen on only while setup works (first-run model downloads take minutes).
            val working = when (val current = state) {
                BootstrapState.NotStarted, is BootstrapState.Step -> true
                is BootstrapState.Setup -> current.anyInFlight
                is BootstrapState.Ready, is BootstrapState.Failed -> false
            }
            DisposableEffect(working) {
                if (working) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {}
            }
            WellnessTheme(darkTheme = darkTheme) {
                // Kept current, so greetings, dates and day names move on while the app is open.
                CompositionLocalProvider(LocalWellnessClock provides rememberSystemClock()) {
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
    }

    private companion object {
        // Scrims behind 3-button navigation, as androidx.activity uses by default.
        val LightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DarkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
