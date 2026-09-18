package com.mimik.wellnessnudge.snapshots

import org.junit.Rule
import org.junit.Test

/**
 * Pills and chips at 150% font size: labels must grow with the text, never clip. The frame
 * is taller than the phone so a page's overflow shows instead of being cut off.
 */
class LargeTextGalleryTest {

    @get:Rule
    val paparazzi = wellnessPaparazzi(Pixel9ProXL.copy(fontScale = 1.5f, screenHeight = 2800))

    @Test
    fun actions() = paparazzi.snapshotThemes("actions_large_text") { ActionsPage() }

    @Test
    fun chips() = paparazzi.snapshotThemes("chips_large_text") { ChipsPage() }
}
