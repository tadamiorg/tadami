package com.sf.tadami.source

import com.sf.tadami.source.model.SAnime

/**
 * A page of anime of a source.
 *
 * @property hasNextPage whether the app can ask for the next page
 * @since extensions-lib 1.5
 */
data class AnimesPage(val animes : List<SAnime>, val hasNextPage : Boolean)
