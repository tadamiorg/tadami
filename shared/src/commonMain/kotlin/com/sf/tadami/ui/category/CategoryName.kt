package com.sf.tadami.ui.category

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.domain.category.Category

/** The name shown for the category, the Default one has none stored */
@Composable
fun Category.visualName(): String {
    return if (isSystemCategory) stringResource(Res.string.category_default) else name
}
