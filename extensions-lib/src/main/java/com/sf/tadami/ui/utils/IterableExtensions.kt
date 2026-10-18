package com.sf.tadami.ui.utils

fun <A, B> Iterable<A>.parallelMap(f: suspend (A) -> B): List<B> =
    throw Exception("Stub")

fun <A, B> Iterable<A>.parallelMapIndexed(f: suspend (Int,A) -> B): List<B> =
    throw Exception("Stub")