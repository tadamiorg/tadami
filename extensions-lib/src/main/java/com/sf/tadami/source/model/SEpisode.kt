package com.sf.tadami.source.model

/**
 * An episode as a source gives it. Create one with [create].
 *
 * @since extensions-lib 1.5
 */
interface SEpisode {
    /**
     * Its URL, unique in its source, usually without the domain (see
     * [com.sf.tadami.source.online.AnimeHttpSource.setUrlWithoutDomain]): the app identifies the
     * episode by it, so it must not change.
     *
     * @since extensions-lib 1.5
     */
    var url : String

    /**
     * Its name.
     *
     * @since extensions-lib 1.5
     */
    var name : String

    /**
     * Its number, -1 when unknown.
     *
     * @since extensions-lib 1.5
     */
    var episodeNumber : Float

    /**
     * When it was released, in epoch milliseconds, 0 when unknown.
     *
     * @since extensions-lib 1.5
     */
    var dateUpload : Long

    /**
     * The languages it's available in, as shown to the user, e.g. "VF, VOSTFR".
     *
     * @since extensions-lib 1.5
     */
    var languages: String?

    companion object{
        /**
         * A new episode, to fill: [url] and [name] must be set.
         *
         * @since extensions-lib 1.5
         */
        fun create() : SEpisode {
            throw Exception("Stub")
        }
    }
}
