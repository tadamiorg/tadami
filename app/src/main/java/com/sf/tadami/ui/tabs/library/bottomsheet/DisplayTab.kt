package com.sf.tadami.ui.tabs.library.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
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
            label = stringResource(id = R.string.library_display_category_tabs),
            checked = showCategoryTabs,
            onClick = { setShowCategoryTabs(!showCategoryTabs) }
        )
        CheckboxItem(
            label = stringResource(id = R.string.library_display_number_of_items),
            checked = showNumberOfItems,
            onClick = { setShowNumberOfItems(!showNumberOfItems) }
        )
    }
}
