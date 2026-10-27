package com.sf.tadami.desktop

import java.awt.Component
import java.awt.Container
import java.awt.Window
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

/**
 * Development only, when TADAMI_AUTOMATION_PORT is set: drives the window from a local socket with
 * synthetic AWT events, without the mouse of the user. One command per line, coordinates in the window
 * (title bar included, as a screenshot of it):
 * `click x y`, `wheel x y notches`, `type text`, `key enter|esc|back|tab|left|right|space|<letter>`, and in an open file chooser
 * `choose path` or `cancel`.
 */
object DevAutomation {

    fun start(window: () -> Window?) {
        val port = System.getenv("TADAMI_AUTOMATION_PORT")?.toIntOrNull() ?: return
        thread(name = "DevAutomation", isDaemon = true) {
            ServerSocket(port, 4, InetAddress.getLoopbackAddress()).use { server ->
                while (true) {
                    server.accept().use { socket ->
                        val writer = socket.getOutputStream().bufferedWriter()
                        socket.getInputStream().bufferedReader().lineSequence().forEach { line ->
                            val result = runCatching {
                                val target = window() ?: error("No window")
                                // Not waited for: a click can open a modal dialog, which returns once closed
                                SwingUtilities.invokeLater { runCatching { run(target, line.trim()) }.onFailure { it.printStackTrace() } }
                                "ok"
                            }.getOrElse { "error ${it.message}" }
                            writer.write(result + "\n")
                            writer.flush()
                        }
                    }
                }
            }
        }
    }

    private fun run(window: Window, command: String) {
        val parts = command.split(" ", limit = 2)
        val args = parts.getOrNull(1).orEmpty()
        when (parts[0]) {
            "click" -> {
                val (x, y) = args.split(" ").map { it.toInt() }
                val (target, px, py) = at(window, x, y)
                val now = System.currentTimeMillis()
                target.dispatchEvent(MouseEvent(target, MouseEvent.MOUSE_MOVED, now, 0, px, py, 0, false))
                target.dispatchEvent(MouseEvent(target, MouseEvent.MOUSE_PRESSED, now, MouseEvent.BUTTON1_DOWN_MASK, px, py, 1, false, MouseEvent.BUTTON1))
                target.dispatchEvent(MouseEvent(target, MouseEvent.MOUSE_RELEASED, now + 50, 0, px, py, 1, false, MouseEvent.BUTTON1))
                target.dispatchEvent(MouseEvent(target, MouseEvent.MOUSE_CLICKED, now + 50, 0, px, py, 1, false, MouseEvent.BUTTON1))
            }
            "wheel" -> {
                val (x, y, notches) = args.split(" ").map { it.toInt() }
                val (target, px, py) = at(window, x, y)
                target.dispatchEvent(
                    MouseWheelEvent(
                        target, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, px, py, 0, false,
                        MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, notches,
                    ),
                )
            }
            "type" -> {
                val target = focused(window)
                args.forEach { char ->
                    target.dispatchEvent(KeyEvent(target, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, char))
                }
            }
            "key" -> {
                val code = when (args) {
                    "enter" -> KeyEvent.VK_ENTER
                    "esc" -> KeyEvent.VK_ESCAPE
                    "back" -> KeyEvent.VK_BACK_SPACE
                    "tab" -> KeyEvent.VK_TAB
                    "left" -> KeyEvent.VK_LEFT
                    "right" -> KeyEvent.VK_RIGHT
                    "space" -> KeyEvent.VK_SPACE
                    else -> args.singleOrNull()?.let { KeyEvent.getExtendedKeyCodeForChar(it.code) }
                        ?: error("Unknown key $args")
                }
                val target = focused(window)
                val now = System.currentTimeMillis()
                target.dispatchEvent(KeyEvent(target, KeyEvent.KEY_PRESSED, now, 0, code, KeyEvent.CHAR_UNDEFINED))
                target.dispatchEvent(KeyEvent(target, KeyEvent.KEY_RELEASED, now + 20, 0, code, KeyEvent.CHAR_UNDEFINED))
            }
            "choose", "cancel" -> {
                val chooser = Window.getWindows().filter { it.isShowing }
                    .firstNotNullOfOrNull { findChooser(it) } ?: error("No file chooser")
                if (parts[0] == "choose") {
                    chooser.selectedFile = File(args)
                    chooser.approveSelection()
                } else {
                    chooser.cancelSelection()
                }
            }
            else -> error("Unknown command $command")
        }
    }

    private fun findChooser(component: Component): JFileChooser? {
        if (component is JFileChooser) return component
        return (component as? Container)?.components?.firstNotNullOfOrNull { findChooser(it) }
    }

    private fun at(window: Window, x: Int, y: Int): Triple<Component, Int, Int> {
        val target = SwingUtilities.getDeepestComponentAt(window, x, y) ?: window
        val point = SwingUtilities.convertPoint(window, x, y, target)
        return Triple(target, point.x, point.y)
    }

    private fun focused(window: Window): Component =
        window.mostRecentFocusOwner ?: SwingUtilities.getDeepestComponentAt(window, window.width / 2, window.height / 2) ?: window
}
