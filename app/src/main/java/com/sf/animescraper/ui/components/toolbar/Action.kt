package com.sf.animescraper.ui.components.toolbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector


sealed class Action(@StringRes val title: Int, val onClick: () -> Unit, val enabled: Boolean) {
    class Drawable(title: Int, @DrawableRes val icon: Int, onClick: () -> Unit, enabled: Boolean = true) :
        Action(title, onClick, enabled)

    class Vector(title: Int, val icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true) :
        Action(title, onClick, enabled)
}
