package com.mimik.wellnessnudge.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.bootstrap.RuntimeInfo
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.ui.components.FloatingNavBar
import com.mimik.wellnessnudge.ui.components.FloatingNavBarDefaults
import com.mimik.wellnessnudge.ui.foryou.ForYouRoute
import com.mimik.wellnessnudge.ui.journal.JournalRoute
import com.mimik.wellnessnudge.ui.nudge.NudgeRoute
import com.mimik.wellnessnudge.ui.runtime.RuntimeSheetRoute
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.today.TodayRoute

/**
 * The app after setup: tabs and nudge screens in one nav host, the floating tab bar
 * (shown on tabs only, its selection derived from the back stack) and the runtime sheet.
 */
@Composable
fun MainScaffold(
    repository: NudgeRepository,
    loadRuntimeInfo: suspend () -> RuntimeInfo,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    var runtimeSheetOpen by rememberSaveable { mutableStateOf(false) }
    val current by navController.currentBackStackEntryAsState()
    val destination = current?.destination
    val onTab = destination == null || destination.isTab
    // Under a nudge screen, keep showing the tab it was opened from while the bar slides away.
    val tabEntry = if (onTab) current else navController.previousBackStackEntry
    val selectedTab = TopLevelTab.entries.indexOfFirst { it.route == tabEntry?.destination?.route }.coerceAtLeast(0)
    val items = remember { TopLevelTab.entries.map { it.item } }

    Box(
        modifier
            .fillMaxSize()
            .background(WellnessTheme.colors.bg),
    ) {
        WellnessNavHost(
            navController = navController,
            repository = repository,
            tabContentPadding = PaddingValues(bottom = FloatingNavBarDefaults.occupiedHeight()),
            onOpenRuntime = { runtimeSheetOpen = true },
        )
        AnimatedVisibility(
            visible = onTab,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(screenTween()) + slideInVertically(screenTween()) { it / 2 },
            exit = fadeOut(screenTween()) + slideOutVertically(screenTween()) { it / 2 },
        ) {
            FloatingNavBar(
                items = items,
                selectedIndex = selectedTab,
                onSelect = { navController.navigateToTab(TopLevelTab.entries[it]) },
            )
        }
    }

    if (runtimeSheetOpen) {
        RuntimeSheetRoute(loadRuntimeInfo = loadRuntimeInfo, onDismiss = { runtimeSheetOpen = false })
    }
}

@Composable
private fun WellnessNavHost(
    navController: NavHostController,
    repository: NudgeRepository,
    tabContentPadding: PaddingValues,
    onOpenRuntime: () -> Unit,
) {
    val openNudge: (String) -> Unit = { id -> navController.navigate(Routes.nudge(id)) }
    val goToToday = { navController.navigateToTab(TopLevelTab.Today) }
    val generate: (NudgeRequest) -> Unit = { request ->
        repository.generate(request)
        navController.navigate(Routes.NUDGE_NEW)
    }
    // A new try replaces the nudge on screen, so Back and Done return to where it was opened from.
    val tryAnother: (NudgeRequest) -> Unit = { request ->
        repository.generate(request)
        if (navController.currentDestination?.route != Routes.NUDGE_NEW) {
            val currentId = navController.currentDestination?.id
            navController.navigate(Routes.NUDGE_NEW) {
                if (currentId != null) popUpTo(currentId) { inclusive = true }
            }
        }
    }
    // Only ever leave a nudge screen: a repeated tap during the exit must not pop the tab below.
    val back: () -> Unit = {
        if (navController.currentDestination?.isNudge == true) navController.popBackStack()
    }

    NavHost(navController = navController, startDestination = Routes.TODAY) {
        tab(Routes.TODAY) {
            TodayRoute(
                repository = repository,
                onGenerate = generate,
                onOpenRuntime = onOpenRuntime,
                contentPadding = tabContentPadding,
            )
        }
        tab(Routes.FOR_YOU) {
            ForYouRoute(
                repository = repository,
                onOpenNudge = openNudge,
                onCreateNudge = goToToday,
                contentPadding = tabContentPadding,
            )
        }
        tab(Routes.JOURNAL) {
            JournalRoute(
                repository = repository,
                onOpenNudge = openNudge,
                onCreateNudge = goToToday,
                contentPadding = tabContentPadding,
            )
        }
        nudge(Routes.NUDGE_NEW) {
            NudgeRoute(repository = repository, nudgeId = null, onBack = back, onTryAnother = tryAnother, onDone = back)
        }
        nudge(Routes.NUDGE_SAVED, listOf(navArgument(Routes.NUDGE_ID) { type = NavType.StringType })) { entry ->
            NudgeRoute(
                repository = repository,
                nudgeId = entry.arguments?.getString(Routes.NUDGE_ID),
                onBack = back,
                onTryAnother = tryAnother,
                onDone = back,
            )
        }
    }
}

/** Switches tabs keeping one entry per tab, with each tab's state saved and restored. */
internal fun NavHostController.navigateToTab(tab: TopLevelTab) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

// Tabs crossfade. When a nudge opens over a tab, the tab stays put underneath it.
private fun NavGraphBuilder.tab(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    enterTransition = { fadeIn(tabTween()) },
    exitTransition = { if (targetState.destination.isNudge) ExitTransition.KeepUntilTransitionsFinished else fadeOut(tabTween()) },
    popEnterTransition = { if (initialState.destination.isNudge) EnterTransition.None else fadeIn(tabTween()) },
    popExitTransition = { fadeOut(tabTween()) },
    content = content,
)

// Nudge screens rise in by a twelfth of the screen while fading in, and sink back out.
private fun NavGraphBuilder.nudge(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable(
    route = route,
    arguments = arguments,
    enterTransition = { fadeIn(screenTween()) + slideInVertically(screenTween()) { it / 12 } },
    exitTransition = { ExitTransition.KeepUntilTransitionsFinished },
    popEnterTransition = { EnterTransition.None },
    popExitTransition = { fadeOut(screenTween()) + slideOutVertically(screenTween()) { it / 12 } },
    content = content,
)

private fun <T> tabTween() = tween<T>(WellnessMotion.TabFadeMillis, easing = WellnessMotion.Easing)

private fun <T> screenTween() = tween<T>(WellnessMotion.ScreenMillis, easing = WellnessMotion.Easing)
