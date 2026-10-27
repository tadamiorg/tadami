package com.sf.tadami.utils

/** Whether a network is available. */
expect fun isOnline(): Boolean

/** Opens [url] in the user's browser. */
expect fun openInBrowser(url: String)
