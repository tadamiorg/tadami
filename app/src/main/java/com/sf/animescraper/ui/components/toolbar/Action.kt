package com.sf.animescraper.ui.components.toolbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class Action(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)