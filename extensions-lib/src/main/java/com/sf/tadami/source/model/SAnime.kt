package com.sf.tadami.source.model

/**
 * An anime as a source gives it. Create one with [create].
 *
 * @since extensions-lib 1.5
 */
interface SAnime{
    /**
     * Its URL, unique in its source, usually without the domain (see
     * [com.sf.tadami.source.online.AnimeHttpSource.setUrlWithoutDomain]): the app identifies the
     * anime by it, so it must not change.
     *
     * @since extensions-lib 1.5
     */
    var url : String

    /**
     * Its title.
     *
     * @since extensions-lib 1.5
     */
    var title : String

    /**
     * Original/canonical site title used for search & migration. Set this to the untouched title
     * when [title] is rewritten (e.g. by appending a season name). When null, consumers fall back
     * to [title].
     *
     * @since extensions-lib 1.5
     */
    var rawTitle : String?

    /**
     * Language of this entry when the site lists each language as its own anime (e.g. "VF",
     * "VOSTFR"). The app shows it as a badge next to the title on every screen, so keep it out of
     * [title]. Optional; an extension that sets it needs lib 1.6 (`set("apiVersion", "1.6")`).
     *
     * @since extensions-lib 1.6
     */
    var language : String?

    /**
     * The URL of its cover.
     *
     * @since extensions-lib 1.5
     */
    var thumbnailUrl : String?

    /**
     * When it was released, as the site gives it.
     *
     * @since extensions-lib 1.5
     */
    var release : String?

    /**
     * Its studio.
     *
     * @since extensions-lib 1.5
     */
    var studio : String?

    /**
     * Its author.
     *
     * @since extensions-lib 1.5
     */
    var author : String?

    /**
     * Its publication status, [SAnimeStatus.UNKNOWN] by default.
     *
     * @since extensions-lib 1.5
     */
    var status : SAnimeStatus

    /**
     * Its synopsis.
     *
     * @since extensions-lib 1.5
     */
    var description : String?

    /**
     * Its genres.
     *
     * @since extensions-lib 1.5
     */
    var genres : List<String>?

    /**
     * Whether its details are complete: set it once they were fetched.
     *
     * @since extensions-lib 1.5
     */
    var initialized: Boolean

    companion object{
        /**
         * A new anime, to fill: [url] and [title] must be set.
         *
         * @since extensions-lib 1.5
         */
        fun create() : SAnime {
            throw Exception("Stub!")
        }
    }
}
