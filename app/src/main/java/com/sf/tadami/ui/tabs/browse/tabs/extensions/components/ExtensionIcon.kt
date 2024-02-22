package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import android.util.DisplayMetrics
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.util.ExtensionsLoader
import com.sf.tadami.utils.rememberResourceBitmapPainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExtensionIcon(
    extension: Extension,
    modifier: Modifier = Modifier,
    density: Int = DisplayMetrics.DENSITY_DEFAULT,
) {
    when (extension) {
        is Extension.Available -> {
            AsyncImage(
                model = extension.iconUrl,
                contentDescription = null,
                placeholder = ColorPainter(Color(0x1F888888)),
                error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
                modifier = modifier
                    .clip(MaterialTheme.shapes.extraSmall),
            )
        }
        is Extension.Installed -> {
            val icon by extension.getIcon(density)
            when (icon) {
                IconsLoadResult.Loading -> Box(modifier = modifier)
                is IconsLoadResult.Success -> Image(
                    bitmap = (icon as IconsLoadResult.Success<ImageBitmap>).value,
                    contentDescription = null,
                    modifier = modifier,
                )
                IconsLoadResult.Error -> Image(
                    bitmap = ImageBitmap.imageResource(id = R.mipmap.ic_default_source),
                    contentDescription = null,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun Extension.getIcon(density: Int = DisplayMetrics.DENSITY_DEFAULT): State<IconsLoadResult<ImageBitmap>> {
    val context = LocalContext.current
    return produceState<IconsLoadResult<ImageBitmap>>(initialValue = IconsLoadResult.Loading, this) {
        withContext(Dispatchers.IO) {
            value = try {
                val appInfo = ExtensionsLoader.getExtensionPackageInfoFromPkgName(context, pkgName)!!.applicationInfo
                val appResources = context.packageManager.getResourcesForApplication(appInfo)
                IconsLoadResult.Success(
                    appResources.getDrawableForDensity(appInfo.icon, density, null)!!
                        .toBitmap()
                        .asImageBitmap(),
                )
            } catch (e: Exception) {
                IconsLoadResult.Error
            }
        }
    }
}