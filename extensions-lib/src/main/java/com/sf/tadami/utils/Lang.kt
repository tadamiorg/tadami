package com.sf.tadami.utils

enum class Lang(private val langRes : Int) {
    ENGLISH(throw Exception("Stub")),
    FRENCH(throw Exception("Stub")),
    UNKNOWN(throw Exception("Stub"));

    fun getRes() : Int = throw Exception("Stub")
    companion object {

        fun getLangByName(name : String) : Lang? = throw Exception("Stub")
        fun valueOfOrDefault(value : String?) : Lang {
            throw Exception("Stub")
        }
        fun getAllLangs(): Set<Lang> = throw Exception("Stub")
        fun Set<Lang>.toPref() : Set<String> = throw Exception("Stub")
    }

}
