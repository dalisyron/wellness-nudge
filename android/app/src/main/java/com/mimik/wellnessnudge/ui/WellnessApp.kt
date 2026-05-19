package com.mimik.wellnessnudge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mimik.wellnessnudge.api.NudgeApi
import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.bootstrap.BootstrapViewModel

@Composable
fun WellnessApp(bootstrap: BootstrapState, vm: BootstrapViewModel) {
    when (bootstrap) {
        is BootstrapState.Ready -> {
            val api = remember(bootstrap.mimBaseUrl, bootstrap.apiKey) {
                NudgeApi.create(bootstrap.mimBaseUrl, bootstrap.apiKey)
            }
            MainNav(api)
        }
        is BootstrapState.Setup -> ModelSetupScreen(
            state = bootstrap,
            onContinue = vm::continueToMain,
            onRetry = vm::retryModel,
        )
        // NotStarted, Step, Failed all fall through to the brief loading /
        // error screen. The main app stays inaccessible until Ready.
        else -> BootstrapScreen(state = bootstrap, onRetry = vm::retry)
    }
}

private data class NavTab(
    val route: String,
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector,
)

@Composable
private fun MainNav(api: NudgeApi) {
    val nav = rememberNavController()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            NavTab("home", "Nudge", Icons.Filled.Today, Icons.Outlined.Today),
            NavTab("tips", "Tips", Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb),
            NavTab("history", "History", Icons.Filled.History, Icons.Outlined.History),
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
            ) {
                tabs.forEachIndexed { idx, tab ->
                    NavigationBarItem(
                        selected = selected == idx,
                        onClick = {
                            selected = idx
                            nav.navigate(tab.route) {
                                popUpTo("home") { inclusive = tab.route == "home" }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected == idx) tab.filled else tab.outlined,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected == idx) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            // Selected icon sits in a soft green pill (the
                            // "indicator"). Stitch uses primary green;
                            // tone it down for readability.
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .background(MaterialTheme.colorScheme.background),
        ) {
            NavHost(navController = nav, startDestination = "home") {
                composable("home") {
                    HomeScreen(api = api, onGenerated = { id ->
                        nav.navigate("result/$id")
                    })
                }
                composable(
                    "result/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: return@composable
                    ResultScreen(api = api, nudgeId = id, onDone = { nav.popBackStack() })
                }
                composable("tips") {
                    TipsScreen(api = api)
                }
                composable("history") {
                    HistoryScreen(api = api, onPick = { id -> nav.navigate("result/$id") })
                }
            }
        }
    }
}
