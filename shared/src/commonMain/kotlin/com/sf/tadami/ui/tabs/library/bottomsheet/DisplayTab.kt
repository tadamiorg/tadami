package com.sf.tadami.ui.tabs.library.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.ui.components.filters.CheckboxItem

@Composable
fun DisplayTab(
    showCategoryTabs: Boolean,
    setShowCategoryTabs: (Boolean) -> Unit,
    showNumberOfItems: Boolean,
    setShowNumberOfItems: (Boolean) -> Unit
) {
    Column {
        CheckboxItem(
            label = stringResource(Res.string.library_display_category_tabs),
            checked = showCategoryTabs,
            onClick = { setShowCategoryTabs(!showCategoryTabs) }
        )
        CheckboxItem(
            label = stringResource(Res.string.library_display_number_of_items),
            checked = showNumberOfItems,
            onClick = { setShowNumberOfItems(!showNumberOfItems) }
        )
    }
}
