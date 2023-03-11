package com.sf.animescraper.network.api.model


/*sealed class AnimeFilter<T>(val name: String, var state: T) {
    class Header<T : Any>(name: String) : AnimeFilter<T?>(name, null)
    abstract class CheckBox(name: String, state: Boolean = false) : AnimeFilter<Boolean>(name, state)
    abstract class Select<S>(name: String, val values: Array<S>, state: Int = 0) : AnimeFilter<Int>(name, state)
    abstract class Group<G>(name: String, state: List<G>, val type: Class<G>) : AnimeFilter<List<G>>(name, state)

    val isCheckBox
        get() = this is CheckBox

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnimeFilter<*>) return false

        return name == other.name && state == other.state
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (state?.hashCode() ?: 0)
        return result
    }
}*/


sealed class AnimeFilter(val name: String){
    class Header(name: String) : AnimeFilter(name)
    abstract class CheckBox(name: String, var state: Boolean = false) : AnimeFilter(name)
    abstract class Select(name: String, val values: Array<String>, var state: Int = 0) : AnimeFilter(name)
    abstract class CheckBoxGroup(name: String, var state: List<CheckBox>) : AnimeFilter(name)
}


