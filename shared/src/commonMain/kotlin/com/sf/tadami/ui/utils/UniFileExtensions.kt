package com.sf.tadami.ui.utils

import com.sf.tadami.storage.StorageFile

val StorageFile.extension: String?
    get() = name?.substringAfterLast('.')

val StorageFile.nameWithoutExtension: String?
    get() = name?.substringBeforeLast('.')

val StorageFile.displayablePath: String
    get() = filePath ?: uri.toString()