@file:Suppress("unused")

package com.sf.tadami.ui.animeinfos.details.infos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage

enum class AnimeCover(val ratio: Float) {
    Square(1f / 1f),
    Book(2f / 3f), ;

    
    @Composable
    operator fun invoke(
        modifier: Modifier = Modifier,
        data: Any?,
        contentDescription: String = "",
        shape: CornerBasedShape = MaterialTheme.shapes.extraSmall,
        onClick: (() -> Unit)? = null,
    ) {
        AsyncImage(
            model = data,
            placeholder = ColorPainter(CoverPlaceholderColor),
            contentDescription = contentDescription,
            modifier = modifier
                .aspectRatio(ratio)
                .clip(shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentScale = ContentScale.Crop,
        )
    }
}

private val CoverPlaceholderColor = Color(0x1F888888)