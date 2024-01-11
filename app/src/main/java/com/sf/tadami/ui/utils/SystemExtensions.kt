package com.sf.tadami.ui.utils

import com.google.android.material.color.DynamicColors
import com.sf.tadami.utils.DeviceUtil

val DeviceUtil.isDynamicColorAvailable by lazy {
    DynamicColors.isDynamicColorAvailable()
}