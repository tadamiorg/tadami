package com.sf.tadami.ui.tabs.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import com.sf.tadami.ui.components.modal.ChoiceOption
import com.sf.tadami.ui.components.modal.TadaSingleChoiceDialog

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
        TadaSingleChoiceDialog(
            title = title,
            options = items.map { (item, label) -> ChoiceOption(item, label) },
            selected = value,
            onSelect = onValueChange,
            onDismissRequest = { showDialog = false },
        )
    }
}
