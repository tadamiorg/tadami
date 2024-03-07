package com.sf.tadami.ui.animeinfos.details.episodes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.R
import com.sf.tadami.ui.themes.colorschemes.active
import com.sf.tadami.ui.utils.padding


@Composable
fun EpisodesHeader(
    modifier: Modifier = Modifier,
    totalEpisodes: Int?,
    filteredEpisodes : Int? = totalEpisodes,
    isFiltered : Boolean? = false,
    onFilterClicked: () -> Unit
) {

    val filterSameSize by remember(totalEpisodes,filteredEpisodes){
        derivedStateOf {
            totalEpisodes == filteredEpisodes
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onFilterClicked()
            }
            .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.tiny),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val filterTint = if(isFiltered == true) MaterialTheme.colorScheme.active else LocalContentColor.current
        Text(
            modifier = Modifier.weight(1f),
            text = pluralStringResource(
                id = R.plurals.details_screen_episodes_number,
                count = if(!filterSameSize) filteredEpisodes ?: 0 else totalEpisodes ?: 0,
                if(!filterSameSize) filteredEpisodes ?: 0 else totalEpisodes ?: 0
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_filter),
            tint = filterTint,
            contentDescription = null
        )
    }

}