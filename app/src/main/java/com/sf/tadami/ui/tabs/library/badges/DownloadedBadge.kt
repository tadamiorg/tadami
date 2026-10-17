package com.sf.tadami.ui.tabs.library.badges

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.sf.tadami.ui.components.material.TextBadge

@Composable
fun DownloadedBadge(count: Int) {
    if (count > 0) {
        TextBadge(
            text = "$count",
            color = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.onTertiary,
        )
    }
}
