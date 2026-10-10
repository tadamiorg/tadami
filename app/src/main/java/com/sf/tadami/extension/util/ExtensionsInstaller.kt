package com.sf.tadami.extension.util

import android.content.Context
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.InstallStep
import com.sf.tadami.network.GET
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.awaitSuccess
import com.sf.tadami.utils.logcat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import logcat.LogPriority
import okhttp3.CacheControl
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Downloads extensions into the app's private storage and makes them loadable.
 *
 * Nothing here talks to `PackageInstaller` any more. An "install" is now a download, a structural
 * check and an atomic rename — no system dialog, no `REQUEST_INSTALL_PACKAGES`, and no
 * developer-verification requirement, since no package is being installed. The extension APK is
 * exactly the same file it always was; only its destination changed.
 *
 * @param context The application context.
 */
internal class ExtensionsInstaller(private val context: Context) {

    private val networkService: NetworkHelper by injectLazy()

    /**
     * Packages whose in-flight install the user asked to abort. The download loop checks this
     * between chunks; the alternative — cancelling the collecting coroutine — isn't available to
     * us because the Extensions tab collects on its own scope.
     */
    private val cancelledInstalls: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Downloads [url] and puts it in place, reporting progress through the returned flow. The flow
     * completes once the extension is installed, cancelled or has failed.
     *
     * The caller is expected to load the result afterwards (see
     * [ExtensionsLoader.loadExtensionFromPkgName]) — that is also where the signature is checked,
     * so an extension signed by an unknown key still lands on disk here and simply refuses to load
     * until the user trusts it.
     */
    fun downloadAndInstall(url: String, extension: Extension): Flow<InstallStep> = flow {
        val pkgName = extension.pkgName
        cancelledInstalls.remove(pkgName)
        val tmp = ExtensionsStorage.tmpFile(context, pkgName)

        emit(InstallStep.Pending)
        emit(InstallStep.Downloading)
        var outcome = attempt { download(url, tmp, pkgName) }

        if (outcome.isSuccess) {
            emit(InstallStep.Installing)
            outcome = attempt {
                verifyArchive(tmp, pkgName)
                ExtensionsStorage.commit(context, pkgName, tmp)
            }
        }

        // On the happy path commit() has already moved the file away and this is a no-op.
        tmp.delete()
        cancelledInstalls.remove(pkgName)

        emit(
            outcome.fold(
                onSuccess = { InstallStep.Installed },
                onFailure = { error ->
                    if (error is InstallCancelledException) {
                        InstallStep.Idle
                    } else {
                        logcat(LogPriority.ERROR, error) { "Failed to install extension $pkgName" }
                        InstallStep.Error
                    }
                },
            ),
        )
    }.flowOn(Dispatchers.IO)

    /** Aborts an in-flight install. Harmless if there isn't one. */
    fun cancelInstall(pkgName: String) {
        cancelledInstalls.add(pkgName)
    }

    /** Uninstalling is now just deleting the file. */
    fun uninstall(pkgName: String): Boolean = ExtensionsStorage.delete(context, pkgName)

    private suspend fun download(url: String, target: File, pkgName: String) {
        // Deliberately uncached: these are one-shot downloads of files we then keep ourselves, and
        // letting them through the shared 5 MiB response cache would evict actual browsing data.
        val request = GET(url, cache = CacheControl.Builder().noStore().build())

        networkService.client.newCall(request).awaitSuccess().use { response ->
            response.body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    while (true) {
                        ensureStillWanted(pkgName)
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
        }
    }

    /**
     * Structural checks only, and the reason the file is never committed blind: it confirms the
     * download really is a Tadami extension and really is the package the repo index claimed,
     * rather than whatever a redirect or a compromised repo happened to return.
     */
    private fun verifyArchive(file: File, expectedPkgName: String) {
        val pkgInfo = ExtensionsLoader.getArchivePackageInfo(context, file)
            ?: throw IOException("$expectedPkgName is not a readable APK")

        if (pkgInfo.packageName != expectedPkgName) {
            throw IOException(
                "Downloaded package ${pkgInfo.packageName} doesn't match the requested $expectedPkgName",
            )
        }
        if (!ExtensionsLoader.declaresExtensionFeature(pkgInfo)) {
            throw IOException("$expectedPkgName is not a Tadami extension")
        }
    }

    private suspend fun ensureStillWanted(pkgName: String) {
        currentCoroutineContext().ensureActive()
        if (pkgName in cancelledInstalls) throw InstallCancelledException()
    }

    /**
     * Like `runCatching`, but lets coroutine cancellation through instead of turning it into a
     * failed install.
     */
    private inline fun <T> attempt(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

    private class InstallCancelledException : Exception()

    companion object {
        private const val DOWNLOAD_BUFFER_SIZE = 8 * 1024
    }
}
