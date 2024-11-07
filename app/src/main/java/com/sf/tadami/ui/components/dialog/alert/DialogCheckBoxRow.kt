package com.sf.tadami.ui.components.dialog.alert

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.sf.tadami.ui.utils.padding

@Composable
fun DialogCheckBoxRow(
    label : String,
    isSelected: Boolean,
    enabled : Boolean = true,
    onSelected: (isSelected : Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .selectable(
                enabled = enabled,
                selected = isSelected,
                onClick = { onSelected(isSelected) },
            )
            .minimumInteractiveComponentSize()
            .fillMaxWidth(),
    ) {
        Checkbox(
            enabled = enabled,
            checked = isSelected,
            onCheckedChange = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = MaterialTheme.padding.large),
        )
    }
}

