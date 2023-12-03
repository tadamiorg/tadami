package com.sf.tadami.ui.animeinfos.details.episodes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sf.tadami.ui.animeinfos.details.infos.DotSeparatorText
import com.sf.tadami.ui.utils.SecondaryItemAlpha
import com.sf.tadami.ui.utils.SeenItemAlpha
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.selectedBackground

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EpisodeListItem(
    modifier: Modifier = Modifier,
    title: String,
    date: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    seen : Boolean,
    watchProgress : String?,
    selected : Boolean
) {
    val textAlpha = if (seen) SeenItemAlpha else 1f
    val textSubtitleAlpha = if (seen) SeenItemAlpha else SecondaryItemAlpha

    Row(
        modifier = modifier
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = MaterialTheme.padding.medium, top = MaterialTheme.padding.small, end = MaterialTheme.padding.extraSmall, bottom = MaterialTheme.padding.small),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableStateOf(0) }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier.alpha(textAlpha),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.alpha(textSubtitleAlpha)) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                ) {
                    if (date != null) {
                        Text(
                            text = date,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (watchProgress != null) DotSeparatorText()
                    }
                    if (watchProgress != null) {
                        Text(
                            text = watchProgress,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(SeenItemAlpha),
                        )

                    }
                }
            }
        }
    }
}