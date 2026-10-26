package com.sf.tadami.ui.components.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun CheckBox(
    modifier: Modifier = Modifier,
    checkBoxModifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    maxWidth: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    title: String,
    state: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit
) {

    Row(modifier = modifier
        .padding(0.dp)
        .then(if (maxWidth) Modifier.fillMaxWidth() else Modifier)
        .clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            onCheckedChange(state.not())
        }, verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            modifier = checkBoxModifier,
            checked = state,
            onCheckedChange = { onCheckedChange(it) },
            enabled = true,
            interactionSource = interactionSource
        )
        Text(
            modifier = textModifier,
            text = title,
            style = textStyle
        )
    }
}

@Composable
fun CheckboxItem(label: String, checked: Boolean, onClick: () -> Unit) {
    BaseFilterItem(
        label = label,
        widget = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
            )
        },
        onClick = onClick,
    )
}
