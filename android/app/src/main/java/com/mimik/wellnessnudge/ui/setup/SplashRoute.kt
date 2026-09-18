package com.mimik.wellnessnudge.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.ScreenTitle

/**
 * Shown while the mimOE runtime starts (`NotStarted`, `Step(START_RUNTIME)`).
 * PLACEHOLDER until the Splash screen (spec 4.1) replaces it.
 */
@Composable
fun SplashRoute(modifier: Modifier = Modifier) {
    DaybreakBackground(modifier) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScreenTitle(eyebrow = "Placeholder", title = "SplashRoute", subtitle = "The splash screen replaces this.")
        }
    }
}
