package com.sf.tadami.extension.util

import android.content.pm.PackageInfo
import android.content.pm.Signature
import java.security.MessageDigest

/**
 * Extension trust.
 *
 * While extensions were installed packages, Android verified their signature for us at install
 * time and we never had to think about it. Nothing installs them any more, so that check moves
 * here — and it is now the only thing between a hostile or compromised repo and arbitrary code
 * running inside Tadami's process, with Tadami's permissions, via [ExtensionsLoader].
 */
internal object ExtensionSignatures {

    /**
     * The certificate every extension in the official repo is signed with.
     *
     * Matching extensions load silently; anything else is [com.sf.tadami.extension.model.LoadResult.Reason.UNTRUSTED]
     * until the user explicitly accepts its fingerprint. That prompt is what makes supporting
     * user-added repos defensible.
     */
    const val OFFICIAL_SIGNATURE = "11dec208a209773efdc5348af5de709ca9af067e9154d1dd3c6abdb0fa3cc200"

    /**
     * SHA-256 of the certificate that signed this package, or null if it carries no usable one.
     *
     * Extensions are signed with APK Signature Scheme v2 only (no v1/JAR), so [PackageInfo.signingInfo]
     * is the real source here. [PackageInfo.signatures] is kept as a fallback because
     * `getPackageArchiveInfo` has been inconsistent about populating `signingInfo` across OEM
     * builds, and a null here means refusing to load a perfectly legitimate extension.
     */
    @Suppress("DEPRECATION")
    fun fingerprintOf(pkgInfo: PackageInfo): String? {
        val signatures: Array<Signature>? = pkgInfo.signingInfo?.let { info ->
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        } ?: pkgInfo.signatures

        // signingCertificateHistory runs oldest-to-newest and only the current signer counts —
        // taking any match would keep honouring a key that was deliberately rotated away.
        return signatures?.lastOrNull()?.toSha256()
    }

    fun isTrusted(fingerprint: String?, trusted: Set<String>): Boolean =
        fingerprint != null && (fingerprint == OFFICIAL_SIGNATURE || fingerprint in trusted)

    private fun Signature.toSha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }
}
