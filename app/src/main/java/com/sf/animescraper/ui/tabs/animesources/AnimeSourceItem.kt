package com.sf.animescraper.ui.tabs.animesources

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sf.animescraper.R
import com.sf.animescraper.network.api.online.AnimeSource
import com.sf.animescraper.ui.utils.capFirstLetter
import java.util.*

@SuppressLint("DiscouragedApi")
@Composable
fun AnimeSourceItem(
    source: AnimeSource,
    onRecentClicked: () -> Unit,
    onSearchClicked: () -> Unit
) {
    val context = LocalContext.current

    val resId = context.resources.getIdentifier(
        source.name.lowercase(Locale.getDefault()),
        "drawable",
        context.packageName
    )

    val image: Painter = painterResource(id = resId)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            modifier = Modifier
                .padding(8.dp)
                .width(35.dp)
                .aspectRatio(1f),
            painter = image,
            contentDescription = null
        )
        Column {
            Text(text = source.name.capFirstLetter(), style = MaterialTheme.typography.labelMedium)
            Text(
                text = stringResource(id = source.lang.getRes()),
                style = MaterialTheme.typography.labelMedium
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onRecentClicked) {
                Text(text = stringResource(id = R.string.anime_sources_screen_recents_btn))
            }
            IconButton(onClick = onSearchClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = null
                )
            }
        }
    }
}


@Preview
@Composable
fun PreviewAnimeSourceItem() {
    val image: Painter = painterResource(id = R.drawable.gogoanime)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            modifier = Modifier
                .padding(8.dp)
                .width(35.dp)
                .aspectRatio(1f),
            painter = image,
            contentDescription = null
        )
        Column {
            Text(text = "FakeName", style = MaterialTheme.typography.labelMedium)
            Text(text = "English", style = MaterialTheme.typography.labelMedium)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { }) {
                Text(text = stringResource(id = R.string.anime_sources_screen_recents_btn))
            }
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = null
                )
            }
        }
    }
}