package com.sf.tadami.ui.components.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.source.model.AnimeFilter
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.utils.capFirstLetter
import com.sf.tadami.ui.utils.lowFirstLetter
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.toInt

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
                style = MaterialTheme.typography.labelLarge
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
                        painter = painterResource(id = if(!dialogState) R.drawable.ic_arrow_down else R.drawable.ic_arrow_up),
                        contentDescription = null,
                    )
                }
            }

        }

        if (dialogState) {
            CustomAlertDialog(
                title = {
                    Text(text = checkBoxGroup.name.capFirstLetter())
                },
                onDismissRequest = { dialogState = false },
                confirmButton = {
                    DefaultDialogConfirmButton {
                        dialogState = false
                    }
                }
            ) {
                LazyVerticalGrid(
                    modifier = Modifier
                        .weight(1f, false)
                        .heightIn(0.dp, (screenHeight / 2).dp),
                    state = lazyGridState,
                    columns = GridCells.Adaptive(130.dp),
                    contentPadding = PaddingValues(end = MaterialTheme.padding.extraSmall)
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
            }
        }
    }
}