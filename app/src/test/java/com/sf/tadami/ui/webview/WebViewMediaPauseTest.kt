package com.sf.tadami.ui.webview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebViewMediaPauseTest {

    @Test
    fun `only the page runs without popups`() {
        assertEquals(0, WebViewMediaPause.runningWindow(windowCount = 1, screenResumed = true))
    }

    @Test
    fun `a popup pauses the pages under it`() {
        assertEquals(2, WebViewMediaPause.runningWindow(windowCount = 3, screenResumed = true))
    }

    @Test
    fun `nothing runs in the background`() {
        assertNull(WebViewMediaPause.runningWindow(windowCount = 1, screenResumed = false))
        assertNull(WebViewMediaPause.runningWindow(windowCount = 3, screenResumed = false))
    }

    @Test
    fun `nothing runs without windows`() {
        assertNull(WebViewMediaPause.runningWindow(windowCount = 0, screenResumed = true))
    }
}
