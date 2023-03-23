package com.sf.tadami.ui.tabs.animesources

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sf.tadami.R
import com.sf.tadami.network.api.online.AnimeSource
import com.sf.tadami.ui.utils.ImageDefaults.CoverPlaceholderColor
import com.sf.tadami.ui.utils.capFirstLetter
import java.util.*

@SuppressLint("DiscouragedApi")
@Composable
fun AnimeSourceItem(
    source: AnimeSource,
    onRecentClicked: () -> Unit,
    onSearchClicked: () -> Unit
) {
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(source.getIconRes())
                .crossfade(true)
                .build(),
            placeholder = ColorPainter(CoverPlaceholderColor),
            contentDescription = source.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(35.dp)
                .aspectRatio(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(text = source.name.capFirstLetter(), style = MaterialTheme.typography.labelMedium)
            Text(
                text = stringResource(id = source.lang.getRes()),
                style = MaterialTheme.typography.labelMedium
            )
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false), horizontalArrangement = Arrangement.End) {
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