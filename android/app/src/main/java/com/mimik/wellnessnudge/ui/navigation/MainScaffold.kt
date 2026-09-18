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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.mimik.wellnessnudge.data.GenerationState
import com.mimik.wellnessnudge.data.NudgeRepository
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.FloatingNavBar
import com.mimik.wellnessnudge.ui.components.FloatingNavBarDefaults
import com.mimik.wellnessnudge.ui.foryou.ForYouRoute
import com.mimik.wellnessnudge.ui.journal.JournalRoute
import com.mimik.wellnessnudge.ui.nudge.NudgeRoute
import com.mimik.wellnessnudge.ui.runtime.RuntimeSheetRoute
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.today.TodayRoute

/**
 * The app after setup: tabs and nudge screens in one nav host, the floating tab bar
 * (shown on tabs only, its selection derived from the back stack) and the runtime sheet.
 *
 * The Daybreak canvas is drawn once, behind the nav host, so tab crossfades keep a steady
 * glow. Every navigation event checks where it comes from: taps that land on a screen that
 * is entering or leaving (both stay drawn and touchable during a 350 ms transition) are
 * dropped, so a double tap can't open two nudges or pop a tab.
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
    // The keyboard covers the bar, so the bar steps aside while it is up.
    val keyboardUp by keyboardUpState()

    DaybreakBackground(modifier) {
        WellnessNavHost(
            navController = navController,
            repository = repository,
            tabContentPadding = tabContentPadding(),
            onOpenRuntime = { runtimeSheetOpen = true },
        )
        AnimatedVisibility(
            visible = onTab && !keyboardUp,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(screenTween()) + slideInVertically(screenTween()) { it / 2 },
            exit = fadeOut(screenTween()) + slideOutVertically(screenTween()) { it / 2 },
        ) {
            FloatingNavBar(
                items = items,
                selectedIndex = selectedTab,
                onSelect = { index ->
                    // Ignore taps while the bar slides away under an opening nudge.
                    if (navController.currentDestination?.isTab == true) {
                        navController.navigateToTab(TopLevelTab.entries[index])
                    }
                },
            )
        }
    }

    if (runtimeSheetOpen) {
        RuntimeSheetRoute(loadRuntimeInfo = loadRuntimeInfo, onDismiss = { runtimeSheetOpen = false })
    }
}

/** Whether the keyboard takes space at the bottom; recomposes only when that flips. */
@Composable
private fun keyboardUpState(): State<Boolean> {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    return remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
}

/**
 * Bottom padding for tab content: the floating bar and the navigation bar under it, or
 * the keyboard while it is taller. Read lazily during layout, so it follows the keyboard
 * animation without recomposing.
 */
@Composable
private fun tabContentPadding(): PaddingValues {
    val bar = WindowInsets(bottom = FloatingNavBarDefaults.Height + FloatingNavBarDefaults.BottomMargin)
    return WindowInsets.navigationBars.add(bar)
        .union(WindowInsets.ime)
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
}

@Composable
private fun WellnessNavHost(
    navController: NavHostController,
    repository: NudgeRepository,
    tabContentPadding: PaddingValues,
    onOpenRuntime: () -> Unit,
) {
    // Nudge screens open only from a settled tab: not from one being left, not twice.
    fun fromTab(entry: NavBackStackEntry) = entry.isResumed && navController.currentDestination?.isTab == true

    fun openNudge(entry: NavBackStackEntry, id: String) {
        if (fromTab(entry)) navController.navigate(Routes.nudge(id)) { launchSingleTop = true }
    }

    fun generate(entry: NavBackStackEntry, request: NudgeRequest) {
        if (!fromTab(entry)) return
        repository.generate(request)
        navController.navigate(Routes.NUDGE_NEW) { launchSingleTop = true }
    }

    fun goToToday(entry: NavBackStackEntry) {
        if (fromTab(entry)) navController.navigateToTab(TopLevelTab.Today)
    }

    // A new try replaces the nudge on screen, so Back and Done return to where it was opened from.
    fun tryAnother(entry: NavBackStackEntry, request: NudgeRequest) {
        val current = navController.currentDestination
        if (!entry.isResumed || current?.isNudge != true) return
        repository.generate(request)
        if (current.route != Routes.NUDGE_NEW) {
            navController.navigate(Routes.NUDGE_NEW) {
                popUpTo(current.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Only ever leave a nudge screen: a repeated tap during the exit must not pop the tab below.
    val back: () -> Unit = {
        if (navController.currentDestination?.isNudge == true) navController.popBackStack()
    }

    NavHost(navController = navController, startDestination = Routes.TODAY) {
        tab(Routes.TODAY) { entry ->
            TodayRoute(
                repository = repository,
                onGenerate = { generate(entry, it) },
                onOpenRuntime = onOpenRuntime,
                contentPadding = tabContentPadding,
            )
        }
        tab(Routes.FOR_YOU) { entry ->
            ForYouRoute(
                repository = repository,
                onOpenNudge = { openNudge(entry, it) },
                onCreateNudge = { goToToday(entry) },
                contentPadding = tabContentPadding,
            )
        }
        tab(Routes.JOURNAL) { entry ->
            JournalRoute(
                repository = repository,
                onOpenNudge = { openNudge(entry, it) },
                onCreateNudge = { goToToday(entry) },
                contentPadding = tabContentPadding,
            )
        }
        nudge(Routes.NUDGE_NEW) { entry ->
            // Nothing to show (e.g. the stack came back after process death with a fresh
            // repository, or the generation was reset): leave rather than render a blank screen.
            val generation by repository.generation.collectAsStateWithLifecycle()
            if (generation is GenerationState.Idle) {
                LaunchedEffect(Unit) { back() }
            }
            NudgeRoute(
                repository = repository,
                nudgeId = null,
                onBack = back,
                onTryAnother = { tryAnother(entry, it) },
                onDone = back,
            )
        }
        nudge(Routes.NUDGE_SAVED, listOf(navArgument(Routes.NUDGE_ID) { type = NavType.StringType })) { entry ->
            NudgeRoute(
                repository = repository,
                nudgeId = requireNotNull(entry.arguments?.getString(Routes.NUDGE_ID)) { "nudge route without an id" },
                onBack = back,
                onTryAnother = { tryAnother(entry, it) },
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

/** Settled on screen: not entering, leaving or covered. */
private val NavBackStackEntry.isResumed: Boolean
    get() = lifecycle.currentState == Lifecycle.State.RESUMED

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
