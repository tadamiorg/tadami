package com.sf.tadami.ui.animeinfos.details.episodes

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import com.sf.tadami.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EpisodesHeader(modifier: Modifier = Modifier, episodesNumber: Int?) {
    Text(
        modifier = modifier,
        text = pluralStringResource(id = R.plurals.details_screen_episodes_number, count = episodesNumber ?: 0, episodesNumber ?: 0),
        style = MaterialTheme.typography.bodyLarge
    )
}