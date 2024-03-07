package com.sf.tadami.ui.components.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

@Composable
fun CheckBox(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    title: String,
    state: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit
) {

    val interactionSource = remember {
        MutableInteractionSource()
    }
    Row(modifier = Modifier
        .padding(0.dp)
        .fillMaxWidth()
        .clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            onCheckedChange(state.not())
        }, verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            modifier = modifier,
            checked = state,
            onCheckedChange = { onCheckedChange(it) },
            enabled = true,
            interactionSource = interactionSource
        )
        Text(
            text = title, style = textStyle
        )
    }
}