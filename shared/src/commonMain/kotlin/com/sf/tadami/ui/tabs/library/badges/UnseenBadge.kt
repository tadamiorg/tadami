package com.sf.tadami.ui.tabs.library.badges

import androidx.compose.runtime.Composable
import com.sf.tadami.ui.components.material.TextBadge

@Composable
fun UnseenBadge(count: Long) {
    if (count > 0) {
        TextBadge(text = "$count")
    }
}