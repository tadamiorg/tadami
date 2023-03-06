package com.sf.animescraper.ui.components.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun CheckBox(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
    title: String,
    state: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit
) {

    val interactionSource = remember {
        MutableInteractionSource()
    }
    Row(modifier = Modifier.padding(0.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            modifier = modifier,
            checked = state,
            onCheckedChange = { onCheckedChange(it) },
            enabled = true,
            interactionSource = interactionSource
        )
        Text(
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onCheckedChange(state.not())
            }, text = title, style = textStyle
        )
    }
}