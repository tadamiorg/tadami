package com.sf.tadami.ui.utils

/**
 * [f] of each element, run in parallel on the IO dispatcher. Blocks until all are done.
 *
 * @since extensions-lib 1.5
 */
fun <A, B> Iterable<A>.parallelMap(f: suspend (A) -> B): List<B> =
    throw Exception("Stub")

/**
 * [f] of each element and its index, run in parallel on the IO dispatcher. Blocks until all are done.
 *
 * @since extensions-lib 1.5
 */
fun <A, B> Iterable<A>.parallelMapIndexed(f: suspend (Int,A) -> B): List<B> =
    throw Exception("Stub")
