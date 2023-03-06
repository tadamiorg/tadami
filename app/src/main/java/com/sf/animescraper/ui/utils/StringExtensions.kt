package com.sf.animescraper.ui.utils

fun String.capFirstLetter() : String{
    return this.replaceFirstChar { it.uppercase() }
}

fun String.lowFirstLetter() : String{
    return this.replaceFirstChar { it.lowercase() }
}

fun String?.ifNull(replace : String = "") : String{
    return this ?: replace
}