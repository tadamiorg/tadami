package com.sf.tadami.utils

/**
 * The language of a source.
 *
 * @since extensions-lib 1.5
 */
enum class Lang(private val langRes : Int) {
    /** English. */
    ENGLISH(throw Exception("Stub")),

    /** French. */
    FRENCH(throw Exception("Stub")),

    /** Unknown. */
    UNKNOWN(throw Exception("Stub"));

    /**
     * The string resource of the app naming it.
     *
     * @since extensions-lib 1.5
     */
    fun getRes() : Int = throw Exception("Stub")
    companion object {

        /**
         * The language named [name], like [valueOf], or null.
         *
         * @since extensions-lib 1.5
         */
        fun getLangByName(name : String) : Lang? = throw Exception("Stub")

        /**
         * The language named [value], or [UNKNOWN].
         *
         * @since extensions-lib 1.5
         */
        fun valueOfOrDefault(value : String?) : Lang {
            throw Exception("Stub")
        }

        /**
         * Every language but [UNKNOWN].
         *
         * @since extensions-lib 1.5
         */
        fun getAllLangs(): Set<Lang> = throw Exception("Stub")

        /**
         * The names of these languages.
         *
         * @since extensions-lib 1.5
         */
        fun Set<Lang>.toPref() : Set<String> = throw Exception("Stub")
    }

}
