package com.sf.tadami.extensions.model

enum class InstallStep {
    Idle, Pending, Downloading, Installing, Installed, Error;

    fun isCompleted(): Boolean {
        return this == Installed || this == Error || this == Idle
    }
}
