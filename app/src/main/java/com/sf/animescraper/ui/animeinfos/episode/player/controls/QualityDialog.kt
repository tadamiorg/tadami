package com.sf.animescraper.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sf.animescraper.R
import com.sf.animescraper.network.scraping.dto.crypto.StreamSource

@Composable
fun QualityDialog(
    sources: List<StreamSource>,
    selectedSource : StreamSource? = null,
    onSelectSource: (source: StreamSource) -> Unit,
    onOutsideClick: () -> Unit
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(selectedSource ?: sources.firstOrNull()) }
    Box(modifier = Modifier
        .fillMaxSize()
        .clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { onOutsideClick() }
        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { }
                .padding(10.dp, 5.dp, 10.dp, 10.dp)
                .size(280.dp, 240.dp),
        ) {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                Text(
                    modifier = Modifier.padding(15.dp, 5.dp, 5.dp, 5.dp),
                    style = MaterialTheme.typography.titleMedium,
                    text = stringResource(id = R.string.player_screen_qd_title)
                )
                Divider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp)
                Column(
                    modifier = Modifier
                        .height(165.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    sources.forEach { source ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .defaultMinSize(1.dp, 1.dp)
                            .selectable(selected = (source == selectedOption), onClick = {
                                onOptionSelected(source)
                            }), horizontalArrangement = Arrangement.Start

                        ) {
                            RadioButton(modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .defaultMinSize(1.dp, 1.dp),
                                selected = (source == selectedOption),
                                onClick = { onOptionSelected(source) })
                            Text(
                                text = source.quality,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
                Divider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onOutsideClick() }, contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = stringResource(id = R.string.player_screen_qd_cancel_btn))
                    }

                    TextButton(onClick = {
                        if (selectedOption != null) {
                            onSelectSource(selectedOption)
                            onOutsideClick()
                        }
                    }, contentPadding = PaddingValues(0.dp)) {
                        Text(text = stringResource(id = R.string.player_screen_qd_ok_btn))
                    }
                }
            }
        }
    }

}

@Preview
@Composable
fun PreviewQualityDialog() {
    QualityDialog(sources = listOf(
        StreamSource("", "Gogoanime : 1080P", null),
        StreamSource("", "VidCdn : 1080P", null),
        StreamSource("", "Embed : 1080P", null),
        StreamSource("", "StreamSB : 1080P", null),
        StreamSource("", "Uptolaod : 1080P", null),
        StreamSource("", "Vaginette : 1080P", null),
        StreamSource("", "Viloeur : 1080P", null),
    ), onSelectSource = {}, onOutsideClick = {})
}
