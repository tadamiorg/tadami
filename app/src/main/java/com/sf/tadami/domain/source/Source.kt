package com.sf.tadami.domain.source

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.sf.tadami.extensions.ExtensionManager
import com.sf.tadami.utils.Lang
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class Source(
    val id: Long,
    val lang: Lang,
    val name: String,
    val supportsLatest: Boolean,
    val isStub: Boolean,
    val isConfigurable: Boolean
) {
    val key: () -> String = {
        when {
            else -> "$id"
        }
    }
}

val Source.icon: ImageBitmap?
    get() {
        return Injekt.get<ExtensionManager>().getAppIconForSource(id)
            ?.toBitmap()
            ?.asImageBitmap()
    }