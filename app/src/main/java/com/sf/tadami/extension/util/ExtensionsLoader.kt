package com.sf.tadami.extension.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.LoadResult
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.Source
import com.sf.tadami.utils.Lang
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.logcat
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.Executors

/**
 * Turns the APK files in [ExtensionsStorage] into live [Source] instances.
 *
 * Extensions used to be installed Android packages that this object discovered through
 * [PackageManager.getInstalledPackages]. They are no longer installed at all: the app downloads the
 * same signed APK into its own private directory and class-loads it from there, which keeps every
 * capability while removing the OS install prompt — and with it any developer-verification
 * requirement — from the picture.
 *
 * Almost nothing about *running* an extension changed. Its code has always executed inside this
 * process under [PathClassLoader], never in a process of its own; installation only ever delivered
 * the file, made it discoverable, and checked its signature. This object now does the last two
 * itself, the signature part via [ExtensionSignatures].
 */
internal object ExtensionsLoader {

    private const val EXTENSION_FEATURE = "tadami.extension"
    private const val METADATA_SOURCE_CLASS = "tadami.extension.class"
    const val API_VERSION_MIN_OBSOLETE = 1.3
    const val API_VERSION_MIN = 1.3
    const val API_VERSION_MAX = 1.5

    /** How long a single extension gets to construct its sources before we give up on it. */
    private const val LOAD_TIMEOUT_MS = 5_000L

    /** Explicit tag: several log sites here sit inside coroutine lambdas. */
    private const val TAG = "ExtensionsLoader"

    /**
     * Extension constructors are third-party code and are allowed to block (several currently do a
     * `runBlocking` preference migration, one also hits the network). `withTimeoutOrNull` can only
     * cancel at a suspension point, so a blocked constructor keeps its thread even after we stop
     * waiting for it. Those abandoned threads live here rather than on [Dispatchers.IO] so they
     * can never starve the rest of the app; the pool is unbounded and reuses idle threads.
     */
    private val instantiationDispatcher =
        Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "extension-init").apply { isDaemon = true }
        }.asCoroutineDispatcher()

    /**
     * Owns the instantiation coroutines. They deliberately do *not* run as children of the caller:
     * a child would keep the caller's scope alive until it finished, which is exactly what we're
     * trying to avoid. Being external, a timed-out load is simply abandoned. [SupervisorJob] keeps
     * one failing extension from cancelling the others.
     */
    private val instantiationScope =
        CoroutineScope(SupervisorJob() + instantiationDispatcher)

    @Suppress("DEPRECATION")
    private val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
            PackageManager.GET_META_DATA or
            PackageManager.GET_SIGNATURES or
            PackageManager.GET_SIGNING_CERTIFICATES

    /**
     * Returns a result for every extension file on disk, loading them concurrently off the
     * caller's thread. Never throws: a file that is malformed, incompatible, untrusted, slow or
     * previously fatal comes back as [LoadResult.Error] so that one bad extension can't stop the
     * app from starting.
     *
     * @param context The application context.
     */
    suspend fun loadExtensions(context: Context): List<LoadResult> = withContext(Dispatchers.IO) {
        // Downloads that died before being committed are worthless; drop them before we look.
        ExtensionsStorage.cleanTmp(context)

        val files = ExtensionsStorage.installed(context)
        if (files.isEmpty()) return@withContext emptyList()

        val trusted = trustedSignatures()

        // Snapshot before the pass starts, otherwise the marks we're about to write would read
        // back as quarantines.
        val quarantined = ExtensionLoadGuard.quarantined(context)

        val deferred = files.map { file ->
            async {
                val pkgName = ExtensionsStorage.pkgNameOf(file)
                if (pkgName in quarantined) {
                    logcat(LogPriority.WARN, tag = TAG) {
                        "Skipping $pkgName: it hung or crashed the app during the previous launch"
                    }
                    LoadResult.Error(pkgName = pkgName, reason = LoadResult.Reason.QUARANTINED)
                } else {
                    loadExtensionGuarded(context, file, trusted)
                }
            }
        }
        deferred.awaitAll()
    }

    /**
     * Loads a single extension by package name, for one that was just downloaded or a retry of one
     * that previously failed.
     */
    suspend fun loadExtensionFromPkgName(context: Context, pkgName: String): LoadResult {
        val file = ExtensionsStorage.apkFile(context, pkgName)
        if (!file.exists()) {
            logcat(LogPriority.WARN) { "Extension file is not found ($pkgName)" }
            return LoadResult.Error(pkgName = pkgName, reason = LoadResult.Reason.NOT_FOUND)
        }
        // An extension we're installing right now gets a clean slate: the user explicitly asked
        // for it, so don't hold a previous bad load against it.
        ExtensionLoadGuard.clearQuarantine(context, pkgName)
        return loadExtensionGuarded(context, file, trustedSignatures())
    }

    /**
     * Package metadata read straight out of an APK file, without installing it.
     *
     * Android leaves `sourceDir`/`publicSourceDir` null on an archive-derived
     * [android.content.pm.ApplicationInfo], and everything that resolves the extension's own
     * resources — `loadIcon`, `getResourcesForApplication`, `getApplicationLabel` — fails silently
     * without them, so they are filled in here rather than at each call site.
     */
    fun getArchivePackageInfo(context: Context, file: File): PackageInfo? {
        val pkgInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    file.path,
                    PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(file.path, PACKAGE_FLAGS)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to read package info for ${file.name}" }
            return null
        } ?: return null

        pkgInfo.applicationInfo?.apply {
            sourceDir = file.path
            publicSourceDir = file.path
        }
        return pkgInfo
    }

    fun getExtensionPackageInfoFromPkgName(context: Context, pkgName: String): PackageInfo? {
        val file = ExtensionsStorage.apkFile(context, pkgName).takeIf { it.exists() } ?: return null
        return getArchivePackageInfo(context, file)?.takeIf { declaresExtensionFeature(it) }
    }

    /**
     * Runs [loadExtension] behind the crash barrier and the load timeout, so that neither a
     * hanging nor a throwing constructor escapes into the caller.
     */
    private suspend fun loadExtensionGuarded(
        context: Context,
        file: File,
        trusted: Set<String>,
    ): LoadResult {
        val pkgName = ExtensionsStorage.pkgNameOf(file)
        ExtensionLoadGuard.markInFlight(context, pkgName)

        val deferred = instantiationScope.async { loadExtension(context, file, trusted) }
        val result = try {
            // Only `await` is under the timeout — it's the one suspension point we can actually
            // cancel. The extension's own code is not interruptible and is left to finish (or not)
            // on its abandoned thread.
            withTimeoutOrNull(LOAD_TIMEOUT_MS) { deferred.await() }
        } catch (e: CancellationException) {
            // The caller went away, not an extension failure — don't record it as one.
            deferred.cancel()
            ExtensionLoadGuard.clearInFlight(context, pkgName)
            throw e
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Extension load error: $pkgName" }
            LoadResult.Error(
                pkgName = pkgName,
                reason = LoadResult.Reason.CLASSLOAD_FAILED,
                throwable = e,
            )
        }

        if (result == null) {
            deferred.cancel()
            // The constructor is still running on an abandoned thread; we're not waiting for it.
            // The in-flight mark stays so the next launch skips this extension entirely.
            logcat(LogPriority.ERROR) { "Extension $pkgName timed out after ${LOAD_TIMEOUT_MS}ms" }
            return LoadResult.Error(pkgName = pkgName, reason = LoadResult.Reason.TIMED_OUT)
        }

        ExtensionLoadGuard.clearInFlight(context, pkgName)
        return result
    }

    /**
     * Loads an extension from its APK file.
     *
     * @param context The application context.
     * @param file The extension APK. It must already be read-only — see [ExtensionsStorage].
     * @param trusted Signing certificates the user has accepted, on top of the official one.
     */
    private fun loadExtension(context: Context, file: File, trusted: Set<String>): LoadResult {
        val pkgManager = context.packageManager
        val fileName = ExtensionsStorage.pkgNameOf(file)

        fun error(
            reason: LoadResult.Reason,
            throwable: Throwable? = null,
            extName: String? = null,
            fingerprint: String? = null,
        ) = LoadResult.Error(fileName, reason, extName, throwable, fingerprint)

        val pkgInfo = getArchivePackageInfo(context, file)
            ?: return error(LoadResult.Reason.MALFORMED)
                .also { logcat(LogPriority.WARN) { "Unreadable extension archive: ${file.name}" } }

        if (!declaresExtensionFeature(pkgInfo)) {
            logcat(LogPriority.WARN) { "${file.name} doesn't declare the $EXTENSION_FEATURE feature" }
            return error(LoadResult.Reason.NOT_FOUND)
        }

        // The file name comes from the repo index, the package name from the APK itself. A
        // mismatch means the index points at something other than what it claims, so refuse it
        // rather than quietly loading whatever turned up.
        if (pkgInfo.packageName != fileName) {
            logcat(LogPriority.WARN) {
                "Extension package name ${pkgInfo.packageName} doesn't match file $fileName"
            }
            return error(LoadResult.Reason.MALFORMED)
        }

        val appInfo = pkgInfo.applicationInfo
            ?: return error(LoadResult.Reason.MALFORMED)
                .also { logcat(LogPriority.WARN) { "Missing applicationInfo for $fileName" } }

        val extName = runCatching { pkgManager.getApplicationLabel(appInfo).toString() }
            .getOrNull()
            ?.substringAfter("Tadami: ")
            ?: fileName

        // Nothing below this point should run for an extension we don't trust, so this sits ahead
        // of every use of the class loader.
        val fingerprint = ExtensionSignatures.fingerprintOf(pkgInfo)
        if (!ExtensionSignatures.isTrusted(fingerprint, trusted)) {
            logcat(LogPriority.WARN) {
                "Extension $fileName is signed with an untrusted certificate ($fingerprint)"
            }
            return error(LoadResult.Reason.UNTRUSTED, extName = extName, fingerprint = fingerprint)
        }

        val versionName = pkgInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

        if (versionName.isNullOrEmpty()) {
            logcat(LogPriority.WARN) { "Missing versionName for extension $extName" }
            return error(LoadResult.Reason.MALFORMED, extName = extName)
        }

        // Validate lib version
        val apiVersion = versionName.substringBeforeLast('.').toDoubleOrNull()
        if (apiVersion == null || apiVersion < API_VERSION_MIN || apiVersion > API_VERSION_MAX) {
            logcat(LogPriority.WARN) {
                "Lib version is $apiVersion, while only versions " +
                        "$API_VERSION_MIN to $API_VERSION_MAX are allowed"
            }
            return error(LoadResult.Reason.INCOMPATIBLE_API, extName = extName)
        }

        val classLoader = try {
            // Unchanged from when this file was an installed package: same parent, so extensions
            // keep resolving okhttp/jsoup/rxjava3/injekt and com.sf.tadami.** out of the host.
            PathClassLoader(file.path, null, context.classLoader)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Extension load error: $extName ($fileName)" }
            return error(LoadResult.Reason.CLASSLOAD_FAILED, e, extName)
        }

        val sourceClasses = appInfo.metaData?.getString(METADATA_SOURCE_CLASS)
        if (sourceClasses.isNullOrBlank()) {
            logcat(LogPriority.WARN) { "Missing $METADATA_SOURCE_CLASS metadata for $fileName" }
            return error(LoadResult.Reason.MALFORMED, extName = extName)
        }

        val sources = sourceClasses
            .split(";")
            .map {
                val sourceClass = it.trim()
                if (sourceClass.startsWith(".")) {
                    pkgInfo.packageName + sourceClass
                } else {
                    sourceClass
                }
            }
            .flatMap {
                try {
                    when (
                        val obj = Class.forName(it, false, classLoader)
                            .getDeclaredConstructor()
                            .newInstance()
                    ) {
                        is Source -> listOf(obj)
                        else -> throw Exception("Unknown source class type: ${obj.javaClass}")
                    }
                } catch (e: Throwable) {
                    logcat(LogPriority.ERROR, e) { "Extension load error: $extName ($it)" }
                    return error(LoadResult.Reason.CLASSLOAD_FAILED, e, extName)
                }
            }

        val langs = sources.filterIsInstance<AnimeCatalogueSource>()
            .map { it.lang }
            .toSet()

        val extension = Extension.Installed(
            name = extName,
            pkgName = pkgInfo.packageName,
            versionName = versionName,
            versionCode = versionCode,
            apiVersion = apiVersion,
            lang = langs.firstOrNull() ?: Lang.UNKNOWN,
            sources = sources,
            pkgFactory = null,
            icon = runCatching { appInfo.loadIcon(pkgManager) }.getOrNull(),
        )
        return LoadResult.Success(extension)
    }

    /**
     * Returns true if the given package is an extension.
     *
     * @param pkgInfo The package info of the application.
     */
    fun declaresExtensionFeature(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }

    private suspend fun trustedSignatures(): Set<String> = runCatching {
        Injekt.get<DataStore<Preferences>>()
            .getPreferencesGroup(SourcesPreferences)
            .trustedSignatures
    }.getOrElse {
        logcat(LogPriority.ERROR, it) { "Failed to read trusted signatures" }
        emptySet()
    }
}
