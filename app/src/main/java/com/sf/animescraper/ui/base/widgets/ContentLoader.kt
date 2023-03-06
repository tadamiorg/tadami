package com.sf.animescraper.ui.base.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ContentLoader(modifier: Modifier = Modifier,isLoading: Boolean, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = isLoading) {
            CircularProgressIndicator(strokeWidth = 3.dp)
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopStart),
            visible = isLoading.not(),
            exit = fadeOut(),
            enter = fadeIn()
        ) {
            content()
        }
    }
}