package com.sf.tadami.ui.utils

import androidx.compose.ui.Modifier

/** Keeps the system's back and edge gestures off this element: systemGestureExclusion on Android. */
expect fun Modifier.excludeSystemGestures(): Modifier
