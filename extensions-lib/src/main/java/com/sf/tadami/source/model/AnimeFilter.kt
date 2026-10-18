package com.sf.tadami.source.model

/**
 * A filter of the search of a source, see
 * [com.sf.tadami.source.AnimeCatalogueSource.getFilterList]. The app shows it, and passes it back
 * to the search with the state the user set.
 *
 * @property name shown in the filters
 * @since extensions-lib 1.5
 */
sealed class AnimeFilter(val name: String){
    /**
     * A title between the filters.
     *
     * @since extensions-lib 1.5
     */
    class Header(name: String) : AnimeFilter(name)

    /**
     * A check box, checked when [state] is true.
     *
     * @since extensions-lib 1.5
     */
    abstract class CheckBox(name: String, var state: Boolean = false) : AnimeFilter(name)

    /**
     * A choice of one of [values], [state] being the index of the one chosen.
     *
     * @since extensions-lib 1.5
     */
    abstract class Select(name: String, val values: Array<String>, var state: Int = 0) : AnimeFilter(name)

    /**
     * A group of check boxes, shown together.
     *
     * @since extensions-lib 1.5
     */
    abstract class CheckBoxGroup(name: String, var state: List<CheckBox>) : AnimeFilter(name)
}
