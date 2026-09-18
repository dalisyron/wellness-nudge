package com.mimik.wellnessnudge.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner radii of the Daybreak system. Chips and buttons are full pills. */
object WellnessShapes {
    val Card = RoundedCornerShape(28.dp)
    val Tile = RoundedCornerShape(24.dp)
    val Input = RoundedCornerShape(20.dp)
    val SheetTop = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val Pill = CircleShape
}

/** Layout rhythm shared by every screen. */
object WellnessSpacing {
    /** Horizontal screen margin. */
    val ScreenMargin = 20.dp

    /** Between sections of a screen. */
    val SectionGap = 28.dp

    /** Between items inside a section. */
    val ItemGap = 12.dp

    /** From an eyebrow or section header to its content. */
    val EyebrowGap = 12.dp

    val CardPadding = 20.dp
    val TilePadding = 18.dp
}

internal val WellnessMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = WellnessShapes.Input,
    large = WellnessShapes.Tile,
    extraLarge = WellnessShapes.Card,
)
