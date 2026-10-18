package com.sf.tadami.ui.components.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * An action of the app's top bar.
 *
 * @since extensions-lib 1.5
 */
@Deprecated("Part of the app's UI, not of the extension API: it will be removed from extensions-lib")
sealed class Action(@StringRes val title: Int, val onClick: () -> Unit, val enabled: Boolean, val tint : Color?) {
    /** @since extensions-lib 1.5 */
    class Drawable(title: Int, @DrawableRes val icon: Int, onClick: () -> Unit, enabled: Boolean = true, tint: Color? = null) :
        Action(title, onClick, enabled,tint)

    /** @since extensions-lib 1.5 */
    class Vector(title: Int, val icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true,tint: Color? = null) :
        Action(title, onClick, enabled,tint)

    /** @since extensions-lib 1.5 */
    class DropDownVector(title: Int, val icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true,tint: Color? = null,val items : List<DropDownAction>) :
        Action(title, onClick, enabled,tint)

    /** @since extensions-lib 1.5 */
    class DropDownDrawable(title: Int, @DrawableRes val icon: Int, onClick: () -> Unit = {}, enabled: Boolean = true, tint: Color? = null,val items : List<DropDownAction>) :
        Action(title, onClick, enabled,tint)

    /** @since extensions-lib 1.5 */
    class CastButton(title: Int = throw Exception("Stub"), onClick: () -> Unit = {}, enabled: Boolean = true, tint: Color? = null) : Action(title,onClick,enabled,tint)
}

/**
 * An item of the drop-down menu of an [Action].
 *
 * @property checked since lib 1.6, whether it shows a check mark, null for none
 * @since extensions-lib 1.5
 */
@Deprecated("Part of the app's UI, not of the extension API: it will be removed from extensions-lib")
class DropDownAction(
    val title : String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val checked: Boolean? = null,
)
