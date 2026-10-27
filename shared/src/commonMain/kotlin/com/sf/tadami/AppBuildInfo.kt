package com.sf.tadami

/**
 * What the build of the running app is, the Android BuildConfig fields: each app sets them when it
 * starts, before anything reads them.
 */
@Suppress("PropertyName")
object AppBuildInfo {
    var APPLICATION_ID: String = "com.sf.tadami"
    var VERSION_NAME: String = ""
    var VERSION_CODE: Int = 0
    var RELEASE_VERSION_NAME: String = ""
    var BUILD_DATE: String = ""
    var DEBUG: Boolean = false
    var UPDATER_ENABLED: Boolean = false
}
