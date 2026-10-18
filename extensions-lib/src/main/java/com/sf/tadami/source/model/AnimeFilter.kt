package com.sf.tadami.source.model

sealed class AnimeFilter(val name: String){
    class Header(name: String) : AnimeFilter(name)
    abstract class CheckBox(name: String, var state: Boolean = false) : AnimeFilter(name)
    abstract class Select(name: String, val values: Array<String>, var state: Int = 0) : AnimeFilter(name)
    abstract class CheckBoxGroup(name: String, var state: List<CheckBox>) : AnimeFilter(name)
}


