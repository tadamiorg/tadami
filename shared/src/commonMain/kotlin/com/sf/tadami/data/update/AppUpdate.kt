package com.sf.tadami.data.update

sealed class AppUpdate {
    class NewUpdate(val release: GithubUpdate) : AppUpdate()
    object NoNewUpdate : AppUpdate()
    // The latest release could not be fetched or read
    object CheckFailed : AppUpdate()
}