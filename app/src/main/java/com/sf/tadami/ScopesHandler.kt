package com.sf.tadami

import kotlinx.coroutines.CoroutineScope

class ScopesHandler {
    val dataStoreScopes : MutableMap<Long, CoroutineScope> = mutableMapOf()
}