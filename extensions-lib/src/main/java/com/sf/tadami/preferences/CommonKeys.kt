package com.sf.tadami.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

/**
 * The keys the app reads from the [com.sf.tadami.source.ConfigurableSource.dataStore] of a source.
 *
 * @since extensions-lib 1.5
 */
object CommonKeys {
    /**
     * Whether the player shows the episode tooltip of the source when an episode starts, see
     * [com.sf.tadami.source.AnimeCatalogueSource.supportEpisodeTooltip].
     *
     * @since extensions-lib 1.5
     */
    val SHOULD_SHOW_EPISODE_TOOLTIP = booleanPreferencesKey("player_should_show_episode_tooltip")
}
