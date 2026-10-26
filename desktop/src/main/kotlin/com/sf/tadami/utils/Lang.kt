package com.sf.tadami.utils

import com.sf.tadami.source.AnimeCatalogueSource

enum class Lang(val displayName : String) {
    ENGLISH("English"),
    FRENCH("French"),
    UNKNOWN("Unknown");
    companion object {

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
