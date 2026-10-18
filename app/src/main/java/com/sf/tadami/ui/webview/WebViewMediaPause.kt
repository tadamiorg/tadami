package com.sf.tadami.ui.webview

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Keeps the media of the pages off the screen paused: the page under a popup, and all of them while
 * the screen is in the background. Nothing plays again by itself once they're shown again, the user
 * resumes it.
 */
object WebViewMediaPause {

    /**
     * The window whose WebView runs, the others are paused ([WebView.onPause]): the one on top, while
     * the screen is resumed. Null when none runs.
     */
    fun runningWindow(windowCount: Int, screenResumed: Boolean): Int? =
        if (screenResumed && windowCount > 0) windowCount - 1 else null

    /** Adds [SCRIPT] to every frame [webView] loads, the cross-origin player iframes included. */
    fun install(webView: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(webView, SCRIPT, setOf("*"))
        }
    }

    /**
     * A paused WebView hides its pages. The WebView pauses their videos then, but not their audio, and
     * plays them again once shown: this pauses all their media, and keeps them paused until the user
     * touches the frame, or a second after it's shown. That also keeps an ad from starting a player
     * meanwhile.
     */
    private const val SCRIPT = """
(function () {
  if (window.__tadamiMediaPause) return;
  window.__tadamiMediaPause = true;
  var blocked = document.visibilityState === 'hidden';
  var timer = 0;
  document.addEventListener('visibilitychange', function () {
    clearTimeout(timer);
    if (document.visibilityState === 'hidden') {
      blocked = true;
      document.querySelectorAll('video, audio').forEach(function (media) { media.pause(); });
    } else {
      timer = setTimeout(function () { blocked = false; }, 1000);
    }
  }, true);
  document.addEventListener('play', function (event) {
    if (blocked) event.target.pause();
  }, true);
  ['pointerdown', 'keydown'].forEach(function (type) {
    window.addEventListener(type, function () {
      if (document.visibilityState === 'visible') {
        clearTimeout(timer);
        blocked = false;
      }
    }, true);
  });
})();
"""
}
