package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.videoselection.tabs

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.data.download.isLocalFile
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.components.modal.PlayerChoiceRow
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn

/** The streams, scrolled to [selectedSource]. */
@Composable
fun VideoTab(
    sources: List<StreamSource>,
    selectedSource: StreamSource?,
    onSelect: (StreamSource) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        listState.scrollToItem(selectedSource?.let { sources.indexOf(it) }.takeIf { it != -1 } ?: 0)
    }
    FastScrollLazyColumn(
        thumbAlways = true,
        state = listState
    ) {
        items(sources) { source ->
            PlayerChoiceRow(
                selected = source == selectedSource,
                label = source.fullName,
                onClick = { onSelect(source) },
                // The downloaded files carry the name of the stream they came from: tell them apart
                trailing = if (source.isLocalFile) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.DownloadDone,
                            contentDescription = stringResource(R.string.episode_downloaded),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}
