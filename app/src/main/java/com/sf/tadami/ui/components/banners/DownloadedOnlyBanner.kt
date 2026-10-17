package com.sf.tadami.ui.components.banners

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sf.tadami.R

val DownloadedOnlyBannerBackgroundColor: Color
    @Composable get() = MaterialTheme.colorScheme.tertiary

/** Above the app's screens while the downloaded only mode is on, drawn behind the status bar. */
@Composable
fun DownloadedOnlyBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Text(
            text = stringResource(R.string.label_downloaded_only),
            modifier = Modifier
                .background(DownloadedOnlyBannerBackgroundColor)
                .fillMaxWidth()
                .padding(4.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            color = MaterialTheme.colorScheme.onTertiary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
