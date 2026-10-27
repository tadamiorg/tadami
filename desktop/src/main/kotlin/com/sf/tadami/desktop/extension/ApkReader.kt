package com.sf.tadami.desktop.extension

import com.android.apksig.ApkVerifier
import net.dongliu.apk.parser.ApkFile
import org.w3c.dom.Element
import java.io.File
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory

/** What the manifest of an extension APK declares. */
class ApkManifest(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val features: List<String>,
    val metaData: Map<String, String>,
)

/** See [ApkReader.signature]. */
class ApkSignature(val hashes: List<String>, val multipleSigners: Boolean)

/** Reads an extension APK without running any of its code, as Android's PackageManager does. */
object ApkReader {

    /**
     * SHA-256 fingerprints of the certificates [apk] is verified to be signed with, the current one
     * last (Android's signingCertificateHistory), empty when its signature doesn't verify.
     */
    fun signatureHashes(apk: File): List<String> = signature(apk).hashes

    /** The certificates of [apk], see [signatureHashes], and whether it has several signers. */
    fun signature(apk: File): ApkSignature {
        val result = ApkVerifier.Builder(apk).setMinCheckedPlatformVersion(28).build().verify()
        if (!result.isVerified) return ApkSignature(emptyList(), multipleSigners = false)
        val signers = result.signerCertificates
        val history = if (signers.size > 1) {
            signers
        } else {
            result.signingCertificateLineage?.certificatesInLineage ?: signers
        }
        return ApkSignature(history.map { sha256(it.encoded) }, multipleSigners = signers.size > 1)
    }

    fun manifest(apk: File): ApkManifest = ApkFile(apk).use { file ->
        val meta = file.apkMeta
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(file.manifestXml.byteInputStream())
        val metaData = mutableMapOf<String, String>()
        val nodes = document.getElementsByTagName("meta-data")
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? Element ?: continue
            val name = element.getAttribute("android:name").takeIf { it.isNotEmpty() } ?: continue
            metaData[name] = element.getAttribute("android:value")
        }
        ApkManifest(
            packageName = meta.packageName,
            label = meta.label.orEmpty(),
            versionName = meta.versionName.orEmpty(),
            versionCode = meta.versionCode ?: 0L,
            features = meta.usesFeatures.orEmpty().map { it.name },
            metaData = metaData,
        )
    }

    /** The launcher icon of the highest density, null when it has none as a bitmap. */
    fun icon(apk: File): ByteArray? = ApkFile(apk).use { file ->
        file.iconFiles.orEmpty()
            .filter { it.isFile && it.data != null && it.path.endsWith(".png") }
            .maxByOrNull { it.density }
            ?.data
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
