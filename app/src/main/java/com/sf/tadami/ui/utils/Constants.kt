package com.sf.tadami.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

const val SeenItemAlpha = .38f
const val SecondaryItemAlpha = .78f

const val GridSelectedCoverAlpha = 0.76f


object CommonAnimeItemDefaults {
    val GridHorizontalSpacer = 16.dp
    val GridVerticalSpacer = 16.dp

    const val BrowseLibraryCoverAlpha = 0.34f
}
object ImageDefaults {
    val CoverPlaceholderColor = Color(0x1F888888)
}

class Padding {

    val extraLarge = 32.dp

    val large = 24.dp

    val medium = 16.dp

    val small = 12.dp

    val extraSmall = 8.dp

    val tiny = 4.dp
}

val MaterialTheme.padding: Padding
    get() = Padding()