package com.sf.tadami.ui.utils

import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun WebView.getHtml(): String = suspendCancellableCoroutine {
    evaluateJavascript("document.documentElement.outerHTML") { html -> it.resume(html) }
}