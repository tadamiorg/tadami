package com.sf.tadami.ui.animeinfos.details.episodes.filters

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.components.dialog.alert.DialogButtonRow
import com.sf.tadami.ui.components.filters.TabbedBottomSheetContentPadding
import com.sf.tadami.ui.tabs.library.bottomsheet.setFlags
import com.sf.tadami.ui.utils.padding

@Composable
fun DisplayTab(anime : Anime, setDisplayMode : (Long) -> Unit) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = TabbedBottomSheetContentPadding.Horizontal,
                vertical = MaterialTheme.padding.small
            )
    ) {
        DialogButtonRow(
            label = stringResource(id = R.string.filter_display_source_name),
            isSelected = anime.displayMode == Anime.DisplayMode.NAME,
            textStyle = MaterialTheme.typography.bodyMedium,
            textSpacing = MaterialTheme.padding.medium
        ) {
            val newFlag = anime.episodeFlags.setFlags(
                Anime.EPISODE_DISPLAY_NAME,Anime.EPISODE_DISPLAY_MASK
            )
            setDisplayMode(newFlag)
        }
        DialogButtonRow(
            label = stringResource(id = R.string.filter_display_episode_number),
            isSelected = anime.displayMode == Anime.DisplayMode.NUMBER,
            textStyle = MaterialTheme.typography.bodyMedium,
            textSpacing = MaterialTheme.padding.medium
        ) {
            val newFlag = anime.episodeFlags.setFlags(
                Anime.EPISODE_DISPLAY_NUMBER,Anime.EPISODE_DISPLAY_MASK
            )
            setDisplayMode(newFlag)
        }
    }
}