package com.sf.tadami.extension.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.LoadResult
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.Source
import com.sf.tadami.utils.Lang
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
import java.util.concurrent.Executors

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
     * Returns a result for every installed extension package, loading them concurrently off the
     * caller's thread. Never throws: a package that is malformed, incompatible, slow or previously
     * fatal comes back as [LoadResult.Error] so that one bad extension can't stop the app from
     * starting.
     *
     * @param context The application context.
     */
    suspend fun loadExtensions(context: Context): List<LoadResult> = withContext(Dispatchers.IO) {
        val pkgManager = context.packageManager

        val installedPkgs = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pkgManager.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()),
                )
            } else {
                pkgManager.getInstalledPackages(PACKAGE_FLAGS)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e, TAG) { "Failed to enumerate installed packages" }
            return@withContext emptyList()
        }

        val extPkgs = installedPkgs
            .asSequence()
            .filter { isPackageAnExtension(it) }
            .map { ExtensionInfo(packageInfo = it, isShared = true) }
            // Remove duplicates. Shared takes priority than private by default
            .distinctBy { it.packageInfo.packageName }
            .toList()

        if (extPkgs.isEmpty()) return@withContext emptyList()

        // Snapshot before the pass starts, otherwise the marks we're about to write would read
        // back as quarantines.
        val quarantined = ExtensionLoadGuard.quarantined(context)

        val deferred = extPkgs.map { info ->
            async {
                val pkgName = info.packageInfo.packageName
                if (pkgName in quarantined) {
                    logcat(LogPriority.WARN, tag = TAG) {
                        "Skipping $pkgName: it hung or crashed the app during the previous launch"
                    }
                    LoadResult.Error(pkgName = pkgName, reason = LoadResult.Reason.QUARANTINED)
                } else {
                    loadExtensionGuarded(context, info)
                }
            }
        }
        deferred.awaitAll()
    }

    /**
     * Attempts to load an extension from the given package name. It checks if the extension
     * contains the required feature flag before trying to load it.
     */
    suspend fun loadExtensionFromPkgName(context: Context, pkgName: String): LoadResult {
        val extensionPackage = getExtensionInfoFromPkgName(context, pkgName)
        if (extensionPackage == null) {
            logcat(LogPriority.WARN) { "Extension package is not found ($pkgName)" }
            return LoadResult.Error(pkgName = pkgName, reason = LoadResult.Reason.NOT_FOUND)
        }
        // An extension we're installing right now gets a clean slate: the user explicitly asked
        // for it, so don't hold a previous bad load against it.
        ExtensionLoadGuard.clearQuarantine(context, pkgName)
        return loadExtensionGuarded(context, extensionPackage)
    }

    fun getExtensionPackageInfoFromPkgName(context: Context, pkgName: String): PackageInfo? {
        return getExtensionInfoFromPkgName(context, pkgName)?.packageInfo
    }

    private fun getExtensionInfoFromPkgName(context: Context, pkgName: String): ExtensionInfo? {
        return try {
            context.packageManager.getPackageInfo(pkgName, PACKAGE_FLAGS)
                .takeIf { isPackageAnExtension(it) }
                ?.let { ExtensionInfo(packageInfo = it, isShared = true) }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to read package info for $pkgName" }
            null
        }
    }

    /**
     * Runs [loadExtension] behind the crash barrier and the load timeout, so that neither a
     * hanging nor a throwing constructor escapes into the caller.
     */
    private suspend fun loadExtensionGuarded(
        context: Context,
        extensionInfo: ExtensionInfo,
    ): LoadResult {
        val pkgName = extensionInfo.packageInfo.packageName
        ExtensionLoadGuard.markInFlight(context, pkgName)

        val deferred = instantiationScope.async { loadExtension(context, extensionInfo) }
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
     * Loads an extension
     *
     * @param context The application context.
     * @param extensionInfo The extension to load.
     */
    private fun loadExtension(context: Context, extensionInfo: ExtensionInfo): LoadResult {
        val pkgManager = context.packageManager
        val pkgInfo = extensionInfo.packageInfo
        val pkgName = pkgInfo.packageName

        fun error(reason: LoadResult.Reason, throwable: Throwable? = null, extName: String? = null) =
            LoadResult.Error(pkgName, reason, extName, throwable)

        val appInfo = pkgInfo.applicationInfo
            ?: return error(LoadResult.Reason.MALFORMED)
                .also { logcat(LogPriority.WARN) { "Missing applicationInfo for $pkgName" } }

        val extName = runCatching { pkgManager.getApplicationLabel(appInfo).toString() }
            .getOrNull()
            ?.substringAfter("Tadami: ")
            ?: pkgName
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
            PathClassLoader(appInfo.sourceDir, null, context.classLoader)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Extension load error: $extName ($pkgName)" }
            return error(LoadResult.Reason.CLASSLOAD_FAILED, e, extName)
        }

        val sourceClasses = appInfo.metaData?.getString(METADATA_SOURCE_CLASS)
        if (sourceClasses.isNullOrBlank()) {
            logcat(LogPriority.WARN) { "Missing $METADATA_SOURCE_CLASS metadata for $pkgName" }
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
            pkgName = pkgName,
            versionName = versionName,
            versionCode = versionCode,
            apiVersion = apiVersion,
            lang = langs.firstOrNull() ?: Lang.UNKNOWN,
            sources = sources,
            pkgFactory = null,
            icon = runCatching { appInfo.loadIcon(pkgManager) }.getOrNull(),
            isShared = extensionInfo.isShared,
        )
        return LoadResult.Success(extension)
    }

    /**
     * Returns true if the given package is an extension.
     *
     * @param pkgInfo The package info of the application.
     */
    private fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }

    private data class ExtensionInfo(
        val packageInfo: PackageInfo,
        val isShared: Boolean,
    )
}
