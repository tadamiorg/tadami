package com.sf.tadami.ui.components.dialog.alert

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.sf.tadami.ui.utils.padding

/* TODO Maybe use only material3 instead of material*/

@Composable
fun DialogButtonRow(
    textStyle : TextStyle = MaterialTheme.typography.bodyLarge.merge(),
    textSpacing : Dp = MaterialTheme.padding.large,
    label: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .selectable(
                selected = isSelected,
                onClick = { if (!isSelected) onSelected() },
            )
            .fillMaxWidth()
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Text(
            text = label,
            style = textStyle,
            modifier = Modifier.padding(start = textSpacing),
        )
    }
}