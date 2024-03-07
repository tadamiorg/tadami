package com.sf.tadami.ui.themes.colorschemes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme


internal abstract class BaseColorScheme {

    abstract val darkScheme: ColorScheme
    abstract val lightScheme: ColorScheme

    fun getColorScheme(isDark: Boolean, isAmoled: Boolean): ColorScheme {
        return (if (isDark) darkScheme else lightScheme)
            .let {
                if (isDark && isAmoled) {
                    it.copy(
                        background = Color.Black,
                        onBackground = Color.White,
                        surface = Color.Black,
                        onSurface = Color.White,
                    )
                } else {
                    it
                }
            }
    }
}


val ColorScheme.active: Color
    @Composable
    get() {
        return if (isSystemInDarkTheme()) Color(255, 235, 59) else Color(255, 193, 7)
    }
