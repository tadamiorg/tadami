package com.sf.tadami.ui.tabs.updates.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
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
        ?: stringResource(Res.string.stub_text)
}