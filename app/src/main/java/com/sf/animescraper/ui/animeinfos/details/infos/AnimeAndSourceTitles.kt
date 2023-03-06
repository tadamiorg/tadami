package com.sf.animescraper.ui.animeinfos.details.infos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sf.animescraper.R
import com.sf.animescraper.ui.utils.secondaryItemAlpha

@Composable
fun AnimeAndSourceTitles(
    appBarPadding: Dp,
    cover: () -> String,
    title: String,
    author: String?,
    artist: String?,
    status: String?,
    sourceName : String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = appBarPadding + 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimeCover.Book(
            modifier = Modifier
                .sizeIn(maxWidth = 100.dp)
                .align(Alignment.Top),
            data = cover(),
            contentDescription = "",
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = title.ifBlank { "" }, style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = author?.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.unknown_author),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .secondaryItemAlpha()
                    .padding(top = 2.dp))
            if (!artist.isNullOrBlank() && author != artist) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .secondaryItemAlpha()
                        .padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.secondaryItemAlpha(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_status_clock),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp),
                )
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    Text(
                        text = status ?: stringResource(id = R.string.unknown_status),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                    DotSeparatorText()
                    Text(
                        text = sourceName,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun DotSeparatorText() {
    Text(text = " • ")
}