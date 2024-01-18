package com.sf.tadami.ui.tabs.updates.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.utils.toRelativeString
import java.util.Date

@Composable
fun relativeDateText(
    date: Date?,
): String {
    val context = LocalContext.current

    return date
        ?.toRelativeString(
            context = context,
        )
        ?: stringResource(R.string.stub_text)
}