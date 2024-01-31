package com.sf.tadami.ui.utils

import java.io.Closeable

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

inline fun <T : Closeable?> Array<T>.use(block: () -> Unit) {
    var blockException: Throwable? = null
    try {
        return block()
    } catch (e: Throwable) {
        blockException = e
        throw e
    } finally {
        when (blockException) {
            null -> forEach { it?.close() }
            else -> forEach {
                try {
                    it?.close()
                } catch (closeException: Throwable) {
                    blockException.addSuppressed(closeException)
                }
            }
        }
    }
}