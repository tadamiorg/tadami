package com.sf.tadami.ui.components.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Top App Bar Action Item
sealed class Action(@StringRes val title: Int, val onClick: () -> Unit, val enabled: Boolean, val tint : Color?) {
    class Drawable(title: Int, @DrawableRes val icon: Int, onClick: () -> Unit, enabled: Boolean = true, tint: Color? = null) :
        Action(title, onClick, enabled,tint)

    class Vector(title: Int, val icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true,tint: Color? = null) :
        Action(title, onClick, enabled,tint)

    class DropDownVector(title: Int, val icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true,tint: Color? = null,val items : List<DropDownAction>) :
        Action(title, onClick, enabled,tint)

    class DropDownDrawable(title: Int, @DrawableRes val icon: Int, onClick: () -> Unit = {}, enabled: Boolean = true, tint: Color? = null,val items : List<DropDownAction>) :
        Action(title, onClick, enabled,tint)
}

class DropDownAction(
    val title : String,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)
