package com.sf.tadami.extension.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.core.content.getSystemService
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps an extension that crashes the app from crashing it at each launch: extensions can't be
 * uninstalled from the system settings anymore. Its state is kept in files of
 * `filesDir/extensions_guard`, written synchronously so that it survives the crash, and that the
 * crash screen's process writes without building the extension manager:
 * - `in_flight/<pkgName>`: an extension whose code is being loaded, with the pid of the process
 *   loading it. It is quarantined only when that process crashed meanwhile, see [onUncaughtException]
 *   and [checkPreviousExit]: not when it was swiped away, killed for memory or the load timed out.
 * - `quarantine/<pkgName>`: an extension not loaded anymore, until it is retried or replaced.
 * - `startup_crashes`: the uncaught crashes in a row before the startup completed, see
 *   [onActivityResumed]. The [SAFE_MODE_CRASHES]th one turns the safe mode on.
 * - `safe_mode`: the startup loads no extension, until the user loads them again.
 */
internal object ExtensionsGuard {

    private const val DIR_NAME = "extensions_guard"
    private const val IN_FLIGHT_DIR = "in_flight"
    private const val QUARANTINE_DIR = "quarantine"
    private const val STARTUP_CRASHES_FILE = "startup_crashes"
    private const val SAFE_MODE_FILE = "safe_mode"

    /** The uncaught crashes in a row before the startup completed that start the next launch in safe mode. */
    const val SAFE_MODE_CRASHES = 2

    /** The extensions this process is loading the code of. */
    private val inFlight: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var activityResumed = false

    @Volatile
    private var extensionsLoaded = false

    /** Whether this process completed its startup: the main screen resumed and the extensions loaded. */
    @Volatile
    private var startupCompleted = false

    fun dir(context: Context): File = File(context.filesDir, DIR_NAME)

    /** Records that the code of [pkgName] is being loaded, until [clearInFlight]. */
    fun markInFlight(context: Context, pkgName: String) {
        inFlight += pkgName
        markInFlight(dir(context), pkgName, Process.myPid())
    }

    fun clearInFlight(context: Context, pkgName: String) {
        inFlight -= pkgName
        clearInFlight(dir(context), pkgName)
    }

    fun isQuarantined(context: Context, pkgName: String): Boolean = isQuarantined(dir(context), pkgName)

    /** Lets [pkgName] be loaded again: retried by the user, or replaced by another APK. */
    fun clearQuarantine(context: Context, pkgName: String) {
        file(dir(context), QUARANTINE_DIR, pkgName).delete()
    }

    fun isSafeMode(context: Context): Boolean = isSafeMode(dir(context))

    fun setSafeMode(context: Context, enabled: Boolean) = setSafeMode(dir(context), enabled)

    /**
     * Called by the uncaught exception handler before the process dies: quarantines the extensions
     * being loaded, and counts the crash when it happened before the startup completed. Never throws.
     */
    fun onUncaughtException(context: Context) {
        runCatching { recordCrash(dir(context), inFlight.toSet(), startupCompleted) }
    }

    /**
     * Quarantines the extensions the previous processes were loading when they crashed in native
     * code or stopped responding, then clears the marks they left. Before Android 11 the exit
     * reasons are unknown: the marks are only cleared, [onUncaughtException] quarantined the
     * extensions of the crashes it caught.
     */
    fun checkPreviousExit(context: Context) {
        runCatching {
            val activityManager = context.getSystemService<ActivityManager>()
            checkLeftoverMarks(dir(context)) { pid, markedAt ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // The last exit of that pid, if it's the death of the process that left the mark
                    activityManager?.getHistoricalProcessExitReasons(context.packageName, pid, 1)
                        ?.firstOrNull { it.processName == context.packageName && it.timestamp >= markedAt }
                        ?.reason
                } else {
                    null
                }
            }
        }.onFailure { Log.d("ExtensionsGuard", it.stackTraceToString()) }
    }

    /** The main screen resumed: the startup completes once the extensions are loaded as well. */
    fun onActivityResumed(context: Context) {
        activityResumed = true
        completeStartupIfReady(context)
    }

    /** The extensions are loaded: the startup completes once the main screen resumed as well. */
    fun onExtensionsLoaded(context: Context) {
        extensionsLoaded = true
        completeStartupIfReady(context)
    }

    @Synchronized
    private fun completeStartupIfReady(context: Context) {
        if (startupCompleted || !activityResumed || !extensionsLoaded) return
        startupCompleted = true
        resetStartupCrashes(dir(context))
    }

    internal fun markInFlight(dir: File, pkgName: String, pid: Int) {
        // Not a reason to keep it from loading: it just couldn't be quarantined
        runCatching {
            val file = file(dir, IN_FLIGHT_DIR, pkgName)
            file.parentFile?.mkdirs()
            file.writeText(pid.toString())
        }.onFailure { Log.d("ExtensionsGuard", it.stackTraceToString()) }
    }

    internal fun clearInFlight(dir: File, pkgName: String) {
        file(dir, IN_FLIGHT_DIR, pkgName).delete()
    }

    internal fun isQuarantined(dir: File, pkgName: String): Boolean = file(dir, QUARANTINE_DIR, pkgName).exists()

    internal fun isSafeMode(dir: File): Boolean = File(dir, SAFE_MODE_FILE).exists()

    internal fun setSafeMode(dir: File, enabled: Boolean) {
        val file = File(dir, SAFE_MODE_FILE)
        if (enabled) {
            dir.mkdirs()
            file.createNewFile()
        } else {
            file.delete()
        }
    }

    internal fun resetStartupCrashes(dir: File) {
        File(dir, STARTUP_CRASHES_FILE).delete()
    }

    internal fun startupCrashes(dir: File): Int {
        return runCatching { File(dir, STARTUP_CRASHES_FILE).readText().trim().toInt() }.getOrDefault(0)
    }

    /**
     * Records an uncaught crash: quarantines the [inFlight] extensions, and counts it unless the
     * [startupCompleted]. The [SAFE_MODE_CRASHES]th one in a row turns the safe mode on.
     */
    internal fun recordCrash(dir: File, inFlight: Set<String>, startupCompleted: Boolean) {
        inFlight.forEach { pkgName ->
            quarantine(dir, pkgName)
            // Already dealt with, the next launch doesn't look at it
            clearInFlight(dir, pkgName)
        }
        if (startupCompleted) return
        val crashes = startupCrashes(dir) + 1
        dir.mkdirs()
        File(dir, STARTUP_CRASHES_FILE).writeText(crashes.toString())
        if (crashes >= SAFE_MODE_CRASHES) setSafeMode(dir, true)
    }

    /**
     * Quarantines the extensions of the marks left in [dir] whose process exited for a crash, see
     * [isCrashExit], then deletes all of them.
     *
     * @param exitReasonOf The exit reason of the process of a pid that died after a mark was left,
     * null when unknown.
     */
    internal fun checkLeftoverMarks(dir: File, exitReasonOf: (pid: Int, markedAt: Long) -> Int?) {
        File(dir, IN_FLIGHT_DIR).listFiles().orEmpty().forEach { mark ->
            val pid = runCatching { mark.readText().trim().toInt() }.getOrNull()
            val reason = pid?.let { exitReasonOf(it, mark.lastModified()) }
            if (isCrashExit(reason) && ExtensionsStorage.isValidPkgName(mark.name)) {
                Log.d("ExtensionsGuard", "${mark.name} was loading when the app crashed ($reason): quarantined")
                quarantine(dir, mark.name)
            }
            mark.delete()
        }
    }

    /**
     * Whether a process exit of [reason] is a crash of the extension it was loading. An uncaught
     * exception isn't one: [onUncaughtException] already dealt with it.
     */
    internal fun isCrashExit(reason: Int?): Boolean {
        return reason == ApplicationExitInfo.REASON_CRASH_NATIVE || reason == ApplicationExitInfo.REASON_ANR
    }

    private fun quarantine(dir: File, pkgName: String) {
        val file = file(dir, QUARANTINE_DIR, pkgName)
        file.parentFile?.mkdirs()
        file.createNewFile()
    }

    private fun file(dir: File, subDir: String, pkgName: String): File {
        require(ExtensionsStorage.isValidPkgName(pkgName)) { "Invalid package name $pkgName" }
        return File(File(dir, subDir), pkgName)
    }
}
