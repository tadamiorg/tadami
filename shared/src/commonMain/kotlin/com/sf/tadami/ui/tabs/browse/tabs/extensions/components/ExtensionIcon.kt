package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import coil3.compose.AsyncImage
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.domain.extensions.Extension

@Composable
fun ExtensionIcon(
    extension: Extension,
    modifier: Modifier = Modifier,
) {
    when (extension) {
        is Extension.Available -> {
            AsyncImage(
                model = extension.iconUrl,
                contentDescription = null,
                placeholder = ColorPainter(Color(0x1F888888)),
                error = painterResource(Res.drawable.cover_error),
                modifier = modifier
                    .clip(MaterialTheme.shapes.extraSmall),
            )
        }
        is Extension.Installed -> {
            val icon by extension.getIcon()
            when (icon) {
                IconsLoadResult.Loading -> Box(modifier = modifier)
                is IconsLoadResult.Success -> Image(
                    bitmap = (icon as IconsLoadResult.Success<ImageBitmap>).value,
                    contentDescription = null,
                    modifier = modifier,
                )
                IconsLoadResult.Error -> Image(
                    bitmap = imageResource(Res.drawable.ic_default_source),
                    contentDescription = null,
                    modifier = modifier,
                )
            }
        }
    }
}

/** The icon loaded with the extension. */
@Composable
private fun Extension.Installed.getIcon(): State<IconsLoadResult<ImageBitmap>> {
    return produceState<IconsLoadResult<ImageBitmap>>(initialValue = IconsLoadResult.Loading, icon) {
        value = icon?.let { IconsLoadResult.Success(it) } ?: IconsLoadResult.Error
    }
}