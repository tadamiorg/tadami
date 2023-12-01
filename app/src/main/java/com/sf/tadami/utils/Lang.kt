package com.sf.tadami.utils

import android.content.res.Resources
import com.sf.tadami.R
import com.sf.tadami.network.api.online.AnimeCatalogueSource

enum class Lang(private val langRes : Int) {
    ENGLISH(R.string.language_en),
    FRENCH(R.string.language_fr),
    UNKNOWN(R.string.language_unknown);

    fun getRes() : Int{
        return this.langRes
    }
    companion object {
        fun getAllLangs(): Set<Lang> {
            return enumValues<Lang>().filter { it != UNKNOWN }.toSet()
        }

        fun Set<Lang>.toPref() : Set<String> {
            return this.map { "${it.getRes()}" }.toSet()
        }
    }

}

fun getString(resId : Int) : String{
    return Resources.getSystem().getString(resId)
}

sealed class SourceList{
    data class SourceItem(
        val source : AnimeCatalogueSource
    ) : SourceList()

    data class Section(
        var title : String
    ) : SourceList()
}
