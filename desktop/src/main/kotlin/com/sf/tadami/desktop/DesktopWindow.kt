package com.sf.tadami.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Frame
import java.awt.Window

/** The app's window, for the screens that change it: the player goes full screen. */
object DesktopWindow {

    lateinit var state: WindowState

    /** Set once the window is open. */
    var window: Window? = null

    private var inFullscreen by mutableStateOf(false)

    val isFullscreen: Boolean get() = inFullscreen

    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    /** Where the window goes back to when it leaves full screen. */
    private var placementBefore = WindowPlacement.Floating
    private var borderless: WindowsBorderless? = null

    fun setFullscreen(fullscreen: Boolean) {
        if (fullscreen == isFullscreen) return
        val frame = window as? Frame
        when {
            // Java's full screen window of Windows stays over the others, or gets minimized as soon as
            // another one has the focus: a borderless window over the monitor instead, as video players do
            isWindows && frame != null -> {
                borderless = if (fullscreen) WindowsBorderless.enter(frame) else borderless?.let { it.exit(); null }
            }
            fullscreen -> {
                placementBefore = state.placement
                state.placement = WindowPlacement.Fullscreen
            }
            else -> state.placement = placementBefore
        }
        inFullscreen = fullscreen
    }
}

/** A window of Windows made borderless over its monitor, which [exit] puts back as it was. */
private class WindowsBorderless(
    private val frame: Frame,
    private val hwnd: WinDef.HWND,
    private val style: Int,
    private val rect: WinDef.RECT,
    private val maximized: Boolean,
) {

    fun exit() {
        val user32 = User32.INSTANCE
        user32.SetWindowLong(hwnd, WinUser.GWL_STYLE, style)
        user32.SetWindowPos(
            hwnd, null, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top,
            WinUser.SWP_NOZORDER or WinUser.SWP_NOACTIVATE or WinUser.SWP_FRAMECHANGED,
        )
        if (maximized) frame.extendedState = Frame.MAXIMIZED_BOTH
    }

    companion object {
        private const val FRAME_STYLES = WinUser.WS_CAPTION or WinUser.WS_THICKFRAME or
            WinUser.WS_MINIMIZEBOX or WinUser.WS_MAXIMIZEBOX or WinUser.WS_SYSMENU

        fun enter(frame: Frame): WindowsBorderless {
            val user32 = User32.INSTANCE
            val maximized = frame.extendedState and Frame.MAXIMIZED_BOTH == Frame.MAXIMIZED_BOTH
            // A maximized window would only cover the work area, without the taskbar
            if (maximized) frame.extendedState = Frame.NORMAL
            val hwnd = WinDef.HWND(Native.getWindowPointer(frame))
            val style = user32.GetWindowLong(hwnd, WinUser.GWL_STYLE)
            val rect = WinDef.RECT().also { user32.GetWindowRect(hwnd, it) }
            val monitor = WinUser.MONITORINFO().also {
                user32.GetMonitorInfo(user32.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST), it)
            }.rcMonitor
            user32.SetWindowLong(hwnd, WinUser.GWL_STYLE, style and FRAME_STYLES.inv())
            user32.SetWindowPos(
                hwnd, null, monitor.left, monitor.top, monitor.right - monitor.left, monitor.bottom - monitor.top,
                WinUser.SWP_NOZORDER or WinUser.SWP_NOACTIVATE or WinUser.SWP_FRAMECHANGED,
            )
            return WindowsBorderless(frame, hwnd, style, rect, maximized)
        }
    }
}
