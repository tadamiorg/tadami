package com.sf.tadami.ui.tabs.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import com.sf.tadami.ui.components.modal.ChoiceOption
import com.sf.tadami.ui.components.modal.TadaMultiChoiceDialog

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
        TadaMultiChoiceDialog(
            title = title,
            options = items.map { (item, label) -> ChoiceOption(item, label.first, enabled = label.second) },
            selected = value,
            confirmEnabled = { overrideOkButton || it != value },
            onConfirm = {
                onValueChange(it)
                showDialog = false
            },
            onDismissRequest = { showDialog = false },
        )
    }
}
