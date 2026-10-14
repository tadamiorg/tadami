package com.sf.tadami.extension.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.pm.PackageInfoCompat
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.network.GET
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.awaitSuccess
import kotlinx.coroutines.runInterruptible
import okhttp3.CacheControl
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Installs the extensions into [ExtensionsStorage]: their APK is downloaded or copied into a
 * temporary file, then verified before it replaces the installed one.
 *
 * @param context The application context.
 */
internal class ExtensionsInstaller(private val context: Context) {

    private val network: NetworkHelper by injectLazy()

    private val dir = ExtensionsStorage.dir(context)

    fun newTmpFile(pkgName: String): File = ExtensionsStorage.newTmpFile(dir, pkgName)

    /** Downloads the APK at [url] into [tmp]. Cancelling the coroutine stops the download. */
    suspend fun download(url: String, tmp: File) {
        // Neither from nor into the HTTP cache, the file is kept here
        val request = GET(url, cache = CacheControl.Builder().noStore().build())
        network.client.newCall(request).awaitSuccess().use { response ->
            // The reads block, the cancellation interrupts them
            runInterruptible { copyAtMost(response.body.byteStream(), tmp, MAX_APK_SIZE) }
        }
    }

    /** The version code of the installed extension [pkgName], null when there is none. */
    fun installedVersionCode(pkgName: String): Long? {
        val file = ExtensionsStorage.apkFile(dir, pkgName).takeIf { it.isFile } ?: return null
        return ExtensionsLoader.getArchivePackageInfo(context, file)
            ?.let { PackageInfoCompat.getLongVersionCode(it) }
    }

    /**
     * Verifies [tmp], then makes it the APK of [pkgName]. To run under the lock of the package,
     * see [ExtensionsStorage.withLock].
     *
     * @param listed The repo entry [tmp] was downloaded from, null for a copied package.
     * @param allowSameVersion Whether the installed version can be replaced by the same one.
     * @return Why [tmp] was refused, the installed APK being kept, or null once it replaced it.
     */
    fun verifyAndCommit(
        tmp: File,
        pkgName: String,
        listed: Extension.Available?,
        allowSameVersion: Boolean,
    ): ApkRejection? {
        val apk = readApk(tmp) ?: return ApkRejection.INVALID_APK
        // One that can't be read can't be used either: nothing to keep
        val installed = ExtensionsStorage.apkFile(dir, pkgName).takeIf { it.isFile }?.let { readApk(it) }
        rejectionOf(apk, pkgName, listed, installed, allowSameVersion)?.let { return it }
        ExtensionsStorage.commit(dir, pkgName, tmp)
        return null
    }

    private fun readApk(file: File): ApkInfo? {
        val pkgInfo = ExtensionsLoader.getArchivePackageInfo(context, file) ?: return null
        return ApkInfo(
            pkgName = pkgInfo.packageName,
            versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo),
            versionName = pkgInfo.versionName,
            isExtension = ExtensionsLoader.isPackageAnExtension(pkgInfo),
            hasNativeLibs = hasNativeLibs(file),
            signatureHashes = ExtensionsLoader.signatureHashes(pkgInfo),
            multipleSigners = pkgInfo.signingInfo?.hasMultipleSigners() == true,
        )
    }

    companion object {
        private const val MAX_APK_SIZE = 20L * 1024 * 1024

        /** Copies [input] into [file], failing past [maxBytes]. */
        internal fun copyAtMost(input: InputStream, file: File, maxBytes: Long) {
            file.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > maxBytes) throw IOException("Larger than $maxBytes bytes")
                    output.write(buffer, 0, read)
                }
            }
        }

        /** Whether the APK [file] has native libraries: they can't be loaded, there is no library path. */
        internal fun hasNativeLibs(file: File): Boolean {
            return ZipFile(file).use { zip -> zip.entries().asSequence().any { it.name.startsWith("lib/") } }
        }

        /**
         * Why [apk] can't become the APK of [pkgName], or null when it can.
         *
         * @param listed The repo entry it was downloaded from, null for a copied package.
         * @param installed The APK it would replace, null when there is none.
         * @param allowSameVersion Whether the version of [installed] can be replaced by the same one.
         */
        internal fun rejectionOf(
            apk: ApkInfo,
            pkgName: String,
            listed: Extension.Available?,
            installed: ApkInfo?,
            allowSameVersion: Boolean,
        ): ApkRejection? = when {
            apk.pkgName != pkgName || !ExtensionsStorage.isValidPkgName(apk.pkgName) -> ApkRejection.WRONG_PACKAGE
            !apk.isExtension -> ApkRejection.NOT_AN_EXTENSION
            // A rollback or another file than the listed one
            listed != null && (apk.versionCode != listed.versionCode || apk.versionName != listed.versionName) ->
                ApkRejection.WRONG_VERSION
            ExtensionsLoader.libVersionIssue(apk.versionName?.let { LibVersion.fromVersionName(it) }) != null ->
                ApkRejection.UNSUPPORTED_LIB
            apk.hasNativeLibs -> ApkRejection.NATIVE_LIBS
            installed == null -> null
            !canReplaceSigners(installed, apk) -> ApkRejection.SIGNER_CHANGED
            apk.versionCode < installed.versionCode -> ApkRejection.NOT_NEWER
            apk.versionCode == installed.versionCode && !allowSameVersion && !raisesLibVersion(installed, apk) ->
                ApkRejection.NOT_NEWER
            else -> null
        }

        /**
         * Whether [new] is built against a newer extensions lib than [old]: with the same version
         * code, an update as well, as the extension manager counts it.
         */
        private fun raisesLibVersion(old: ApkInfo, new: ApkInfo): Boolean {
            val newLib = new.versionName?.let { LibVersion.fromVersionName(it) } ?: return false
            val oldLib = old.versionName?.let { LibVersion.fromVersionName(it) } ?: return true
            return newLib > oldLib
        }

        /**
         * Whether an APK signed as [new] can replace one signed as [old], Android's own update rule:
         * the same signers, or a key rotated from the old one, in the history the platform verified.
         */
        internal fun canReplaceSigners(old: ApkInfo, new: ApkInfo): Boolean = when {
            old.signatureHashes.isEmpty() || new.signatureHashes.isEmpty() -> false
            // No key rotation with several signers
            old.multipleSigners || new.multipleSigners ->
                old.multipleSigners == new.multipleSigners && old.signatureHashes.toSet() == new.signatureHashes.toSet()
            else -> old.signatureHashes.last() in new.signatureHashes
        }
    }
}

/** What the verification of an extension APK reads of it. */
internal data class ApkInfo(
    val pkgName: String,
    val versionCode: Long,
    val versionName: String?,
    /** Whether it declares the extension feature. */
    val isExtension: Boolean,
    val hasNativeLibs: Boolean,
    /** See [ExtensionsLoader.signatureHashes]. */
    val signatureHashes: List<String>,
    val multipleSigners: Boolean,
)

/** Why an extension APK is refused, the installed one being kept. */
internal enum class ApkRejection(@StringRes val messageRes: Int) {
    /** Not an APK that can be read. */
    INVALID_APK(R.string.ext_install_error_invalid),
    WRONG_PACKAGE(R.string.ext_install_error_invalid),
    NOT_AN_EXTENSION(R.string.ext_install_error_invalid),

    /** Its version isn't the listed one. */
    WRONG_VERSION(R.string.ext_install_error_invalid),
    UNSUPPORTED_LIB(R.string.ext_install_error_invalid),
    NATIVE_LIBS(R.string.ext_install_error_invalid),

    /** Not newer than the installed one. */
    NOT_NEWER(R.string.ext_install_error_not_newer),

    /** Not signed with the key of the installed one. */
    SIGNER_CHANGED(R.string.ext_install_error_signer),
}
