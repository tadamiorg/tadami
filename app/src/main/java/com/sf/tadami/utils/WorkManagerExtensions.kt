package com.sf.tadami.utils

import androidx.work.WorkInfo
import androidx.work.WorkManager

fun WorkManager.isRunning(tag: String): Boolean {
    val list = this.getWorkInfosByTag(tag).get()
    return list.any { it.state == WorkInfo.State.RUNNING }
}