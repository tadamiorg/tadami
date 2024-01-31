package com.sf.tadami.data.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.R
import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.data.backup.models.BackupAnime
import com.sf.tadami.data.backup.models.BackupHistory
import com.sf.tadami.data.backup.models.BackupPreference
import com.sf.tadami.data.backup.models.BooleanPreferenceValue
import com.sf.tadami.data.backup.models.FloatPreferenceValue
import com.sf.tadami.data.backup.models.IntPreferenceValue
import com.sf.tadami.data.backup.models.LongPreferenceValue
import com.sf.tadami.data.backup.models.StringPreferenceValue
import com.sf.tadami.data.backup.models.StringSetPreferenceValue
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.interactors.anime.FetchIntervalInteractor
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.history.HistoryUpdate
import com.sf.tadami.notifications.backup.BackupCreateWorker
import com.sf.tadami.notifications.backup.BackupNotifier
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.utils.createFileInCacheDir
import com.sf.tadami.utils.editPreference
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import kotlin.math.max
import data.Anime as AnimeDb

class BackupRestorer(
    private val context: Context,
    private val notifier: BackupNotifier,
) {

    private val handler: DataBaseHandler = Injekt.get()
    private val updateAnime: UpdateAnimeInteractor = Injekt.get()
    private val episodeRepository: EpisodeRepository = Injekt.get()
    private val fetchInterval: FetchIntervalInteractor = Injekt.get()

    private val dataStore: DataStore<Preferences> = Injekt.get()

    private var now = ZonedDateTime.now()
    private var currentFetchWindow = fetchInterval.getWindow(now)

    private var restoreAmount = 0
    private var restoreProgress = 0

    /**
     * Mapping of source ID to source name from backup data
     */
    private var sourceMapping: Map<Long, String> = emptyMap()

    private val errors = mutableListOf<Pair<Date, String>>()

    suspend fun syncFromBackup(uri: Uri): Boolean {
        val startTime = System.currentTimeMillis()
        restoreProgress = 0
        errors.clear()

        if (!performRestore(uri)) {
            return false
        }

        val endTime = System.currentTimeMillis()
        val time = endTime - startTime

        val logFile = writeErrorLog()

        notifier.showRestoreComplete(time, errors.size, logFile.parent, logFile.name)

        return true
    }

    private fun writeErrorLog(): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("tadami_restore.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                file.bufferedWriter().use { out ->
                    errors.forEach { (date, message) ->
                        out.write("[${sdf.format(date)}] $message\n")
                    }
                }
                return file
            }
        } catch (e: Exception) {
            // Empty
        }
        return File("")
    }

    private suspend fun performRestore(uri: Uri): Boolean {
        val backup = BackupUtil.decodeBackup(context, uri)

        restoreAmount = backup.backupAnime.size + 1 // +3 for categories, app prefs, source prefs

        now = ZonedDateTime.now()
        currentFetchWindow = fetchInterval.getWindow(now)

        return coroutineScope {
            restoreAppPreferences(backup.backupPreferences)

            // Restore individual anime
            backup.backupAnime.forEach {
                if (!isActive) {
                    return@coroutineScope false
                }

                restoreAnime(it)
            }

            true
        }
    }

    private suspend fun restoreAnime(backupAnime: BackupAnime) {
        val anime = backupAnime.getAnimeImpl()
        val episodes = backupAnime.getEpisodeImpl()
        val history = backupAnime.history

        try {
            val dbAnime = getAnimeFromDatabase(anime.url, anime.source)
            val restoredAnime = if (dbAnime == null) {
                // Anime not in database
                restoreNonExistingAnime(anime, episodes,history)
            } else {
                // Anime in database
                // Copy information from anime already in database
                val updatedAnime = restoreExistingAnime(anime, dbAnime)
                // Fetch rest of anime information
                restoreUpdatedAnime(updatedAnime, episodes, history)
            }
            updateAnime.awaitUpdateFetchInterval(restoredAnime, now, currentFetchWindow)
        } catch (e: Exception) {
            val sourceName = sourceMapping[anime.source] ?: anime.source
            errors.add(Date() to "${anime.title} [$sourceName]: ${e.message}")
        }

        restoreProgress += 1

        showRestoreProgress(
            restoreProgress,
            restoreAmount,
            anime.title,
            context.getString(R.string.restoring_backup),
        )

    }

    private suspend fun getAnimeFromDatabase(url: String, source: Long): AnimeDb? {
        return handler.awaitOneOrNull { animeQueries.getBySourceAndUrl(url, source) }
    }

    private suspend fun restoreExistingAnime(anime: Anime, animeDb: AnimeDb): Anime {
        var updatedAnime = anime.copy(id = animeDb._id)
        updatedAnime = updatedAnime.copyFrom(animeDb)
        updateAnime(updatedAnime)
        return updatedAnime
    }

    private suspend fun updateAnime(anime: Anime): Long {
        handler.await {
            animeQueries.update(
                source = anime.source,
                url = anime.url,
                description = anime.description,
                release = anime.release,
                genres = anime.genres?.joinToString(separator = ", "),
                title = anime.title,
                status = anime.status,
                thumbnailUrl = anime.thumbnailUrl,
                favorite = anime.favorite,
                initialized = anime.initialized,
                animeId = anime.id,
                lastUpdate = anime.lastUpdate,
                nextUpdate = null,
                calculateInterval = null,
                episodeFlags = anime.episodeFlags,
                dateAdded = anime.dateAdded
            )
        }
        return anime.id
    }

    private suspend fun restoreNonExistingAnime(
        anime: Anime,
        episodes: List<Episode>,
        history: List<BackupHistory>
    ): Anime {
        val fetchedAnime = restoreAnime(anime)
        restoreEpisodes(fetchedAnime, episodes)
        restoreHistory(history)
        return fetchedAnime
    }

    private suspend fun restoreEpisodes(anime: Anime, episodes: List<Episode>) {
        val dbEpisodesByUrl = episodeRepository.getEpisodesByAnimeId(anime.id)
            .associateBy { it.url }

        val processed = episodes.map { episode ->
            var updatedEpisode = episode

            val dbEpisode = dbEpisodesByUrl[updatedEpisode.url]
            if (dbEpisode != null) {
                updatedEpisode = updatedEpisode
                    .copyFrom(dbEpisode)
                    .copy(
                        id = dbEpisode.id,
                        totalTime = dbEpisode.totalTime
                    )
                if (dbEpisode.seen && !updatedEpisode.seen) {
                    updatedEpisode = updatedEpisode.copy(
                        seen = true,
                        timeSeen = dbEpisode.timeSeen,
                    )
                } else if (updatedEpisode.timeSeen == 0L && dbEpisode.timeSeen != 0L) {
                    updatedEpisode = updatedEpisode.copy(
                        timeSeen = dbEpisode.timeSeen,
                    )
                }
            }

            updatedEpisode.copy(animeId = anime.id)
        }

        val (existingEpisodes, newEpisodes) = processed.partition { it.id > 0 }
        updateKnownEpisodes(existingEpisodes)
        insertEpisodes(newEpisodes)
    }

    /**
     * Inserts list of episodes
     */
    private suspend fun insertEpisodes(episodes: List<Episode>) {
        handler.await {
            episodes.forEach { episode ->
                episodeQueries.insert(
                    episode.animeId,
                    episode.url,
                    episode.name,
                    episode.episodeNumber.toDouble(),
                    episode.timeSeen,
                    episode.totalTime,
                    episode.dateFetch,
                    episode.dateUpload,
                    episode.seen,
                    episode.sourceOrder,
                    episode.languages
                )
            }
        }
    }

    /**
     * Updates a list of episodes with known database ids
     */
    private suspend fun updateKnownEpisodes(episodes: List<Episode>) {
        handler.await {
            episodes.forEach { episode ->
                episodeQueries.update(
                    animeId = null,
                    url = null,
                    name = null,
                    episodeNumber = null,
                    timeSeen = episode.timeSeen,
                    totalTime = episode.totalTime,
                    seen = episode.seen,
                    sourceOrder = null,
                    dateFetch = null,
                    dateUpload = null,
                    episodeId = episode.id,
                    languages = episode.languages

                )
            }
        }
    }

    private suspend fun restoreAnime(anime: Anime): Anime {
        return anime.copy(
            initialized = anime.description != null,
            id = insertAnime(anime),
        )
    }

    private suspend fun restoreUpdatedAnime(
        backupAnime: Anime,
        episodes: List<Episode>,
        history: List<BackupHistory>
    ): Anime {
        restoreEpisodes(backupAnime, episodes)
        restoreHistory(history)
        return backupAnime
    }

    private suspend fun insertAnime(anime: Anime): Long {
        return handler.await {
            animeQueries.insert(
                source = anime.source,
                url = anime.url,
                description = anime.description,
                genres = anime.genres,
                title = anime.title,
                status = anime.status,
                thumbnailUrl = anime.thumbnailUrl,
                favorite = anime.favorite,
                release = anime.release,
                initiliazed = anime.initialized,
                lastUpdate = anime.lastUpdate,
                nextUpdate = 0L,
                calculateInterval = 0L,
                episodeFlags = anime.episodeFlags,
                dateAdded = anime.dateAdded

            )
            animeQueries.selectLastInsertedRowId().executeAsOne()
        }
    }

    private suspend fun restoreAppPreferences(preferences: List<BackupPreference>) {
        restorePreferences(preferences, dataStore)

        LibraryUpdateWorker.setupTask(context)
        BackupCreateWorker.setupTask(context)

        restoreProgress += 1
        showRestoreProgress(
            restoreProgress,
            restoreAmount,
            context.getString(R.string.app_settings),
            context.getString(R.string.restoring_backup),
        )
    }

    private suspend fun restorePreferences(
        toRestore: List<BackupPreference>,
        preferenceStore: DataStore<Preferences>,
    ) {

        toRestore.forEach { (key, value) ->
            when (value) {
                is IntPreferenceValue -> {
                    preferenceStore.editPreference(value.value, intPreferencesKey(key))
                }

                is LongPreferenceValue -> {
                    preferenceStore.editPreference(value.value, longPreferencesKey(key))
                }

                is FloatPreferenceValue -> {
                    preferenceStore.editPreference(value.value, floatPreferencesKey(key))
                }

                is StringPreferenceValue -> {
                    preferenceStore.editPreference(value.value, stringPreferencesKey(key))
                }

                is BooleanPreferenceValue -> {
                    preferenceStore.editPreference(value.value, booleanPreferencesKey(key))
                }

                is StringSetPreferenceValue -> {
                    preferenceStore.editPreference(value.value, stringSetPreferencesKey(key))
                }
            }
        }
    }

    private suspend fun restoreHistory(history: List<BackupHistory>) {
        // List containing history to be updated
        val toUpdate = mutableListOf<HistoryUpdate>()
        for ((url, seenAt) in history) {
            var dbHistory = handler.awaitOneOrNull { historyQueries.getHistoryByEpisodeUrl(url) }
            // Check if history already in database and update
            if (dbHistory != null) {
                dbHistory = dbHistory.copy(
                    seen_at = Date(max(seenAt, dbHistory.seen_at?.time ?: 0L)),
                )
                toUpdate.add(
                    HistoryUpdate(
                        episodeId = dbHistory.episode_id,
                        seenAt = dbHistory.seen_at!!
                    ),
                )
            } else {
                // If not in database create
                handler
                    .awaitOneOrNull { episodeQueries.getEpisodeByUrl(url) }
                    ?.let {
                        toUpdate.add(
                            HistoryUpdate(
                                episodeId = it._id,
                                seenAt = Date(seenAt)
                            ),
                        )
                    }
            }
        }
        handler.await {
            toUpdate.forEach { payload ->
                historyQueries.upsert(
                    payload.episodeId,
                    payload.seenAt
                )
            }
        }
    }

    private fun showRestoreProgress(
        progress: Int,
        amount: Int,
        title: String,
        contentTitle: String
    ) {
        notifier.showRestoreProgress(title, contentTitle, progress, amount)
    }
}