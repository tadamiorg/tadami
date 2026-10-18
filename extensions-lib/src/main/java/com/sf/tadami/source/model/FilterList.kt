package com.sf.tadami.source.model

class AnimeFilterList(val filters : MutableList<AnimeFilter>) : MutableList<AnimeFilter> by filters{
    constructor(vararg fs: AnimeFilter) : this(if (fs.isNotEmpty()) fs.toMutableList() else mutableListOf())

    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return filters.hashCode()
    }
}