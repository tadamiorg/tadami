package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.R
import com.sf.tadami.ui.components.material.IconButton
import com.sf.tadami.ui.utils.padding

@Composable
fun TopControl(
    modifier: Modifier = Modifier,
    title: () -> String,
    episode: String,
    onBackClicked: () -> Unit
) {
    val videoTitle = remember(title()) { title() }
    val episodeNumber = remember(episode) { episode }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.Absolute.Left) {

        IconButton(onClick = onBackClicked) {
            Icon(painter = painterResource(id = R.drawable.ic_back_arrow), contentDescription = "Go back",tint = MaterialTheme.colorScheme.onSurface)
        }

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = MaterialTheme.padding.small),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = videoTitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,

            )
            Text(
                text = episodeNumber,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )

        }
    }
}

@Preview
@Composable
fun PreviewTopControl() {
    TopControl(
        Modifier.fillMaxWidth(),
        { "Ore ga ojô-sama gakkô ni 'shomin sample' toshite gettsu sareta ken" },
        "episode 5"
    ) {}

}