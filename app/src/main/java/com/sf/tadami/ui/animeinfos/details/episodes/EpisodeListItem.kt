package com.sf.tadami.ui.animeinfos.details.episodes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    languages : String?,
    selected : Boolean,
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
            .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
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
                        if (watchProgress != null || languages != null) DotSeparatorText()
                    }
                    if (watchProgress != null) {
                        Text(
                            text = watchProgress,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(SeenItemAlpha),
                        )
                        if (languages != null) DotSeparatorText()

                    }
                    if (languages != null) {
                        Text(
                            text = languages,
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