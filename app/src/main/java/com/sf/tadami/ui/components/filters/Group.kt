package com.sf.tadami.ui.components.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.source.model.AnimeFilter
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.themes.header
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

    val screenHeight = LocalWindowInfo.current.containerSize.height

    val lazyGridState = rememberLazyGridState()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { dialogState = !dialogState }
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = checkBoxGroup.name,
                style = MaterialTheme.typography.header,
            )

            Spacer(modifier = Modifier.weight(1f))

            Row {
                Text(
                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = MaterialTheme.padding.extraSmall),
                    text = checkBoxGroup.state.fold(0) { sum, checkbox -> sum + checkbox.state.toInt() }
                        .let {
                            if (it == 0) stringResource(
                                id = R.string.discover_search_screen_filters_group_selected_text
                            )
                            else "$it ${checkBoxGroup.name.lowFirstLetter()}"
                        },
                    style = MaterialTheme.typography.labelMedium,
                )

                Icon(
                    imageVector = if (dialogState) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
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
                    .heightIn(0.dp, (screenHeight / 2).dp),
                state = lazyGridState,
                columns = GridCells.Adaptive(130.dp),
                contentPadding = PaddingValues(end = MaterialTheme.padding.extraSmall)
            ) {
                itemsIndexed(items = checkBoxGroup.state, key = { index, _ ->
                    index
                }) { index, checkbox ->
                    CheckBox(
                        checkBoxModifier = Modifier.scale(0.8f),
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