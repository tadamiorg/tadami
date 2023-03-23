package com.sf.tadami.ui.base.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.tadami.ui.utils.clickableNoIndication

@Composable
fun ContentLoader(modifier: Modifier = Modifier,isLoading: Boolean, delay : Int = 0,content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).align(Alignment.Center).clickableNoIndication {  },
            visible = isLoading,
            enter = fadeIn(animationSpec = tween(0)),
            exit = fadeOut(animationSpec = tween(delayMillis = delay))
        ) {
            CircularProgressIndicator(modifier = Modifier.wrapContentSize(),strokeWidth = 3.dp)
        }
    }
}