package com.sf.tadami.ui.utils

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner

/** The view models kept for the whole app rather than one screen: the activity's on Android. */
val LocalAppViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner> {
    error("No app ViewModelStoreOwner, see HomeScreen")
}
