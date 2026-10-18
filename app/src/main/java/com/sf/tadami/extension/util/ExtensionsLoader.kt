package com.sf.tadami.extension.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.extension.model.LoadResult
import com.sf.tadami.preferences.extensions.TrustedExtensionsPreferences
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.Source
import com.sf.tadami.utils.Lang
import com.sf.tadami.utils.getPreferencesGroup
import dalvik.system.PathClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors

internal object ExtensionsLoader {

    private const val EXTENSION_FEATURE = "tadami.extension"
    private const val METADATA_SOURCE_CLASS = "tadami.extension.class"
    private const val EXTENSION_PACKAGE_PREFIX = "com.sf.tadami.extension."
    /** Stands for the lib version of an extension whose version name can't be parsed. */
    private val UNKNOWN_LIB_VERSION = LibVersion(0, 0)
    // 1.5: SAnime.status became an SAnimeStatus, older extensions call the removed setStatus(String)
    val API_VERSION_MIN = LibVersion(1, 5)
    // Bump with each extension API addition, and have the extensions using it declare that lib
    // version: older apps refuse lib versions above their own max instead of crashing on a missing
    // member. 1.6: SAnime.language (released 1.9.3 accepts up to 1.5). 1.7: the suspend source
    // methods, ConfigurableSource, ConfigurableAnimeHttpSource, awaitSuccess, PUT and DELETE.
    val API_VERSION_MAX = LibVersion(1, 7)

    /** SHA-256 fingerprint of the key the official extensions are signed with. */
    private const val OFFICIAL_SIGNATURE_HASH =
        "11dec208a209773efdc5348af5de709ca9af067e9154d1dd3c6abdb0fa3cc200"

    /** How long an extension gets to build its sources before it is listed as timed out. */
    private const val LOAD_TIMEOUT_MS = 5_000L

    /**
     * Runs the code of the extensions, which may block: a timed out one keeps its thread, here
     * rather than in a pool the rest of the app shares. Threads are created as needed, then reused.
     */
    private val loaderScope = CoroutineScope(
        SupervisorJob() + Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "ExtensionLoader").apply { isDaemon = true }
        }.asCoroutineDispatcher(),
    )

    // GET_SIGNATURES as well: getPackageArchiveInfo of API 28 and 29 only reads the certificates
    // with it, even for signingInfo
    @Suppress("DEPRECATION")
    private const val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
            PackageManager.GET_META_DATA or
            PackageManager.GET_SIGNATURES or
            PackageManager.GET_SIGNING_CERTIFICATES

    /**
     * Return a list of all the extensions of [ExtensionsStorage] initialized concurrently. The
     * suspects of a crash are loaded one at a time after the others, see [ExtensionsGuard.isSuspect].
     *
     * @param context The application context.
     * @param safeMode Whether none of their code is loaded, see [ExtensionLoadIssue.SAFE_MODE].
     * @param onLateResult Gets the result of an extension listed as [ExtensionLoadIssue.TIMED_OUT]
     * once it finishes loading.
     */
    fun loadExtensions(
        context: Context,
        safeMode: Boolean = false,
        onLateResult: (LoadResult) -> Unit = {},
    ): List<LoadResult> {
        val pkgNames = ExtensionsStorage.pkgNames(ExtensionsStorage.dir(context))
        if (pkgNames.isEmpty()) return emptyList()

        val trustedSignatures = trustedSignatures()
        val (suspects, others) = pkgNames.partition { ExtensionsGuard.isSuspect(context, it) }

        // Load each extension concurrently and wait for completion. Then each suspect alone: a
        // repeat crash comes from the one loading
        return loadInParallel(context, others, trustedSignatures, safeMode, onLateResult) +
            suspects.flatMap { loadInParallel(context, listOf(it), trustedSignatures, safeMode, onLateResult) }
    }

    /**
     * Loads the extension [pkgName] of [ExtensionsStorage] again: installed, updated, trusted or
     * retried. Listed as [ExtensionLoadIssue.TIMED_OUT] after [LOAD_TIMEOUT_MS], its result is then
     * given to [onLateResult] once it finishes loading, see [loadAll].
     */
    fun reloadExtension(context: Context, pkgName: String, onLateResult: (LoadResult) -> Unit): LoadResult {
        return loadInParallel(context, listOf(pkgName), trustedSignatures(), safeMode = false, onLateResult).single()
    }

    /**
     * Loads the extensions [pkgNames] of [ExtensionsStorage] in parallel, each listed as
     * [ExtensionLoadIssue.TIMED_OUT] after [LOAD_TIMEOUT_MS], see [loadAll].
     */
    private fun loadInParallel(
        context: Context,
        pkgNames: List<String>,
        trustedSignatures: Set<String>,
        safeMode: Boolean,
        onLateResult: (LoadResult) -> Unit,
    ): List<LoadResult> {
        if (pkgNames.isEmpty()) return emptyList()
        return loadAll(
            items = pkgNames,
            timeoutMillis = LOAD_TIMEOUT_MS,
            // One malformed package must not prevent the others from loading
            load = {
                runCatching { loadExtensionFromPkgName(context, it, trustedSignatures, safeMode).withOrigin(context, it) }
                    .getOrDefault(LoadResult.Error)
            },
            timedOut = { pkgName ->
                Log.d("LoadExtension", "Extension $pkgName timed out after ${LOAD_TIMEOUT_MS}ms")
                runCatching {
                    getExtensionPackageInfo(context, pkgName)
                        ?.let { unusableResult(context, it, ExtensionLoadIssue.TIMED_OUT).withOrigin(context, pkgName) }
                }.getOrNull() ?: LoadResult.Error
            },
            onLateResult = onLateResult,
        )
    }

    /** This result with where its extension [pkgName] was installed from, see [ExtensionsStorage.readOrigin]. */
    private fun LoadResult.withOrigin(context: Context, pkgName: String): LoadResult {
        if (this !is LoadResult.Success) return this
        val origin = ExtensionsStorage.readOrigin(ExtensionsStorage.dir(context), pkgName) ?: return this
        return copy(extension = extension.copy(repoUrl = origin.repoUrl, isManual = origin.manual))
    }

    /**
     * Runs [load] on each of [items] in parallel, each in its own thread of the loader pool, and
     * waits [timeoutMillis] at most for each. A load still running then gives [timedOut], and goes
     * on: its result is given to [onLateResult] once done. [load] must not throw.
     */
    internal fun <T, R : Any> loadAll(
        items: List<T>,
        timeoutMillis: Long,
        load: (T) -> R,
        timedOut: (T) -> R,
        onLateResult: (R) -> Unit,
    ): List<R> = runBlocking {
        items.map { item ->
            // Not a child of runBlocking, which would wait for it after its timeout
            val loading = loaderScope.async { load(item) }
            async {
                withTimeoutOrNull(timeoutMillis) { loading.await() } ?: run {
                    loaderScope.launch {
                        // Nothing to crash the app for: it stays listed as timed out
                        runCatching { onLateResult(loading.await()) }
                            .onFailure { Log.d("LoadExtension", it.stackTraceToString()) }
                    }
                    timedOut(item)
                }
            }
        }.awaitAll()
    }

    /**
     * Attempts to load the extension [pkgName] of [ExtensionsStorage]. It checks if the extension
     * contains the required feature flag before trying to load it.
     *
     * @param safeMode Whether none of its code is loaded, see [ExtensionLoadIssue.SAFE_MODE].
     */
    private fun loadExtensionFromPkgName(
        context: Context,
        pkgName: String,
        trustedSignatures: Set<String>,
        safeMode: Boolean,
    ): LoadResult {
        val pkgInfo = getExtensionPackageInfo(context, pkgName)
        if (pkgInfo == null) {
            Log.d("LoadExtensionFromPkgName","Extension package is not found ($pkgName)")
            return LoadResult.Error
        }
        return loadExtension(context, pkgInfo, trustedSignatures, safeMode)
    }

    /** The package of the extension [pkgName] of [ExtensionsStorage], null when it has none. */
    private fun getExtensionPackageInfo(context: Context, pkgName: String): PackageInfo? {
        val file = ExtensionsStorage.apkFile(ExtensionsStorage.dir(context), pkgName)
        if (!file.isFile) return null
        return getArchivePackageInfo(context, file)
            ?.takeIf { it.packageName == pkgName && isPackageAnExtension(it) }
    }

    /** The package of the APK [file], its resources readable, or null when it isn't a valid one. */
    fun getArchivePackageInfo(context: Context, file: File): PackageInfo? {
        val pkgManager = context.packageManager
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getPackageArchiveInfo(file.path, PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getPackageArchiveInfo(file.path, PACKAGE_FLAGS)
        }
        return pkgInfo?.apply { applicationInfo?.fixBasePaths(file.path) }
    }

    /**
     * Loads an extension
     *
     * @param context The application context.
     * @param pkgInfo The package of the extension to load.
     * @param trustedSignatures The signing keys the user trusted, see [isTrusted].
     * @param safeMode Whether none of its code is loaded, see [ExtensionLoadIssue.SAFE_MODE].
     */
    private fun loadExtension(
        context: Context,
        pkgInfo: PackageInfo,
        trustedSignatures: Set<String>,
        safeMode: Boolean,
    ): LoadResult {
        return try {
            loadExtensionUnsafe(context, pkgInfo, trustedSignatures, safeMode)
        } catch (e: Throwable) {
            Log.d("LoadExtension","Extension load error: ${pkgInfo.packageName}")
            Log.d("LoadExtension Error",e.stackTraceToString())
            LoadResult.Error
        }
    }

    private fun loadExtensionUnsafe(
        context: Context,
        pkgInfo: PackageInfo,
        trustedSignatures: Set<String>,
        safeMode: Boolean,
    ): LoadResult {
        val pkgManager = context.packageManager
        val appInfo = pkgInfo.applicationInfo ?: return LoadResult.Error
        val pkgName = pkgInfo.packageName

        val extName = pkgManager.getApplicationLabel(appInfo).toString().substringAfter("Tadami: ")
        val versionName = pkgInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
        val apiVersion = versionName?.let { LibVersion.fromVersionName(it) }
        val signatureHashes = signatureHashes(pkgInfo)

        fun unusable(issue: ExtensionLoadIssue) = unusableResult(context, pkgInfo, issue)

        // Before anything of the package is loaded: the code of an untrusted one must never run
        if (!isTrusted(signatureHashes, trustedSignatures)) {
            return unusable(ExtensionLoadIssue.UNTRUSTED)
        }

        libVersionIssue(apiVersion)?.let {
            Log.d("LoadExtension", "Lib version is $apiVersion, while only versions " +
                    "$API_VERSION_MIN to $API_VERSION_MAX are allowed")
            return unusable(it)
        }

        // Neither runs any of its code
        if (ExtensionsGuard.isQuarantined(context, pkgName)) {
            return unusable(ExtensionLoadIssue.QUARANTINED)
        }
        if (safeMode) {
            return unusable(ExtensionLoadIssue.SAFE_MODE)
        }

        val sourceClasses = appInfo.metaData?.getString(METADATA_SOURCE_CLASS)
        if (sourceClasses.isNullOrBlank()) {
            Log.d("LoadExtension","Missing $METADATA_SOURCE_CLASS meta-data for extension $extName")
            return unusable(ExtensionLoadIssue.LOAD_FAILED)
        }

        // Its code runs until its sources are built: a suspect if the app crashes meanwhile
        ExtensionsGuard.markInFlight(context, pkgName)
        val sources = try {
            val classLoader = try {
                // Parent first: the classes of the app win over any of the extension
                PathClassLoader(appInfo.sourceDir, null, context.classLoader)
            } catch (e: Exception) {
                Log.d("ClassLoader","Extension load error: $extName ($pkgName)")
                Log.d("ClassLoader",e.stackTraceToString())
                return unusable(ExtensionLoadIssue.LOAD_FAILED)
            }
            // Before any of its code runs: a crash coming from it, even later, quarantines it
            ExtensionsGuard.registerClassLoader(pkgName, extName, versionName.orEmpty(), classLoader)

            sourceClasses
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
                        when (val obj = Class.forName(it, false, classLoader).getDeclaredConstructor().newInstance()) {
                            is Source -> listOf(obj)
                            else -> throw Exception("Unknown source class type: ${obj.javaClass}")
                        }
                    } catch (e: Throwable) {
                        Log.d("LoadExtension","Extension load error: $extName ($it)")
                        Log.d("LoadExtension Error",e.stackTraceToString())
                        return unusable(ExtensionLoadIssue.LOAD_FAILED)
                    }
                }
        } finally {
            // Built or failed, without crashing the app
            ExtensionsGuard.clearInFlight(context, pkgName)
        }

        val langs = sources.filterIsInstance<AnimeCatalogueSource>()
            .map { it.lang }
            .toSet()

        val extension = Extension.Installed(
            name = extName,
            pkgName = pkgName,
            versionName = versionName.orEmpty(),
            versionCode = versionCode,
            apiVersion = apiVersion ?: UNKNOWN_LIB_VERSION,
            lang = langs.firstOrNull() ?: Lang.UNKNOWN,
            sources = sources,
            pkgFactory = null,
            icon = appInfo.loadIcon(pkgManager),
            signatureHash = signatureHashes.lastOrNull(),
        )
        return LoadResult.Success(extension)
    }

    /** Still listed, without its sources, so that the user sees why it can't be used */
    private fun unusableResult(
        context: Context,
        pkgInfo: PackageInfo,
        issue: ExtensionLoadIssue,
    ): LoadResult {
        val pkgManager = context.packageManager
        val appInfo = pkgInfo.applicationInfo ?: return LoadResult.Error
        val pkgName = pkgInfo.packageName
        val extName = pkgManager.getApplicationLabel(appInfo).toString().substringAfter("Tadami: ")
        val versionName = pkgInfo.versionName

        Log.d("LoadExtension", "Extension $extName ($pkgName $versionName) can't be used: $issue")
        return LoadResult.Success(
            Extension.Installed(
                name = extName,
                pkgName = pkgName,
                versionName = versionName.orEmpty(),
                versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo),
                apiVersion = versionName?.let { LibVersion.fromVersionName(it) } ?: UNKNOWN_LIB_VERSION,
                lang = langFromPackageName(pkgName),
                sources = emptyList(),
                pkgFactory = null,
                icon = appInfo.loadIcon(pkgManager),
                loadIssue = issue,
                signatureHash = signatureHashes(pkgInfo).lastOrNull(),
            ),
        )
    }

    private fun trustedSignatures(): Set<String> = runBlocking {
        Injekt.get<DataStore<Preferences>>()
            .getPreferencesGroup(TrustedExtensionsPreferences)
            .trustedSignatures
    }

    /**
     * SHA-256 fingerprints of the certificates [pkgInfo] is signed with, the current one last.
     * With a rotated key, the older ones the platform verified come first.
     */
    internal fun signatureHashes(pkgInfo: PackageInfo): List<String> {
        // signingInfo is there from API 28, the minSdk: no need for the legacy signatures
        val signingInfo = pkgInfo.signingInfo ?: return emptyList()
        val signatures = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
        return signatures.orEmpty().map { sha256(it.toByteArray()) }
    }

    /** Lowercase hex SHA-256 of [bytes]. */
    internal fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    /**
     * Whether an extension signed with [signatureHashes] can be loaded: signed with the official
     * key, or with one of the [trustedSignatures] the user trusted.
     */
    internal fun isTrusted(signatureHashes: List<String>, trustedSignatures: Set<String>): Boolean {
        return signatureHashes.any { it == OFFICIAL_SIGNATURE_HASH || it in trustedSignatures }
    }

    /** Why an extension built against [libVersion] can't be loaded, or null when it can. */
    internal fun libVersionIssue(libVersion: LibVersion?): ExtensionLoadIssue? = when {
        libVersion == null -> ExtensionLoadIssue.INVALID_VERSION
        libVersion < API_VERSION_MIN -> ExtensionLoadIssue.LIB_TOO_OLD
        libVersion > API_VERSION_MAX -> ExtensionLoadIssue.LIB_TOO_NEW
        else -> null
    }

    /** The language part of an extension package name: `com.sf.tadami.extension.<lang>.<name>`. */
    internal fun langFromPackageName(pkgName: String): Lang {
        return Lang.fromCode(pkgName.substringAfter(EXTENSION_PACKAGE_PREFIX, "").substringBefore('.'))
    }

    /**
     * Returns true if the given package is an extension.
     *
     * @param pkgInfo The package info of the application.
     */
    internal fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
        return pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }
    }

    /**
     * On Android 13+ the ApplicationInfo generated by getPackageArchiveInfo doesn't
     * have sourceDir which breaks assets loading (used for getting icon here).
     */
    private fun ApplicationInfo.fixBasePaths(apkPath: String) {
        if (sourceDir == null) {
            sourceDir = apkPath
        }
        if (publicSourceDir == null) {
            publicSourceDir = apkPath
        }
    }
}