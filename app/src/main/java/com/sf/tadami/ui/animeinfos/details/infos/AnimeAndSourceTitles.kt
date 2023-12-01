package com.sf.tadami.ui.animeinfos.details.infos

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import com.sf.tadami.ui.utils.secondaryItemAlpha
import com.sf.tadami.R
import com.sf.tadami.ui.utils.padding

@Composable
fun AnimeAndSourceTitles(
    appBarPadding: Dp,
    cover: () -> String,
    title: String,
    author: String?,
    artist: String?,
    status: String?,
    sourceName : String,
    isStubSource : Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.padding.medium, top = appBarPadding + MaterialTheme.padding.medium, end = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimeCover.Book(
            modifier = Modifier
                .sizeIn(maxWidth = 100.dp)
                .align(Alignment.Top),
            data = cover(),
            contentDescription = "",
        )
        Column(modifier = Modifier.padding(start = MaterialTheme.padding.medium)) {
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
                        .padding(end = MaterialTheme.padding.tiny)
                        .size(16.dp),
                )
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    Text(
                        text = status ?: stringResource(id = R.string.unknown_status),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                    DotSeparatorText()
                    if (isStubSource) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
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