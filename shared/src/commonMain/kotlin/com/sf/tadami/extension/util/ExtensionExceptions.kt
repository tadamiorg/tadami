package com.sf.tadami.extension.util

/** An error, not an exception, thrown by the code of an extension, see callGuarded. */
class ExtensionCodeException(cause: Throwable) : Exception(cause.toString(), cause)

/** The extension of the source [sourceName] crashed, and was unloaded, see callGuarded. */
class ExtensionDisabledException(val sourceName: String) : Exception("Disabled after a crash: $sourceName")
