package com.sf.tadami.ui.components.filters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.ui.utils.padding

@Composable
fun TriStateItem(
    label: String,
    state: ToggleableState,
    setFilters: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = rememberRipple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                setFilters()
            }
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.large),
    ) {
        Icon(
            imageVector = when (state) {
                ToggleableState.Indeterminate -> Icons.Rounded.CheckBoxOutlineBlank
                ToggleableState.On -> Icons.Rounded.CheckBox
                ToggleableState.Off -> Icons.Rounded.DisabledByDefault
            },
            contentDescription = null,
            tint = if (state == ToggleableState.Indeterminate) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}