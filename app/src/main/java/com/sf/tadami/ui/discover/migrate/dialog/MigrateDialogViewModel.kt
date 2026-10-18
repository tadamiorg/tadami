package com.sf.tadami.ui.discover.migrate.dialog

import android.util.Log
import androidx.lifecycle.ViewModel
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.category.GetCategories
import com.sf.tadami.data.interactors.category.SetAnimeCategories
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.UpdateAnime
import com.sf.tadami.domain.anime.toSAnime
import com.sf.tadami.domain.episode.toUpdateEpisode
import com.sf.tadami.extension.util.callGuarded
import com.sf.tadami.source.Source
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.ui.tabs.browse.SourceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant

class MigrateDialogViewModel : ViewModel() {
    private val _state = MutableStateFlow(MigrateDialogUiState())
    val state = _state.asStateFlow()

    private val sourceManager: SourceManager = Injekt.get()
    private val updateAnimeInteractor: UpdateAnimeInteractor = Injekt.get()
    private val episodeRepository: EpisodeRepository = Injekt.get()
    private val animeRepository: AnimeRepository = Injekt.get()
    private val downloadManager: TadamiDownloadManager = Injekt.get()
    private val getCategories: GetCategories = Injekt.get()
    private val setAnimeCategories: SetAnimeCategories = Injekt.get()

    fun hasDownloads(anime: Anime): Boolean = downloadManager.getDownloadCount(anime) > 0

    suspend fun migrateAnime(
        oldAnime: Anime,
        newAnime: Anime,
        replace: Boolean,
        flags: Int,
    ) {
        val source = sourceManager.get(newAnime.source) ?: return
        val prevSource = sourceManager.get(oldAnime.source)

        _state.update { it.copy(migrationState = MigrationState.RUNNING) }

        try {
            val episodes = source.callGuarded { getEpisodeList(newAnime.toSAnime()) }

            migrateAnimeInternal(
                oldSource = prevSource,
                newSource = source,
                oldAnime = oldAnime,
                newAnime = newAnime,
                sourceEpisodes = episodes,
                replace = replace,
                flags = flags,
            )
        } catch (e: Throwable) {
            // Explicitly stop if an error occurred; the dialog normally gets popped at the end
            // anyway
            Log.d("Anime migration error",e.stackTraceToString())
            _state.update { it.copy(migrationState = MigrationState.ERRORED) }
        }
    }

    private suspend fun migrateAnimeInternal(
        oldSource: Source?,
        newSource: Source,
        oldAnime: Anime,
        newAnime: Anime,
        sourceEpisodes: List<SEpisode>,
        replace: Boolean,
        flags: Int,
    ) {
        val migrateEpisodes = MigrationFlags.hasEpisodes(flags)
        val migrateCategories = MigrationFlags.hasCategories(flags)
        val deleteDownloaded = MigrationFlags.hasDeleteDownloaded(flags)

        try {
            updateAnimeInteractor.awaitEpisodesSyncFromSource(newAnime, sourceEpisodes, newSource)
        } catch (_: Exception) {
            // Worst case, episodes won't be synced
        }

        // Update episodes seen, bookmark and dateFetch
        if (migrateEpisodes) {
            val prevAnimeEpisodes = episodeRepository.getEpisodesByAnimeId(oldAnime.id)
            val animeEpisodes = episodeRepository.getEpisodesByAnimeId(newAnime.id)

            val maxEpisodeSeen = prevAnimeEpisodes
                .filter { it.seen }
                .maxOfOrNull { it.episodeNumber }

            val updatedAnimeEpisodes = animeEpisodes.map { animeEpisode ->
                var updatedEpisode = animeEpisode

                val prevEpisode = prevAnimeEpisodes
                    .find { it.episodeNumber == updatedEpisode.episodeNumber }

                if (prevEpisode != null) {
                    updatedEpisode = updatedEpisode.copy(
                        dateFetch = prevEpisode.dateFetch
                    )
                }

                if (maxEpisodeSeen != null && updatedEpisode.episodeNumber <= maxEpisodeSeen) {
                    updatedEpisode = updatedEpisode.copy(seen = true)
                }


                updatedEpisode
            }

            val episodeUpdates = updatedAnimeEpisodes.map { it.toUpdateEpisode() }
            episodeRepository.updateAll(episodeUpdates)
        }

        // Before it joins the library, so it shows up in its tabs
        if (migrateCategories) {
            setAnimeCategories.await(newAnime.id, getCategories.await(oldAnime.id).map { it.id })
        }

        // The new entry first: stopped or failing halfway, the anime stays in the library
        val favorited = animeRepository.updateAnime(
            UpdateAnime(
                id = newAnime.id,
                favorite = true,
                episodeFlags = oldAnime.episodeFlags,
                dateAdded = if (replace) oldAnime.dateAdded else Instant.now().toEpochMilli(),
            ),
        )
        check(favorited) { "Couldn't add the new anime to the library" }

        if (replace) {
            animeRepository.updateAnime(UpdateAnime(oldAnime.id, favorite = false, dateAdded = 0))
            // Not moved to the new entry: its episodes have other urls, so other folders
            if (deleteDownloaded) {
                // Its queued downloads too
                downloadManager.deleteAnime(oldAnime)
            } else {
                downloadManager.cancelQueuedDownloads(
                    downloadManager.queueState.value.filter { it.anime.id == oldAnime.id },
                )
            }
        }
    }
}

data class MigrateDialogUiState(
    var isOpened: Boolean = false,
    var migrationState: MigrationState = MigrationState.IDLE
)

enum class MigrationState {
    RUNNING,
    IDLE,
    ERRORED
}