package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.sf.tadami.App
import com.sf.tadami.R
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity

@Composable
fun TopControl(
    modifier: Modifier = Modifier,
    title: () -> String,
    episode: String,
    onBackClicked: () -> Unit,
    onCastClicked: () -> Unit
) {
    val videoTitle = remember(title()) { title() }
    val episodeNumber = remember(episode) { episode }

    val context = LocalContext.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.SpaceBetween) {

        IconButton(onClick = onBackClicked) {
            Icon(painter = painterResource(id = R.drawable.ic_back_arrow), contentDescription = "Go back",tint = MaterialTheme.colorScheme.onSurface)
        }

        Row(
            modifier = Modifier.weight(1f,false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =  Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
        ) {
            Text(
                modifier = Modifier.weight(1f,false),
                text = videoTitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = "-",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = episodeNumber,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                textAlign = TextAlign.Center
            )
        }

        AndroidView(
            factory = {
                MediaRouteButton(context)
            },
            update = {mediaButton ->
                CastButtonFactory.setUpMediaRouteButton((context as EpisodeActivity).applicationContext, mediaButton)
            }
        )
    }
}

@Preview
@Composable
fun PreviewTopControl() {
    TopControl(Modifier.fillMaxWidth(),
        { "Ore ga ojô-sama gakkô ni 'shomin sample' toshite gettsu sareta ken" },
        "episode 5", onBackClicked = {}, onCastClicked = {})

}