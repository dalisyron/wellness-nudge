package com.mimik.wellnessnudge.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Reports whether any of this element is inside the window: false once it is scrolled or
 * clipped out of view. Decorative loops use it to rest while nobody can see them.
 */
internal fun Modifier.onScreenChanged(onChange: (onScreen: Boolean) -> Unit): Modifier =
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        onChange(bounds.width > 0f && bounds.height > 0f)
    }

/**
 * Frame pause for slow decorative loops (the idle orb, a status pulse): about 30 frames a
 * second, which such motion doesn't need more of, and which lets the display lower its
 * refresh rate instead of redrawing at 120 Hz for as long as the screen is open.
 */
internal const val SlowFrameMillis = 33L
