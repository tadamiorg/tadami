package com.sf.tadami.data.providers

import java.io.File

/** The default folder of the downloads and backups, until the user picks one. Registered in Injekt. */
interface FoldersProvider {

    fun directory(): File

    /** [directory] as the URI the storage preferences hold. */
    fun path(): String
}
