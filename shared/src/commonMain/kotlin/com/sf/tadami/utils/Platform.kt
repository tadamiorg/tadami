package com.sf.tadami.utils

/** Whether a network is available. */
expect fun isOnline(): Boolean

/** Opens [url] in the user's browser. */
expect fun openInBrowser(url: String)

/** How long ago or in how long [epochMillis] is from [now], as "3 hours ago": DateUtils on Android. */
expect fun relativeTimeSpan(epochMillis: Long, now: Long): String

/** [bytes] as "1.23 MB": Formatter.formatFileSize on Android. */
expect fun formatFileSize(bytes: Long): String

/** [bytes] as "1.2 MB", rounded more than [formatFileSize]. */
expect fun formatShortFileSize(bytes: Long): String
