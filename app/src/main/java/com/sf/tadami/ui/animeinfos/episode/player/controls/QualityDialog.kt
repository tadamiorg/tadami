package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.dialog.simple.SimpleDialog

@Composable
fun QualityDialog(
    opened: Boolean,
    onDismissRequest: () -> Unit,
    sources: List<StreamSource>,
    selectedSource: StreamSource? = null,
    onSelectSource: (source: StreamSource) -> Unit,
) {
    val (selectedOption, onOptionSelected) = remember {
        mutableStateOf(selectedSource ?: sources.firstOrNull())
    }
    
    SimpleDialog(
        opened = opened,
        title = { Text(text = stringResource(id = R.string.player_screen_qd_title)) },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DefaultDialogConfirmButton {
                if (selectedOption != null) {
                    onSelectSource(selectedOption)
                    onDismissRequest()
                }
            }
        },
        dismissButton = {
            DefaultDialogCancelButton(onDismissRequest = onDismissRequest)
        }
    ) {
        LaunchedEffect(Unit){
            onOptionSelected(selectedSource ?: sources.firstOrNull())
        }
        LazyColumn {
            items(sources) { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .defaultMinSize(1.dp, 1.dp)
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
                        text = source.quality,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewQualityDialog() {
    QualityDialog(
        sources = listOf(
        StreamSource("", "Gogoanime : 1080P", null),
        StreamSource("", "VidCdn : 1080P", null),
        StreamSource("", "Embed : 1080P", null),
        StreamSource("", "StreamSB : 1080P", null),
        StreamSource("", "Uptolaod : 1080P", null),
        StreamSource("", "Vaginette : 1080P", null),
        StreamSource("", "Viloeur : 1080P", null),
    ),
        onSelectSource = {},
        onDismissRequest = {},
        opened = true
    )
}
