package com.sf.tadami.source.model

interface SSeason {
    var name: String
    var url: String
    var number: Float

    companion object {
        fun create(): SSeason {
            return SSeasonImpl()
        }
    }
}
