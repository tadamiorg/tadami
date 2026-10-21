package com.sf.tadami.ui.components.modal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.IndeterminateCheckBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The control of a choice row. */
sealed interface ChoiceControl {
    data class Radio(val selected: Boolean) : ChoiceControl
    data class Check(val checked: Boolean) : ChoiceControl
    data class TriState(val state: ToggleableState) : ChoiceControl
}

private fun Modifier.choiceClick(control: ChoiceControl, enabled: Boolean, onClick: () -> Unit): Modifier =
    when (control) {
        is ChoiceControl.Radio -> selectable(control.selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
        is ChoiceControl.Check -> toggleable(control.checked, enabled = enabled, role = Role.Checkbox) { onClick() }
        is ChoiceControl.TriState -> triStateToggleable(control.state, enabled = enabled, role = Role.Checkbox, onClick = onClick)
    }

/**
 * A row of a choice list: 48dp, inset 8dp, 12dp corners. A single choice has no radio: the selected
 * row is a tonal pill with a trailing check. A multi choice has a trailing checkbox, a tri-state one
 * a trailing tri-state box.
 */
@Composable
fun ModalChoiceRow(
    label: String,
    control: ChoiceControl,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val pill = control is ChoiceControl.Radio && control.selected
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pill) colors.secondaryContainer else Color.Transparent)
            .heightIn(min = if (supporting != null) ModalDefaults.RowHeightWithSupporting else ModalDefaults.RowHeight)
            .choiceClick(control, enabled, onClick)
            .alpha(if (enabled) 1f else 0.38f)
            .padding(start = 16.dp, end = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp), verticalArrangement = Arrangement.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (pill) colors.onSecondaryContainer else colors.onSurface,
            )
            if (supporting != null) {
                Text(text = supporting, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        when (control) {
            is ChoiceControl.Radio -> if (control.selected) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onSecondaryContainer)
            }
            is ChoiceControl.Check -> Checkbox(checked = control.checked, onCheckedChange = null, enabled = enabled)
            is ChoiceControl.TriState -> Icon(
                imageVector = when (control.state) {
                    ToggleableState.On -> Icons.Filled.CheckBox
                    ToggleableState.Indeterminate -> Icons.Filled.IndeterminateCheckBox
                    ToggleableState.Off -> Icons.Filled.CheckBoxOutlineBlank
                },
                contentDescription = null,
                tint = if (control.state == ToggleableState.Off) colors.onSurfaceVariant else colors.primary,
            )
        }
    }
}

/** An option of a dialog: a leading checkbox and its label, the whole row clickable. */
@Composable
fun ModalCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ModalDefaults.RowHeight)
            .toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = horizontalPadding),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
