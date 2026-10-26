package com.sf.tadami.utils

import kotlinx.coroutines.flow.StateFlow
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class CollectAsStateValue<T>(private val stateFlow: StateFlow<T>) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return stateFlow.value
    }
}