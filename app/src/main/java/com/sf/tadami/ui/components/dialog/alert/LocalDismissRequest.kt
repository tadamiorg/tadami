package com.sf.tadami.ui.components.dialog.alert

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

// LocalElevations.kt file

val LocalDismissRequest : ProvidableCompositionLocal<()->Unit> = compositionLocalOf { {} }