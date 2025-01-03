package com.sf.tadami.ui.themes.colorschemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colors for Doom theme
 * Original color scheme by LuftVerbot
 * M3 colors generated by Material Theme Builder (https://goo.gle/material-theme-builder-web)
 *
 * Key colors:
 * Primary 0xFFF38020
 * Secondary 0xFFF38020
 * Tertiary 0xFF1B1B22
 * Neutral 0xFF655C5A
 */
internal object DoomColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFFF0000),
        onPrimary = Color(0xFFFAFAFA),
        primaryContainer = Color(0xFFFF0000),
        onPrimaryContainer = Color(0xFFFAFAFA),
        secondary = Color(0xFFFF0000),
        onSecondary = Color(0xFFFAFAFA),
        secondaryContainer = Color(0xFFFF0000),
        onSecondaryContainer = Color(0xFFFAFAFA),
        tertiary = Color(0xFFBFBFBF),
        onTertiary = Color(0xFFFF0000),
        tertiaryContainer = Color(0xFFBFBFBF),
        onTertiaryContainer = Color(0xFFFF0000),
        background = Color(0xFF1B1B1B),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF1B1B1B),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF303030),
        onSurfaceVariant = Color(0xFFD8FFFF),
        surfaceTint = Color(0xFFFF0000),
        inverseSurface = Color(0xFFFAFAFA),
        inverseOnSurface = Color(0xFF313131),
        outline = Color(0xFFFF0000),
        inversePrimary = Color(0xFF6D0D0B),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFFFF0000),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFF0000),
        onPrimaryContainer = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFF6D0D0B), // Assuming 'inversePrimary' maps to 'doom_primaryInverse'
        secondary = Color(0xFFFF0000),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFF0000),
        onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFFBFBFBF),
        onTertiary = Color(0xFFFF0000),
        tertiaryContainer = Color(0xFFBFBFBF),
        onTertiaryContainer = Color(0xFFFF0000),
        background = Color(0xFF212121),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF212121),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF4D4D4D),
        onSurfaceVariant = Color(0xFFD84945),
        surfaceTint = Color(0xFFFF0000), // Assuming 'surfaceTint' maps to 'doom_primary' or similar
        inverseSurface = Color(0xFF424242),
        inverseOnSurface = Color(0xFFFAFAFA),
        outline = Color(0xFFFF0000),
    )
}