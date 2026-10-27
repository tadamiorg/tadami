package com.sf.tadami.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.tadami.ui.utils.UiToasts
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

/** The toasts of the shared code, shown over the window as Android shows them over the screen. */
object Toasts {

    private class Toast(val message: String, val duration: Int)

    private val current = MutableStateFlow<Toast?>(null)

    fun install() {
        UiToasts.display = { msg, duration -> current.value = Toast(msg, duration) }
    }

    @Composable
    fun Host() {
        val toast by current.collectAsState()
        var shown by remember { mutableStateOf<Toast?>(null) }
        LaunchedEffect(toast) {
            val next = toast ?: return@LaunchedEffect
            shown = next
            delay(if (next.duration == UiToasts.LENGTH_LONG) 3500 else 2000)
            if (current.value === next) current.value = null
            shown = null
        }
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(visible = shown != null, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.widthIn(max = 480.dp),
                ) {
                    Text(
                        text = shown?.message.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
