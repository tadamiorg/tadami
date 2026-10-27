package com.sf.tadami.utils

import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.language_en
import com.sf.tadami.shared.resources.language_fr
import com.sf.tadami.shared.resources.language_unknown
import com.sf.tadami.source.AnimeCatalogueSource
import org.jetbrains.compose.resources.StringResource

enum class Lang(
    /** The string naming it. */
    val res: StringResource,
) {
    ENGLISH(Res.string.language_en),
    FRENCH(Res.string.language_fr),
    UNKNOWN(Res.string.language_unknown);

    /** The Android string resource naming it, part of the extension API: use [res]. */
    @Deprecated("An Android resource ID: use res", ReplaceWith("res"))
    fun getRes(): Int = androidStringRes?.invoke(this) ?: 0

    companion object {

        /** The Android string resource ID of each language, set by the Android app, for [getRes]. */
        var androidStringRes: ((Lang) -> Int)? = null

        fun getLangByName(name : String) : Lang?{
            return Lang.values().find {
                it.name == name
            }
        }
        fun valueOfOrDefault(value : String?) : Lang {
            return try {
                Lang.valueOf(value ?: "")
            } catch (e: Exception){
                UNKNOWN
            }
        }
        /** From a language code ("en", "fr"), as found in extension package names and repo indexes. */
        fun fromCode(code: String?): Lang {
            return when (code) {
                "en" -> ENGLISH
                "fr" -> FRENCH
                else -> UNKNOWN
            }
        }

        fun getAllLangs(): Set<Lang> {
            return enumValues<Lang>().filter { it != UNKNOWN }.toSet()
        }

        fun Set<Lang>.toPref() : Set<String> {
            return this.map { it.name }.toSet()
        }
    }

}

sealed class SourceList{
    data class SourceItem(
        val source : AnimeCatalogueSource
    ) : SourceList()

    data class Section(
        var title : String
    ) : SourceList()
}
