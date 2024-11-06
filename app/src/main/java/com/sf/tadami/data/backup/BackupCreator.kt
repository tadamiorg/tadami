package com.sf.tadami.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.hippo.unifile.UniFile
import com.sf.tadami.R
import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.data.backup.models.*
import com.sf.tadami.data.episode.EpisodeMapper
import com.sf.tadami.data.interactors.history.GetHistoryInteractor
import com.sf.tadami.data.interactors.library.LibraryInteractor
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.notifications.backup.BackupFileValidator
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.source.Source
import com.sf.tadami.source.online.ConfigurableParsedHttpAnimeSource
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.tabs.more.settings.screens.data.backup.BackupSerializer
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.getDataStoreValues
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import okio.buffer
import okio.gzip
import okio.sink
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.FileOutputStream
import java.time.Instant

class BackupCreator(
    private val context: Context,
    private val isAutoBackup: Boolean,
) {

    private val handler: DataBaseHandler = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private val getHistory: GetHistoryInteractor = Injekt.get()
    private val sourceManager: SourceManager = Injekt.get()
    private val getLibary: LibraryInteractor = Injekt.get()


    @OptIn(ExperimentalSerializationApi::class)
    internal val parser = ProtoBuf

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun createBackup(uri: Uri, options: BackupOptions): String {
        var file: UniFile? = null
        try {
            file = if (isAutoBackup) {
                // Get dir of file and create
                val dir = UniFile.fromUri(context, uri)

                // Delete older backups
                dir?.listFiles { _, filename -> Backup.filenameRegex.matches(filename) }
                    .orEmpty()
                    .sortedByDescending { it.name }
                    .drop(MAX_AUTO_BACKUPS - 1)
                    .forEach { it.delete() }

                // Create new file to place backup
                dir?.createFile(Backup.getFilename())
            } else {
                UniFile.fromUri(context, uri)
            }

            if (file == null || !file.isFile) {
                throw IllegalStateException(context.getString(R.string.create_backup_file_error))
            }

            val databaseAnime = getLibary.await()
            val backup = Backup(
                backupAnimes(databaseAnime, options),
                backupAppPreferences(options),
                backupSources(databaseAnime),
                backupSourcePreferences(options)
            )

            val byteArray = parser.encodeToByteArray(BackupSerializer, backup)
            if (byteArray.isEmpty()) {
                throw IllegalStateException(context.getString(R.string.empty_backup_error))
            }

            file.openOutputStream().also {
                // Force overwrite old file
                (it as? FileOutputStream)?.channel?.truncate(0)
            }.sink().gzip().buffer().use { it.write(byteArray) }
            val fileUri = file.uri

            // Make sure it's a valid backup file
            BackupFileValidator().validate(context, fileUri)

            if (isAutoBackup) {
                dataStore.editPreference(Instant.now().toEpochMilli(),BackupPreferences.AUTO_BACKUP_LAST_TIMESTAMP)
            }

            return fileUri.toString()
        } catch (e: Exception) {
            Log.e("BackupCreator", e.stackTraceToString())
            file?.delete()
            throw e
        }
    }

    private suspend fun backupAnimes(
        animes: List<LibraryAnime>,
        options: BackupOptions
    ): List<BackupAnime> {
        if (!options.libraryEntries) return emptyList()
        return animes.map {
            backupAnime(it, options)
        }
    }

    private suspend fun backupAnime(anime: LibraryAnime, options: BackupOptions): BackupAnime {
        // Entry for this anime
        val animeObject = BackupAnime.copyFrom(anime)

        // Check if user wants episode information in backup
        if (options.episodes) {
            // Backup all the episodes
            handler.awaitList {
                episodeQueries.getEpisodesByAnimeId(
                    animeId = anime.id,
                    mapper = EpisodeMapper::mapBackupEpisode,
                )
            }
                .takeUnless(List<BackupEpisode>::isEmpty)
                ?.let { animeObject.episodes = it }
        }

        // Check if user wants history information in backup
        if (options.history) {
            val historyByAnimeId = getHistory.await(anime.id)
            if (historyByAnimeId.isNotEmpty()) {
                val history = historyByAnimeId.map { history ->
                    val episode =
                        handler.awaitOne { episodeQueries.getEpisodeById(history.episodeId) }
                    BackupHistory(episode.url, history.seenAt?.time ?: 0L)
                }
                if (history.isNotEmpty()) {
                    animeObject.history = history
                }
            }
        }

        return animeObject
    }

    private suspend fun backupAppPreferences(options: BackupOptions): List<BackupPreference> {
        if (!options.appSettings) return emptyList()

        return dataStore.getDataStoreValues().asMap().toBackupPreferences()
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<Preferences.Key<*>, Any>.toBackupPreferences(): List<BackupPreference> {
        return this.filterKeys {
            !CustomPreferences.isPrivate(it.name) && !CustomPreferences.isAppState(it.name)
        }
            .mapNotNull { (key, value) ->
                when (value) {
                    is Int -> BackupPreference(key.name, IntPreferenceValue(value))
                    is Long -> BackupPreference(key.name, LongPreferenceValue(value))
                    is Float -> BackupPreference(key.name, FloatPreferenceValue(value))
                    is String -> BackupPreference(key.name, StringPreferenceValue(value))
                    is Boolean -> BackupPreference(key.name, BooleanPreferenceValue(value))
                    is Set<*> -> (value as? Set<String>)?.let {
                        BackupPreference(key.name, StringSetPreferenceValue(it))
                    }

                    else -> null
                }
            }
    }

    private suspend fun backupSourcePreferences(options: BackupOptions): List<BackupSourcePreferences> {
        if (!options.sourcesSettings) return emptyList()
        return sourceManager.getCatalogueSources()
            .filterIsInstance<ConfigurableParsedHttpAnimeSource<*>>()
            .map {
                BackupSourcePreferences(
                    it.id,
                    it.dataStore.getDataStoreValues().asMap().toBackupPreferences()
                )
            }
            .filter { it.prefs.isNotEmpty() }
    }

    private fun backupSources(animes: List<LibraryAnime>): List<BackupSource> {
        return animes
            .asSequence()
            .map(LibraryAnime::source)
            .distinct()
            .map(sourceManager::getOrStub)
            .map { it.toBackupSource() }
            .toList()
    }

    private fun Source.toBackupSource() =
        BackupSource(
            name = this.name,
            sourceId = this.id,
        )

    companion object {
        private const val MAX_AUTO_BACKUPS: Int = 4
    }
}