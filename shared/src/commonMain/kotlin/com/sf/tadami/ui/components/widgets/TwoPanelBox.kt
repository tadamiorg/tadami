package com.sf.tadami.ui.components.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The two panes of the tablet UI, as Mihon: the start one half the width, at most 450dp, or
 * [startFraction] of the width when that is wider
 */
@Composable
fun TwoPanelBox(
    startContent: @Composable BoxScope.() -> Unit,
    endContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    startFraction: Float? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val mihonWidth = (maxWidth / 2).coerceAtMost(450.dp)
        val firstWidth = startFraction?.let { maxOf(maxWidth * it, mihonWidth) } ?: mihonWidth
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(firstWidth),
            content = startContent,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(maxWidth - firstWidth),
            content = endContent,
        )
    }
}
