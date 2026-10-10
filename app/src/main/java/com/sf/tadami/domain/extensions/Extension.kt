package com.sf.tadami.domain.extensions

import android.graphics.drawable.Drawable
import com.sf.tadami.source.Source
import com.sf.tadami.source.StubSource
import com.sf.tadami.utils.Lang

sealed class Extension {

    abstract val name: String
    abstract val pkgName: String
    abstract val versionName: String
    abstract val versionCode: Long
    abstract val apiVersion: Double
    abstract val lang: Lang?

    /**
     * Stable identity for list rendering.
     *
     * Equal to [pkgName] for everything except an available extension that ships several sources:
     * the Extensions tab shows one row per source, so those rows need to be distinguishable while
     * [pkgName] must keep naming the actual package — it is what the APK on disk is named after and
     * what the loader matches against the archive.
     */
    open val uiKey: String get() = pkgName

    data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val apiVersion: Double,
        override val lang: Lang,
        val pkgFactory: String?,
        val sources: List<Source>,
        val icon: Drawable?,
        val hasUpdate: Boolean = false,
        val isObsolete: Boolean = false,
        val repoUrl: String? = null,
    ) : Extension()

    data class Available(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val apiVersion: Double,
        override val lang: Lang,
        val sources: List<Source>,
        val apkName: String,
        val iconUrl: String,
        val repoUrl: String,
        /** Set when this row represents one source of a multi-source extension. See [uiKey]. */
        val sourceId: Long? = null,
    ) : Extension() {

        override val uiKey: String get() = if (sourceId == null) pkgName else "$pkgName-$sourceId"

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