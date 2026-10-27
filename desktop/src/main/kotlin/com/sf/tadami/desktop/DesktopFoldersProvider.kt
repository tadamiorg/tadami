package com.sf.tadami.desktop

import com.sf.tadami.data.providers.FoldersProvider
import java.io.File

/** The downloads and backups go to a Tadami folder of the user's, until the user picks one. */
class DesktopFoldersProvider : FoldersProvider {

    // Made at once: the URI of a folder ends with a slash only once it exists
    override fun directory(): File = File(System.getProperty("user.home"), "Tadami").apply { mkdirs() }

    override fun path(): String = directory().toURI().toString()
}
