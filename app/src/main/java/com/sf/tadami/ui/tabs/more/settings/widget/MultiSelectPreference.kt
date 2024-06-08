package com.sf.tadami.ui.tabs.more.settings.widget

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.unit.Dp
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.components.dialog.alert.DialogCheckBoxRow

@Composable
fun <T>MultiSelectPreference(
    customPrefsVerticalPadding : Dp? = null,
    value : Set<T>,
    items : Map<T,Pair<String,Boolean>>,
    title : String,
    overrideOkButton : Boolean,
    subtitleProvider : @Composable () -> String?,
    onValueChange: (items : Set<T>) -> Unit

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

        val selectedItems = remember { value.toMutableStateList() }

        CustomAlertDialog(
            title = {
                Text(text = title)
            },
            onDismissRequest = {
                showDialog = false
            },
            confirmButton = {
                DefaultDialogConfirmButton(enabled = overrideOkButton || value != selectedItems.toSet()) {
                    onValueChange(selectedItems.toSet())
                    showDialog = false
                }
            },
            dismissButton = {
                DefaultDialogCancelButton()
            }
        ) {
            LazyColumn{
                items(items.toList()){(item,label) ->
                    DialogCheckBoxRow(label = label.first, isSelected = selectedItems.contains(item),enabled = label.second) {
                        if(it){
                            selectedItems.remove(item)
                        }else{
                            selectedItems.add(item)
                        }
                    }
                }
            }
        }
    }
}