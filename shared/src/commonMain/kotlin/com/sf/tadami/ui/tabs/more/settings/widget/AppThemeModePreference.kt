package com.sf.tadami.ui.tabs.more.settings.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.Dp
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.preferences.appearance.ThemeMode

private val options = mapOf(
    ThemeMode.SYSTEM to Res.string.theme_system,
    ThemeMode.LIGHT to Res.string.theme_light,
    ThemeMode.DARK to Res.string.theme_dark,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeModePreference(
    value: ThemeMode,
    customPrefsVerticalPadding : Dp? = null,
    onItemClick: (ThemeMode) -> Unit,
) {
    BasePreference(
        customPrefsVerticalPadding = customPrefsVerticalPadding,
        subcomponent = {
            MultiChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PrefsHorizontalPadding),
            ) {
                options.onEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        checked = mode == value,
                        onCheckedChange = { onItemClick(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index,
                            options.size,
                        ),
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }
        },
    )
}