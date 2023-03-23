package com.sf.tadami.ui.base.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState

abstract class AppBarAction(
    @DrawableRes val icon: Int,
    @StringRes val name: Int,
    var onClick: (enabled: MutableState<Boolean>) -> Unit = {}
) {
    fun setOnClick(listener: (enabled: MutableState<Boolean>) -> Unit): AppBarAction {
        this.onClick = listener
        return this
    }
}
