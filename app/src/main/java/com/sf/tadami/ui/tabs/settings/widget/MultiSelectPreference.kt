package com.sf.tadami.ui.tabs.settings.widget

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.sf.tadami.ui.components.dialog.alert.*

@Composable
fun <T>MultiSelectPreference(
    value : Set<T>,
    items : Map<T,Pair<String,Boolean>>,
    title : String,
    overrideOkButton : Boolean,
    subtitleProvider : @Composable () -> String?,
    onValueChange: (items : Set<T>) -> Unit

) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    TextPreference(
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
                DefaultDialogCancelButton {
                    showDialog = false
                }
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