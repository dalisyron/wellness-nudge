package com.mimik.wellnessnudge.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.navigation.NavDestination
import com.mimik.wellnessnudge.ui.components.NavBarItem

/** Navigation routes: three tabs under the floating bar, and full-screen nudge routes. */
object Routes {
    const val TODAY = "today"
    const val FOR_YOU = "foryou"
    const val JOURNAL = "journal"

    /** Generates a nudge from the request handed to NudgeRepository.generate. */
    const val NUDGE_NEW = "nudge/new"

    const val NUDGE_ID = "id"

    /** A saved nudge; build it with [nudge]. Its own prefix keeps it apart from [NUDGE_NEW]. */
    const val NUDGE_SAVED = "nudge/saved/{$NUDGE_ID}"

    fun nudge(id: String) = "nudge/saved/${Uri.encode(id)}"
}

/**
 * The floating bar's tabs, in order: outlined glyphs while idle, filled when selected. The
 * sunrise has no outlined cut, but it is a light glyph either way.
 */
enum class TopLevelTab(val route: String, val item: NavBarItem) {
    Today(Routes.TODAY, NavBarItem("Today", Icons.Rounded.WbTwilight)),
    ForYou(Routes.FOR_YOU, NavBarItem("For you", Icons.Outlined.AutoAwesome, Icons.Rounded.AutoAwesome)),
    Journal(Routes.JOURNAL, NavBarItem("Journal", Icons.Outlined.AutoStories, Icons.Rounded.AutoStories)),
}

internal val NavDestination.isTab: Boolean get() = TopLevelTab.entries.any { it.route == route }

internal val NavDestination.isNudge: Boolean get() = route == Routes.NUDGE_NEW || route == Routes.NUDGE_SAVED
