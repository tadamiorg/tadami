package com.sf.tadami.extension.model

enum class InstallStep {
    Idle, Pending, Downloading, Installing, Installed, Error,

    /** An installed extension being loaded again, once trusted for instance: it can't be cancelled. */
    Loading;

    fun isCompleted(): Boolean {
        return this == Installed || this == Error || this == Idle
    }
}
