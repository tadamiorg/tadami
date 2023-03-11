package com.sf.animescraper.network.api.model

/*data class AnimeFilterList(private val list: MutableList<AnimeFilter<*>>) : MutableList<AnimeFilter<*>> by list {

    constructor(vararg fs: AnimeFilter<*>) : this(if (fs.isNotEmpty()) fs.toMutableList() else mutableListOf())

    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return list.hashCode()
    }
}*/

class AnimeFilterList(val filters : MutableList<AnimeFilter>) : MutableList<AnimeFilter> by filters{
    constructor(vararg fs: AnimeFilter) : this(if (fs.isNotEmpty()) fs.toMutableList() else mutableListOf())

    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return filters.hashCode()
    }
}