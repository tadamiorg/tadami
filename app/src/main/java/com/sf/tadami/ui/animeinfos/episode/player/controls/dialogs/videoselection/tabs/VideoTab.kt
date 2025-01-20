package com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.videoselection.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn

@Composable()
fun videoTab(
    sources: List<StreamSource>,
    selectedSource: StreamSource? = null,
    selectedOption: StreamSource?,
    onOptionSelected: (StreamSource?) -> Unit
) : ScreenTabContent {

    val listState = rememberLazyListState()

    return ScreenTabContent(
        titleRes = R.string.label_video,
    ){ contentPadding: PaddingValues, _ ->
        LaunchedEffect(Unit){
            val realSource = selectedSource ?: sources.firstOrNull()
            onOptionSelected(realSource)
            listState.animateScrollToItem(realSource?.let { sources.indexOf(it) }.takeIf { it !=-1 } ?: 0)
        }
        FastScrollLazyColumn(
            modifier = Modifier.padding(contentPadding),
            thumbAlways = true,
            state = listState
        ) {
            items(sources) { source ->
                Row(
                    modifier = Modifier
                        .defaultMinSize(1.dp, 1.dp)
                        .fillMaxWidth()
                        .selectable(
                            selected = (source == selectedOption),
                            onClick = {
                                onOptionSelected(source)
                            }
                        ),
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .defaultMinSize(1.dp, 1.dp),
                        selected = (source == selectedOption),
                        onClick = {
                            onOptionSelected(source)
                        }
                    )
                    Text(
                        text = source.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}