package com.sf.tadami.source.model

/**
 * The filters of the search of a source, see
 * [com.sf.tadami.source.AnimeCatalogueSource.getFilterList]. It equals no other list, not even
 * itself, so the app always searches again with it.
 *
 * @since extensions-lib 1.5
 */
class AnimeFilterList(val filters : MutableList<AnimeFilter>) : MutableList<AnimeFilter> by filters{
    constructor(vararg fs: AnimeFilter) : this(if (fs.isNotEmpty()) fs.toMutableList() else mutableListOf())

    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun hashCode(): Int {
        return filters.hashCode()
    }
}
