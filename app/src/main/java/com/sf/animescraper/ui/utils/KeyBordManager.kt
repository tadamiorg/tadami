package com.sf.animescraper.ui.utils

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager

class KeyBoardManager(context: Context) {

    private val activity = context as Activity
    private var keyboardDismissListener: KeyboardDismissListener? = null

    private abstract class KeyboardDismissListener(
        private val rootView: View,
        private val onKeyboardDismiss: () -> Unit
    ) : ViewTreeObserver.OnGlobalLayoutListener {
        private var isKeyboardClosed: Boolean = false
        override fun onGlobalLayout() {
            val r = Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) {
                // 0.15 ratio is right enough to determine keypad height.
                isKeyboardClosed = false
            } else if (!isKeyboardClosed) {
                isKeyboardClosed = true
                onKeyboardDismiss.invoke()
            }
        }
    }

    fun attachKeyboardDismissListener(onKeyboardDismiss: () -> Unit) {
        val rootView = activity.findViewById<View>(android.R.id.content)
        keyboardDismissListener = object : KeyboardDismissListener(rootView, onKeyboardDismiss) {}
        keyboardDismissListener?.let {
            rootView.viewTreeObserver.addOnGlobalLayoutListener(it)
        }
    }

    fun release() {
        val rootView = activity.findViewById<View>(android.R.id.content)
        keyboardDismissListener?.let {
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        keyboardDismissListener = null
    }
}

@Composable
fun AppKeyboardFocusManager() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    DisposableEffect(key1 = context) {
        val keyboardManager = KeyBoardManager(context)
        keyboardManager.attachKeyboardDismissListener {
            focusManager.clearFocus()
        }
        onDispose {
            keyboardManager.release()
        }
    }
}