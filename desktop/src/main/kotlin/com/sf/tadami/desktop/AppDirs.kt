package com.sf.tadami.desktop

import java.io.File

/** Where the desktop app keeps its files: the OS's app data folder. */
object AppDirs {

    val data: File by lazy {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        val dir = when {
            os.contains("win") -> File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming", "Tadami")
            os.contains("mac") -> File(home, "Library/Application Support/Tadami")
            else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "tadami")
        }
        dir.apply { mkdirs() }
    }

    val cache: File by lazy { File(data, "cache").apply { mkdirs() } }

    val extensions: File by lazy { File(data, "extensions").apply { mkdirs() } }

    val preferences: File by lazy { File(data, "preferences").apply { mkdirs() } }
}
