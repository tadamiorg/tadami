package com.sf.tadami.ui.utils

fun <T : R, R : Any> List<T>.insertSeparators(
    generator: (T?, T?) -> R?,
): List<R> {
    if (isEmpty()) return emptyList()
    val newList = mutableListOf<R>()
    for (i in -1..lastIndex) {
        val before = getOrNull(i)
        before?.let { newList.add(it) }
        val after = getOrNull(i + 1)
        val separator = generator.invoke(before, after)
        separator?.let { newList.add(it) }
    }
    return newList
}

fun <T> List<T>.moveItemUp(index: Int) : List<T> {
    val newList = this.toMutableList()
    if (index > 0 && index < newList.size) {
        val temp = newList[index]
        newList[index] = newList[index - 1]
        newList[index - 1] = temp
    }
    return newList
}

fun <T> List<T>.moveItemDown(index: Int) : List<T> {
    val newList = this.toMutableList()
    if (index >= 0 && index < newList.size - 1) {
        val temp = newList[index]
        newList[index] = newList[index + 1]
        newList[index + 1] = temp
    }
    return newList
}