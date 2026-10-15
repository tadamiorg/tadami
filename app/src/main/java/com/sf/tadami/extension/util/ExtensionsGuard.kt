package com.sf.tadami.extension.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.core.content.getSystemService
import java.io.File
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Keeps an extension that crashes the app from crashing it at each launch: extensions can't be
 * uninstalled from the system settings anymore. Its state is kept in files of
 * `filesDir/extensions_guard`, written synchronously so that it survives the crash, and that the
 * crash screen's process writes without building the extension manager:
 * - `in_flight/<pkgName>`: an extension whose code is being loaded, with the pid of the process
 *   loading it. It becomes a suspect only when that process crashed meanwhile without the crash
 *   coming from the code of an extension, see [onUncaughtException] and [checkPreviousExit]: not
 *   when it was swiped away, killed for memory or the load timed out.
 * - `quarantine/<pkgName>`: an extension not loaded anymore, until it is retried or replaced. A
 *   crash quarantines the extensions it comes from, see [culpritsOf], or a suspect loading when
 *   the app crashed again.
 * - `suspects/<pkgName>`: an extension that was loading when the app crashed, the crash not
 *   telling which extension it comes from, a native crash for instance. The next startup loads it
 *   alone, after the others, see [isSuspect]: a repeat crash then comes from it. Cleared once it
 *   loaded without crashing the app.
 * - `startup_crashes`: the uncaught crashes in a row before the startup completed, see
 *   [onActivityResumed], but the ones that quarantined an extension. The [SAFE_MODE_CRASHES]th one
 *   turns the safe mode on.
 * - `safe_mode`: the startup loads no extension, until the user loads them again.
 */
internal object ExtensionsGuard {

    private const val DIR_NAME = "extensions_guard"
    private const val IN_FLIGHT_DIR = "in_flight"
    private const val QUARANTINE_DIR = "quarantine"
    private const val SUSPECTS_DIR = "suspects"
    private const val STARTUP_CRASHES_FILE = "startup_crashes"
    private const val SAFE_MODE_FILE = "safe_mode"

    /** The uncaught crashes in a row before the startup completed that start the next launch in safe mode. */
    const val SAFE_MODE_CRASHES = 2

    /** The extensions this process is loading the code of. */
    private val inFlight: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** The code of the extensions this process loaded, to find the ones a crash comes from. */
    private val loadedCode = ConcurrentLinkedQueue<ExtensionCode>()

    private class ExtensionCode(
        val pkgName: String,
        val name: String,
        val versionName: String,
        classLoader: ClassLoader,
    ) {
        // Not kept by this: the code of an extension replaced since then can be collected
        private val classLoader = WeakReference(classLoader)

        /** Whether [classLoader] defines each class already looked up, by name */
        private val definedClasses = ConcurrentHashMap<String, Boolean>()

        val isCollected: Boolean get() = classLoader.get() == null

        /**
         * Whether [className] is a class of this extension. Its loader is parent first: a class of
         * the app or of the platform is defined by another loader.
         */
        fun defines(className: String): Boolean {
            val loader = classLoader.get() ?: return false
            return definedClasses.getOrPut(className) {
                runCatching { Class.forName(className, false, loader).classLoader === loader }
                    .getOrDefault(false)
            }
        }
    }

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

    /** The code of [pkgName] loaded, or failed to, without crashing the app: not a suspect anymore. */
    fun clearInFlight(context: Context, pkgName: String) {
        inFlight -= pkgName
        clearInFlight(dir(context), pkgName)
        clearSuspect(dir(context), pkgName)
    }

    /**
     * Records that [classLoader] loads the code of [pkgName], named [name] in its version
     * [versionName], see [culpritsOf].
     */
    fun registerClassLoader(pkgName: String, name: String, versionName: String, classLoader: ClassLoader) {
        loadedCode.removeAll { it.isCollected }
        loadedCode += ExtensionCode(pkgName, name, versionName, classLoader)
    }

    /** The name of the extension [pkgName] whose code was loaded last, its package name if none was. */
    fun nameOf(pkgName: String): String = loadedCode.lastOrNull { it.pkgName == pkgName }?.name ?: pkgName

    /** The version name of the extension [pkgName] whose code was loaded last, empty if none was. */
    fun versionNameOf(pkgName: String): String = loadedCode.lastOrNull { it.pkgName == pkgName }?.versionName.orEmpty()

    /** Forgets the code of [pkgName], uninstalled. */
    fun forgetClassLoaders(pkgName: String) {
        loadedCode.removeAll { it.pkgName == pkgName }
    }

    /**
     * The extensions [throwable] comes from, see [attribute]. Empty when it comes from none of them,
     * or when they can't be told. Never throws.
     */
    fun culpritsOf(throwable: Throwable): Set<String> = runCatching {
        val code = loadedCode.toList()
        attribute(throwable, code.mapTo(mutableSetOf()) { it.pkgName }, inFlight.toSet()) { pkgName, className ->
            code.any { it.pkgName == pkgName && it.defines(className) }
        }
    }.getOrDefault(emptySet())

    fun isQuarantined(context: Context, pkgName: String): Boolean = isQuarantined(dir(context), pkgName)

    /** Whether [pkgName] is loaded alone, after the others, see `suspects` above. */
    fun isSuspect(context: Context, pkgName: String): Boolean = isSuspect(dir(context), pkgName)

    /** Lets [pkgName] be loaded again: retried by the user, or replaced by another APK. */
    fun clearQuarantine(context: Context, pkgName: String) {
        file(dir(context), QUARANTINE_DIR, pkgName).delete()
    }

    fun isSafeMode(context: Context): Boolean = isSafeMode(dir(context))

    fun setSafeMode(context: Context, enabled: Boolean) = setSafeMode(dir(context), enabled)

    /**
     * Called by the uncaught exception handler before the process dies: quarantines the [culprits],
     * the extensions the crash comes from, see [culpritsOf], or makes suspects of the ones being
     * loaded, see [recordCrash]. Counts the crash when it happened before the startup completed.
     * Never throws.
     */
    fun onUncaughtException(context: Context, culprits: Set<String>) {
        runCatching { recordCrash(dir(context), culprits, inFlight.toSet(), startupCompleted) }
    }

    /**
     * Quarantines the extensions [pkgNames] a crash the app survived comes from, see [culpritsOf]:
     * a thread other than the main one ended, the process goes on.
     */
    fun quarantine(context: Context, pkgNames: Set<String>) {
        pkgNames.forEach { quarantine(dir(context), it) }
    }

    /**
     * Makes suspects of the extensions the previous processes were loading when they crashed in
     * native code or stopped responding, see [checkLeftoverMarks], then clears the marks they left.
     * Before Android 11 the exit reasons are unknown: the marks are only cleared,
     * [onUncaughtException] dealt with the crashes it caught.
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

    internal fun isSuspect(dir: File, pkgName: String): Boolean = file(dir, SUSPECTS_DIR, pkgName).exists()

    internal fun clearSuspect(dir: File, pkgName: String) {
        file(dir, SUSPECTS_DIR, pkgName).delete()
    }

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
     * Records an uncaught crash: quarantines the [culprits], the extensions it comes from. When it
     * comes from none of them, the [inFlight] extensions become suspects, see [suspect]. Counts the
     * crash unless the [startupCompleted] or it quarantined an extension, its cause being dealt
     * with: the [SAFE_MODE_CRASHES]th one in a row turns the safe mode on.
     */
    internal fun recordCrash(dir: File, culprits: Set<String>, inFlight: Set<String>, startupCompleted: Boolean) {
        val quarantined = if (culprits.isNotEmpty()) {
            culprits.forEach { quarantine(dir, it) }
            true
        } else {
            // Each one, not only up to the first quarantined
            inFlight.map { suspect(dir, it) }.any { it }
        }
        // Already dealt with, the next launch doesn't look at them
        inFlight.forEach { clearInFlight(dir, it) }
        if (startupCompleted || quarantined) return
        val crashes = startupCrashes(dir) + 1
        dir.mkdirs()
        File(dir, STARTUP_CRASHES_FILE).writeText(crashes.toString())
        if (crashes >= SAFE_MODE_CRASHES) setSafeMode(dir, true)
    }

    /**
     * Makes suspects of the extensions of the marks left in [dir] whose process exited for a crash,
     * see [isCrashExit] and [suspect], then deletes all of them.
     *
     * @param exitReasonOf The exit reason of the process of a pid that died after a mark was left,
     * null when unknown.
     */
    internal fun checkLeftoverMarks(dir: File, exitReasonOf: (pid: Int, markedAt: Long) -> Int?) {
        File(dir, IN_FLIGHT_DIR).listFiles().orEmpty().forEach { mark ->
            val pid = runCatching { mark.readText().trim().toInt() }.getOrNull()
            val reason = pid?.let { exitReasonOf(it, mark.lastModified()) }
            if (isCrashExit(reason) && ExtensionsStorage.isValidPkgName(mark.name)) {
                Log.d("ExtensionsGuard", "${mark.name} was loading when the app crashed ($reason)")
                suspect(dir, mark.name)
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

    /**
     * The extensions [throwable] comes from: the ones whose code defines a class of its stack frames,
     * or of the frames of its causes and suppressed exceptions, see [stackClassNames]. Those whose
     * own classes, of their package, are there. Otherwise, the classes being of a library several of
     * them bundle, the [inFlight] ones among them, or all of them when none is in flight.
     *
     * @param pkgNames The extensions whose code was loaded.
     * @param defines Whether the code of the extension pkgName defines the class className.
     */
    internal fun attribute(
        throwable: Throwable,
        pkgNames: Set<String>,
        inFlight: Set<String>,
        defines: (pkgName: String, className: String) -> Boolean,
    ): Set<String> {
        val classNames = stackClassNames(throwable)
        val classesByPkgName = pkgNames
            .associateWith { pkgName -> classNames.filter { defines(pkgName, it) } }
            .filterValues { it.isNotEmpty() }
        val owners = classesByPkgName.filter { (pkgName, classes) -> classes.any { it.startsWith("$pkgName.") } }
        if (owners.isNotEmpty()) return owners.keys
        return classesByPkgName.keys.intersect(inFlight).ifEmpty { classesByPkgName.keys }
    }

    /** The classes of the stack frames of [throwable], of its causes and of its suppressed exceptions. */
    internal fun stackClassNames(throwable: Throwable): Set<String> {
        val classNames = LinkedHashSet<String>()
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        val pending = ArrayDeque(listOf(throwable))
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (!seen.add(current)) continue
            current.stackTrace.mapTo(classNames) { it.className }
            current.cause?.let { pending += it }
            pending += current.suppressed
        }
        return classNames
    }

    private fun quarantine(dir: File, pkgName: String) {
        val file = file(dir, QUARANTINE_DIR, pkgName)
        file.parentFile?.mkdirs()
        file.createNewFile()
        clearSuspect(dir, pkgName)
    }

    /**
     * Makes a suspect of [pkgName], loading when the app crashed. Quarantines it when it was one
     * already: loaded alone, the crash came from it.
     *
     * @return Whether it was quarantined.
     */
    private fun suspect(dir: File, pkgName: String): Boolean {
        if (isSuspect(dir, pkgName)) {
            quarantine(dir, pkgName)
            return true
        }
        val file = file(dir, SUSPECTS_DIR, pkgName)
        file.parentFile?.mkdirs()
        file.createNewFile()
        return false
    }

    private fun file(dir: File, subDir: String, pkgName: String): File {
        require(ExtensionsStorage.isValidPkgName(pkgName)) { "Invalid package name $pkgName" }
        return File(File(dir, subDir), pkgName)
    }
}
