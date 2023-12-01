package com.sf.tadami.ui.utils



fun <T>Map<T, *>.toPrefMultiCheckbox(): Map<T, Pair<String, Boolean>> {
    return this.mapValues { (_, value) ->
        when(value){
            is Pair<*, *> -> {
                val stringValue = value.first as String
                val boolValue = value.second as? Boolean ?: true
                stringValue to boolValue
            }
            is String -> {
                value to true
            }
            else -> {
                throw IllegalArgumentException("Invalid value type")
            }
        }
    }
}