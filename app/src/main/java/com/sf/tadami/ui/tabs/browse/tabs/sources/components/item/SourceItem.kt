package com.sf.tadami.ui.tabs.browse.tabs.sources.components.item

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.R
import com.sf.tadami.domain.source.Source

@Composable
fun SourceItem(
    source: Source,
    onClickItem: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onRecentClicked: (Source) -> Unit,
    onOptionsClicked: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSourceItem(
        modifier = modifier,
        source = source,
        onClickItem = { onClickItem(source) },
        onLongClickItem = { onLongClickItem(source) },
        action = {
            if (source.supportsLatest) {
                TextButton(onClick = { onRecentClicked(source) }) {
                    Text(
                        text = stringResource(R.string.anime_sources_screen_recents_btn),
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
            if(source.isConfigurable){
                IconButton(onClick = {
                    onOptionsClicked(source)
                }, enabled = true) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_vertical_settings),
                        contentDescription = null
                    )
                }
            }else {
                IconButton(onClick = {}, enabled = false, colors = IconButtonColors(Color.Transparent,Color.Transparent,Color.Transparent,Color.Transparent)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_vertical_settings),
                        contentDescription = null
                    )
                }
            }
        },
    )
}