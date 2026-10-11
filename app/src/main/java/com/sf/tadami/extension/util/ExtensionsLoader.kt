package com.sf.tadami.extension.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.extension.model.LoadResult
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.Source
import com.sf.tadami.utils.Lang
import dalvik.system.PathClassLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

internal object ExtensionsLoader {

    private const val EXTENSION_FEATURE = "tadami.extension"
    private const val METADATA_SOURCE_CLASS = "tadami.extension.class"
    private const val EXTENSION_PACKAGE_PREFIX = "com.sf.tadami.extension."
    /** Stands for the lib version of an extension whose version name can't be parsed. */
    private val UNKNOWN_LIB_VERSION = LibVersion(0, 0)
    val API_VERSION_MIN = LibVersion(1, 3)
    // Bump with each extension API addition, and have the extensions using it declare that lib
    // version: older apps refuse lib versions above their own max instead of crashing on a missing
    // member. 1.6: SAnime.language (released 1.9.3 accepts up to 1.5).
    val API_VERSION_MAX = LibVersion(1, 6)

    @Suppress("DEPRECATION")
    private val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
            PackageManager.GET_META_DATA or
            PackageManager.GET_SIGNATURES or
            PackageManager.GET_SIGNING_CERTIFICATES

    /**
     * Return a list of all the available extensions initialized concurrently.
     *
     * @param context The application context.
     */
    fun loadExtensions(context: Context): List<LoadResult> {
        val pkgManager = context.packageManager

        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }

        val sharedExtPkgs = installedPkgs
            .asSequence()
            .filter { isPackageAnExtension(it) }
            .map { ExtensionInfo(packageInfo = it, isShared = true) }


        val extPkgs = (sharedExtPkgs)
            // Remove duplicates. Shared takes priority than private by default
            .distinctBy { it.packageInfo.packageName }
            .toList()

        if (extPkgs.isEmpty()) return emptyList()

        // Load each extension concurrently and wait for completion
        return runBlocking {
            val deferred = extPkgs.map {
                // One malformed package must not prevent the others from loading
                async { runCatching { loadExtension(context, it) }.getOrDefault(LoadResult.Error) }
            }
            deferred.awaitAll()
        }
    }

    /**
     * Attempts to load an extension from the given package name. It checks if the extension
     * contains the required feature flag before trying to load it.
     */
    fun loadExtensionFromPkgName(context: Context, pkgName: String): LoadResult {
        Log.d("ExtensionsLoader", "loadExtensionFromPkgName called for: $pkgName")
        val extensionPackage = getExtensionInfoFromPkgName(context, pkgName)
        if (extensionPackage == null) {
            Log.d("LoadExtensionFromPkgName","Extension package is not found ($pkgName)")
            return LoadResult.Error
        }
        return loadExtension(context, extensionPackage)
    }

    fun getExtensionPackageInfoFromPkgName(context: Context, pkgName: String): PackageInfo? {
        return getExtensionInfoFromPkgName(context, pkgName)?.packageInfo
    }

    private fun getExtensionInfoFromPkgName(context: Context, pkgName: String): ExtensionInfo? {
        val sharedPkg = try {
            context.packageManager.getPackageInfo(pkgName, PACKAGE_FLAGS)
                .takeIf { isPackageAnExtension(it) }
                ?.let {
                    ExtensionInfo(
                        packageInfo = it,
                        isShared = true,
                    )
                }
        } catch (error: PackageManager.NameNotFoundException) {
            null
        }

        return sharedPkg
    }

    /**
     * Loads an extension
     *
     * @param context The application context.
     * @param extensionInfo The extension to load.
     */
    private fun loadExtension(context: Context, extensionInfo: ExtensionInfo): LoadResult {
        return try {
            loadExtensionUnsafe(context, extensionInfo)
        } catch (e: Throwable) {
            Log.d("LoadExtension","Extension load error: ${extensionInfo.packageInfo.packageName}")
            Log.d("LoadExtension Error",e.stackTraceToString())
            LoadResult.Error
        }
    }

    private fun loadExtensionUnsafe(context: Context, extensionInfo: ExtensionInfo): LoadResult {
        val pkgManager = context.packageManager
        val pkgInfo = extensionInfo.packageInfo
        val appInfo = pkgInfo.applicationInfo ?: return LoadResult.Error
        val pkgName = pkgInfo.packageName

        val extName = pkgManager.getApplicationLabel(appInfo).toString().substringAfter("Tadami: ")
        val versionName = pkgInfo.versionName
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
        val apiVersion = versionName?.let { LibVersion.fromVersionName(it) }

        // Still listed, without its sources, so that the user sees why it can't be used
        fun unusable(issue: ExtensionLoadIssue): LoadResult {
            Log.d("LoadExtension", "Extension $extName ($pkgName $versionName) can't be used: $issue")
            return LoadResult.Success(
                Extension.Installed(
                    name = extName,
                    pkgName = pkgName,
                    versionName = versionName.orEmpty(),
                    versionCode = versionCode,
                    apiVersion = apiVersion ?: UNKNOWN_LIB_VERSION,
                    lang = langFromPackageName(pkgName),
                    sources = emptyList(),
                    pkgFactory = null,
                    icon = appInfo.loadIcon(pkgManager),
                    isShared = extensionInfo.isShared,
                    loadIssue = issue,
                ),
            )
        }

        libVersionIssue(apiVersion)?.let {
            Log.d("LoadExtension", "Lib version is $apiVersion, while only versions " +
                    "$API_VERSION_MIN to $API_VERSION_MAX are allowed")
            return unusable(it)
        }

        val sourceClasses = appInfo.metaData?.getString(METADATA_SOURCE_CLASS)
        if (sourceClasses.isNullOrBlank()) {
            Log.d("LoadExtension","Missing $METADATA_SOURCE_CLASS meta-data for extension $extName")
            return unusable(ExtensionLoadIssue.LOAD_FAILED)
        }

        val classLoader = try {
            PathClassLoader(appInfo.sourceDir, null, context.classLoader)
        } catch (e: Exception) {
            Log.d("ClassLoader","Extension load error: $extName ($pkgName)")
            Log.d("ClassLoader",e.stackTraceToString())
            return unusable(ExtensionLoadIssue.LOAD_FAILED)
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
            isShared = extensionInfo.isShared,
        )
        return LoadResult.Success(extension)
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
    private fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
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

    private data class ExtensionInfo(
        val packageInfo: PackageInfo,
        val isShared: Boolean,
    )
}