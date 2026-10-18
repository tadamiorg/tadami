package com.sf.tadami.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.category.Category

/** The name shown for the category, the Default one has none stored */
@Composable
fun Category.visualName(): String {
    return if (isSystemCategory) stringResource(R.string.category_default) else name
}
