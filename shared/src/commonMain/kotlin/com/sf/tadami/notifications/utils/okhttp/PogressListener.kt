package com.sf.tadami.notifications.utils.okhttp

interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}