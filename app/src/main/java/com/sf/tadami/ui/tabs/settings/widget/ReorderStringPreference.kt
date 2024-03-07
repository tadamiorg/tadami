package com.sf.tadami.ui.tabs.settings.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.utils.moveItemDown
import com.sf.tadami.ui.utils.moveItemUp
import com.sf.tadami.ui.utils.padding

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderStringPreference(
    customPrefsVerticalPadding : Dp? = null,
    valueList : List<String>,
    items : Map<String,String>,
    title : String,
    subtitleProvider : @Composable () -> String?,
    onValueChange: (items : String) -> Unit

) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    TextPreference(
        customPrefsVerticalPadding = customPrefsVerticalPadding,
        title = title,
        subtitle = subtitleProvider(),
        onPreferenceClick = {
            showDialog = true
        }
    )

    if(showDialog){
        CustomAlertDialog(
            title = {
                Text(text = title)
            },
            onDismissRequest = {
                showDialog = false
            },
            confirmButton = {
            },
            dismissButton = {
                DefaultDialogCancelButton()
            }
        ) {
            LazyColumn{
                itemsIndexed(
                    items = valueList,
                    key = { _, item -> "item-${item}" },
                ) { index, value ->
                    ReorderListItem(
                        modifier = Modifier
                            .animateItemPlacement()
                            .padding(vertical = MaterialTheme.padding.tiny),
                        item = value to items[value],
                        canMoveUp = index != 0,
                        canMoveDown = index != valueList.lastIndex,
                        onMoveUp = {
                            val newList = valueList.moveItemUp(index)
                            onValueChange(newList.joinToString(separator = ","))
                        },
                        onMoveDown = {
                            val newList = valueList.moveItemDown(index)
                            onValueChange(newList.joinToString(separator = ","))
                        }
                    )
                }
            }
        }
    }
}