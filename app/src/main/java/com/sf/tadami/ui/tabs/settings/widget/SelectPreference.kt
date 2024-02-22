package com.sf.tadami.ui.tabs.settings.widget

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DialogButtonRow

@Composable
fun <T : Any>SelectPreference(
    customPrefsVerticalPadding : Dp? = null,
    value : T,
    items : Map<out T,String>,
    title : String,
    subtitleProvider : () -> String?,
    onValueChange: (item : T) -> Unit
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
                DefaultDialogCancelButton()
            }
        ) {
            LazyColumn{
                items(items.toList()){(item,label) ->
                    DialogButtonRow(label = label, isSelected = item == value) {
                        onValueChange(item)
                        showDialog = false
                    }
                }
            }
        }
    }
}