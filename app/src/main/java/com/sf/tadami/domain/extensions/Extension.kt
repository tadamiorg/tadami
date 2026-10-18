package com.sf.tadami.domain.extensions

import android.graphics.drawable.Drawable
import com.sf.tadami.extension.api.ExtensionRepos
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.LibVersion
import com.sf.tadami.extension.model.SourceIdConflict
import com.sf.tadami.source.Source
import com.sf.tadami.source.StubSource
import com.sf.tadami.utils.Lang

sealed class Extension {

    companion object {
        /**
         * Whether an extension of [versionCode], built against [libVersion], updates the installed
         * one of [installedVersionCode] and [installedLibVersion]: a greater version code, or the
         * same one with a higher lib, the rule the install verification applies. A null lib
         * version is unknown: one can't be an update, an installed one is replaced by a known one.
         */
        fun isUpdate(
            versionCode: Long,
            libVersion: LibVersion?,
            installedVersionCode: Long,
            installedLibVersion: LibVersion?,
        ): Boolean = when {
            versionCode != installedVersionCode -> versionCode > installedVersionCode
            libVersion == null -> false
            else -> installedLibVersion == null || libVersion > installedLibVersion
        }
    }

    abstract val name: String
    abstract val pkgName: String
    abstract val versionName: String
    abstract val versionCode: Long
    abstract val apiVersion: LibVersion
    abstract val lang: Lang?

    data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val apiVersion: LibVersion,
        override val lang: Lang,
        val pkgFactory: String?,
        val sources: List<Source>,
        val icon: Drawable?,
        val hasUpdate: Boolean = false,
        val isObsolete: Boolean = false,
        /**
         * The repo it was installed or updated from, recorded since, or the only repo that listed it,
         * then recorded too, null when it's unknown, see [listedIn].
         */
        val repoUrl: String? = null,
        /** Whether it was installed with adb in a debug build, from no repo. */
        val isManual: Boolean = false,
        /** Set when the extension can't be used, [sources] is empty then. */
        val loadIssue: ExtensionLoadIssue? = null,
        /** SHA-256 fingerprint of its signing key, the one stored when the user trusts it. */
        val signatureHash: String? = null,
        /** Why its sources aren't registered, with [ExtensionLoadIssue.SOURCE_ID_CONFLICT]. */
        val sourceIdConflict: SourceIdConflict? = null,
        /**
         * Its sources, loaded but not registered because of [sourceIdConflict]: they replace [sources]
         * once the conflict is gone, see resolveSourceIdConflicts. Empty when it was refused before
         * its code was loaded, from its [declaredSourceIds]: it's loaded once the conflict is gone.
         */
        val refusedSources: List<Source> = emptyList(),
        /**
         * The IDs of its sources its manifest declares, null when it declares none, the extensions
         * published before generated source IDs: those are checked for a conflict before any of its
         * code is loaded, see ExtensionsLoader.
         */
        val declaredSourceIds: List<Long>? = null,
    ) : Extension() {

        /**
         * Its entry in [available], the one it's updated with: only from the repo it comes from when
         * it's known, see [repoUrl], from the official repo otherwise, or the first one that lists
         * it. Another repo can't update it: one of the same package signed with another key would be
         * refused anyway, see ExtensionsInstaller.
         */
        fun listedIn(available: List<Available>): Available? {
            val entries = available.filter { it.pkgName == pkgName }
            return if (repoUrl != null) {
                entries.find { it.repoUrl == repoUrl }
            } else {
                entries.find { ExtensionRepos.isOfficial(it.repoUrl) } ?: entries.firstOrNull()
            }
        }

        /** Whether [available] is an update of this one, see [isUpdate]. */
        fun isUpdatedBy(available: Available): Boolean =
            isUpdate(available.versionCode, available.apiVersion, versionCode, apiVersion)
    }

    data class Available(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val apiVersion: LibVersion,
        override val lang: Lang,
        val sources: List<Source>,
        val apkName: String,
        val iconUrl: String,
        val repoUrl: String,
        /** The size in bytes of its APK the repo lists, null when it doesn't. */
        val size: Long? = null,
        /** The hex SHA-256 of its APK the repo lists, null when it doesn't. */
        val sha256: String? = null,
    ) : Extension() {

        data class Source(
            val id: Long,
            val lang: Lang,
            val name: String,
            val baseUrl: String,
        ) {
            fun toStubSource(): StubSource {
                return StubSource(
                    id = this.id,
                    lang = this.lang,
                    name = this.name,
                )
            }
        }
    }
}