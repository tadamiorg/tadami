package com.sf.tadami.source.model

class SSeasonImpl : SSeason {
    override lateinit var name: String
    override lateinit var url: String
    override var number: Float = -1f
    override fun toString(): String {
        return "SSeasonImpl(name='$name', url='$url', number=$number)"
    }
}
