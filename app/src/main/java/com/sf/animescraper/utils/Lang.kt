package com.sf.animescraper.utils

import android.content.res.Resources
import com.sf.animescraper.R
import com.sf.animescraper.network.api.online.AnimeSource

enum class Lang(private val langRes : Int) {
    ENGLISH(R.string.language_en),
    FRENCH(R.string.language_fr);

    fun getRes() : Int{
        return this.langRes
    }

}

fun getString(resId : Int) : String{
    return Resources.getSystem().getString(resId)
}

sealed class SourceList{
    data class SourceItem(
        val source : AnimeSource
    ) : SourceList()

    data class Section(
        var title : String
    ) : SourceList()
}
