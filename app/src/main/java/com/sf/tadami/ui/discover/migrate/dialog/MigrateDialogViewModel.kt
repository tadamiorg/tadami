package com.sf.tadami.ui.discover.migrate.dialog

import android.util.Log
import androidx.lifecycle.ViewModel
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.UpdateAnime
import com.sf.tadami.domain.episode.toUpdateEpisode
import com.sf.tadami.source.Source
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.utils.awaitSingleOrError
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

    suspend fun migrateManga(
        oldAnime: Anime,
        newAnime: Anime,
        replace: Boolean,
        flags: Int,
    ) {
        val source = sourceManager.get(newAnime.source) ?: return
        val prevSource = sourceManager.get(oldAnime.source)

        _state.update { it.copy(migrationState = MigrationState.RUNNING) }

        try {
            val episodes = source.fetchEpisodesList(newAnime).awaitSingleOrError()

            migrateMangaInternal(
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

    private suspend fun migrateMangaInternal(
        oldSource: Source?,
        newSource: Source,
        oldAnime: Anime,
        newAnime: Anime,
        sourceEpisodes: List<SEpisode>,
        replace: Boolean,
        flags: Int,
    ) {
        val migrateChapters = MigrationFlags.hasEpisodes(flags)
        val deleteDownloaded = MigrationFlags.hasDeleteDownloaded(flags)

        try {
            updateAnimeInteractor.awaitEpisodesSyncFromSource(newAnime, sourceEpisodes, newSource)
        } catch (_: Exception) {
            // Worst case, chapters won't be synced
        }

        // Update chapters read, bookmark and dateFetch
        if (migrateChapters) {
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

        /*// Delete downloaded
        if (deleteDownloaded) {
            if (oldSource != null) {
                downloadManager.deleteManga(oldAnime, oldSource)
            }
        }*/

        if (replace) {
            animeRepository.updateAnime(UpdateAnime(oldAnime.id, favorite = false, dateAdded = 0))
        }

        animeRepository.updateAnime(
            UpdateAnime(
                id = newAnime.id,
                favorite = true,
                episodeFlags = oldAnime.episodeFlags,
                dateAdded = if (replace) oldAnime.dateAdded else Instant.now().toEpochMilli(),
            ),
        )
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