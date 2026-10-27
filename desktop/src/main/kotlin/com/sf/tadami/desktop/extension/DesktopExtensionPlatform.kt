package com.sf.tadami.desktop.extension

import androidx.compose.ui.graphics.toComposeImageBitmap
import com.sf.tadami.desktop.AppDirs
import com.sf.tadami.extension.util.ApkPackage
import com.sf.tadami.extension.util.ExtensionPlatform
import com.sf.tadami.utils.Log
import org.jetbrains.skia.Image
import java.io.File
import java.net.URLClassLoader

/**
 * The extension APKs on the desktop: read with apksig and apk-parser, their dex converted to JVM
 * bytecode once, cached by version.
 */
class DesktopExtensionPlatform : ExtensionPlatform {

    override val filesDir: File get() = AppDirs.data

    private val jarsDir: File by lazy { File(AppDirs.cache, "extension-jars").apply { mkdirs() } }

    override fun readApk(file: File): ApkPackage? = try {
        val signature = ApkReader.signature(file)
        val manifest = ApkReader.manifest(file)
        ApkPackage(
            file = file,
            packageName = manifest.packageName,
            versionName = manifest.versionName,
            versionCode = manifest.versionCode,
            label = manifest.label,
            features = manifest.features,
            metaData = manifest.metaData,
            signatureHashes = signature.hashes,
            multipleSigners = signature.multipleSigners,
            iconLoader = { ApkReader.icon(file)?.let { Image.makeFromEncoded(it).toComposeImageBitmap() } },
        )
    } catch (e: Exception) {
        Log.w(TAG, "Unreadable APK ${file.name}", e)
        null
    }

    @Synchronized
    override fun classLoader(apk: ApkPackage, parent: ClassLoader): ClassLoader {
        // The converted code of this exact APK: another install of the package gets its own
        val prefix = apk.packageName + "-"
        val jar = File(jarsDir, "$prefix${apk.versionCode}-${apk.file.length()}-${apk.file.lastModified()}.jar")
        if (!jar.isFile) {
            jarsDir.listFiles { file -> file.name.startsWith(prefix) }?.forEach { it.delete() }
            DexConverter.convert(apk.file, jar)
        }
        return URLClassLoader(arrayOf(jar.toURI().toURL()), parent)
    }

    override fun processId(): Int = ProcessHandle.current().pid().toInt()

    private companion object {
        const val TAG = "DesktopExtensionPlatform"
    }
}
