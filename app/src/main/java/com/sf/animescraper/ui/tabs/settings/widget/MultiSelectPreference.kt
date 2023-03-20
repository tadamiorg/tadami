package com.sf.animescraper.ui.tabs.settings.widget

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.sf.animescraper.ui.components.dialog.alert.*

@Composable
fun MultiSelectPreference(
    value : Set<String>,
    items : Map<String,String>,
    title : String,
    subtitleProvider : @Composable () -> String?,
    onValueChange: (items : Set<String>) -> Unit

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
                DefaultDialogConfirmButton(enabled = value != selectedItems.toSet()) {
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
                    DialogCheckBoxRow(label = label, isSelected = selectedItems.contains(item)) {
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