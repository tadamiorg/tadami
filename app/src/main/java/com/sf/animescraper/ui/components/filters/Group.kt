package com.sf.animescraper.ui.components.filters

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.magnifier
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sf.animescraper.R
import com.sf.animescraper.network.scraping.dto.search.AnimeFilter
import com.sf.animescraper.ui.utils.capFirstLetter
import com.sf.animescraper.ui.utils.clickableNoIndication
import com.sf.animescraper.ui.utils.lowFirstLetter
import com.sf.animescraper.ui.utils.toInt

@Composable
fun Group(
    checkBoxGroup: AnimeFilter.CheckBoxGroup,
    onUpdateGroup: (checkBoxGroup: AnimeFilter.CheckBoxGroup) -> Unit
) {

    var dialogState by rememberSaveable {
        mutableStateOf(false)
    }

    val configuration = LocalConfiguration.current

    val screenHeight = remember {
        configuration.screenHeightDp
    }

    val lazyGridState = rememberLazyGridState()

    val interactionSource = remember {
        MutableInteractionSource()
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = checkBoxGroup.name,
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    dialogState = true
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = checkBoxGroup.state.fold(0) { sum, checkbox -> sum + checkbox.state.toInt() }
                        .let {
                            if (it == 0) stringResource(
                                id = R.string.discover_search_screen_filters_group_selected_text
                            )
                            else "$it ${checkBoxGroup.name.lowFirstLetter()}"
                        },
                    style = MaterialTheme.typography.labelLarge,
                )
                IconButton(
                    onClick = { dialogState = true },
                    interactionSource = interactionSource
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = null,
                    )
                }
            }

        }

        if (dialogState) {
            Dialog(
                onDismissRequest = { dialogState = false },
            ) {
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.inverseSurface)
                        .wrapContentHeight()

                ) {

                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = checkBoxGroup.name.capFirstLetter(),
                        style = MaterialTheme.typography.labelLarge
                    )


                    LazyVerticalGrid(
                        modifier = Modifier
                            .weight(1f, false)
                            .heightIn(0.dp, (screenHeight / 2).dp),
                        state = lazyGridState,
                        columns = GridCells.Adaptive(130.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {


                        itemsIndexed(items = checkBoxGroup.state, key = { index, _ ->
                            index
                        }) { index, checkbox ->
                            CheckBox(
                                modifier = Modifier.scale(0.8f),
                                title = checkbox.name,
                                state = checkbox.state,
                                onCheckedChange = {
                                    checkBoxGroup.state[index].state = it
                                    onUpdateGroup(checkBoxGroup)
                                }
                            )
                        }


                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.inverseSurface),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            contentPadding = PaddingValues(8.dp),
                            onClick = { dialogState = false }
                        ) {
                            Text(text = stringResource(id = R.string.discover_search_screen_filters_group_ok_btn))
                        }
                    }

                }
            }
        }
    }
}