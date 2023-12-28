package com.sf.tadami.ui.animeinfos.details.episodes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import com.sf.tadami.R
import com.sf.tadami.ui.utils.padding

@Composable
fun EpisodesHeader(
    modifier: Modifier = Modifier,
    episodesNumber: Int?,
    isFiltered : Boolean? = false,
    onFilterClicked: () -> Unit
) {
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
        Text(
            modifier = Modifier.weight(1f),
            text = pluralStringResource(
                id = R.plurals.details_screen_episodes_number,
                count = episodesNumber ?: 0,
                episodesNumber ?: 0
            ),
            style = MaterialTheme.typography.titleMedium
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_filter),
            tint = if(isFiltered == true) Color.Yellow else Color.Unspecified,
            contentDescription = null
        )
    }

}