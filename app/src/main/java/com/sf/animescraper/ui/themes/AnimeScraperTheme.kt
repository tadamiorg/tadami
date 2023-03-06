package com.sf.animescraper.ui.themes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import com.google.accompanist.themeadapter.material3.createMdc3Theme

@Composable
fun AnimeScraperTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current

    val (colorScheme) = createMdc3Theme(
        context = context,
        layoutDirection = layoutDirection,
    )

    MaterialTheme(
        colorScheme = colorScheme!!,
        content = content,
        typography = Typography,
        shapes = Shapes
    )
}