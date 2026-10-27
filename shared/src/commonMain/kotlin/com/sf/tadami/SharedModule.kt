package com.sf.tadami

import com.sf.tadami.data.AndroidDatabaseHandler
import com.sf.tadami.data.DataBaseHandler
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.anime.AnimeRepositoryImpl
import com.sf.tadami.data.animeStatusAdapter
import com.sf.tadami.data.category.CategoryRepository
import com.sf.tadami.data.category.CategoryRepositoryImpl
import com.sf.tadami.data.dateColumnAdapter
import com.sf.tadami.data.download.DownloadProvider
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.episode.EpisodeRepositoryImpl
import com.sf.tadami.data.history.HistoryRepository
import com.sf.tadami.data.history.HistoryRepositoryImpl
import com.sf.tadami.data.interactors.anime.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.anime.FetchIntervalInteractor
import com.sf.tadami.data.interactors.anime.GetAnime
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.category.CreateCategoryWithName
import com.sf.tadami.data.interactors.category.DeleteCategory
import com.sf.tadami.data.interactors.category.GetCategories
import com.sf.tadami.data.interactors.category.RenameCategory
import com.sf.tadami.data.interactors.category.ReorderCategory
import com.sf.tadami.data.interactors.category.SetAnimeCategories
import com.sf.tadami.data.interactors.category.SetCategorySort
import com.sf.tadami.data.interactors.extension.GetExtensionLanguages
import com.sf.tadami.data.interactors.extension.GetExtensionsByType
import com.sf.tadami.data.interactors.history.GetHistoryInteractor
import com.sf.tadami.data.interactors.history.GetNextEpisodeInteractor
import com.sf.tadami.data.interactors.history.RemoveHistoryInteractor
import com.sf.tadami.data.interactors.history.UpdateHistoryInteractor
import com.sf.tadami.data.interactors.library.LibraryInteractor
import com.sf.tadami.data.interactors.sources.GetEnabledSources
import com.sf.tadami.data.interactors.sources.GetLanguagesWithSources
import com.sf.tadami.data.interactors.sources.GetSourcesWithNonLibraryAnime
import com.sf.tadami.data.interactors.updates.GetUpdatesInteractor
import com.sf.tadami.data.listOfStringsAdapter
import com.sf.tadami.data.sources.SourceRepository
import com.sf.tadami.data.sources.SourceRepositoryImpl
import com.sf.tadami.data.sources.StubSourceRepository
import com.sf.tadami.data.sources.StubSourceRepositoryImpl
import com.sf.tadami.data.updates.UpdatesRepository
import com.sf.tadami.data.updates.UpdatesRepositoryImpl
import com.sf.tadami.domain.storage.StorageManager
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.tabs.browse.SourceManagerImplementation
import data.Anime
import data.History
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

/**
 * The singletons of the shared code, in the Injekt module of each app with what the platform provides:
 * its SqlDriver, NetworkHelper, StorageFiles, ExtensionPlatform, PlatformUi, DownloadPlatform and
 * BackgroundJobs.
 */
@OptIn(ExperimentalSerializationApi::class)
fun InjektRegistrar.registerSharedInjectables() {
    addSingletonFactory {
        Database(
            driver = get(),
            AnimeAdapter = Anime.Adapter(
                genresAdapter = listOfStringsAdapter,
                statusAdapter = animeStatusAdapter
            ),
            HistoryAdapter = History.Adapter(
                seen_atAdapter = dateColumnAdapter
            )
        )
    }

    addSingletonFactory<ProtoBuf> {
        ProtoBuf
    }

    addSingletonFactory<DataBaseHandler> {
        AndroidDatabaseHandler(get())
    }

    addSingletonFactory { ExtensionManager() }

    // Sources
    addSingletonFactory<StubSourceRepository>{
        StubSourceRepositoryImpl(get())
    }

    // Storage

    addSingletonFactory {
        StorageManager(get())
    }

    // Downloads

    addSingletonFactory {
        DownloadProvider()
    }

    addSingletonFactory {
        TadamiDownloadManager()
    }

    addSingletonFactory<SourceManager>{
        SourceManagerImplementation(get(),get())
    }

    // DataSources

    addSingletonFactory<AnimeRepository>{
        AnimeRepositoryImpl(get(),get())
    }

    addSingletonFactory<EpisodeRepository>{
        EpisodeRepositoryImpl(get())
    }

    addSingletonFactory<SourceRepository>{
        SourceRepositoryImpl(get(),get())
    }

    addSingletonFactory<HistoryRepository>{
        HistoryRepositoryImpl(get())
    }

    addSingletonFactory<UpdatesRepository>{
        UpdatesRepositoryImpl(get())
    }

    addSingletonFactory<CategoryRepository>{
        CategoryRepositoryImpl(get())
    }

    // Anime interactors

    addSingletonFactory {
        UpdateAnimeInteractor(get(),get(),get())
    }

    addSingletonFactory {
        AnimeWithEpisodesInteractor(get(),get())
    }

    addSingletonFactory {
        FetchIntervalInteractor(get())
    }

    addSingletonFactory {
        GetNextEpisodeInteractor(get(),get())
    }

    addSingletonFactory {
        RemoveHistoryInteractor(get())
    }

    addSingletonFactory {
        GetAnime(get())
    }

    // Library interactors

    addSingletonFactory {
        LibraryInteractor(get())
    }

    // Category interactors

    addSingletonFactory {
        GetCategories(get())
    }

    addSingletonFactory {
        CreateCategoryWithName(get())
    }

    addSingletonFactory {
        RenameCategory(get())
    }

    addSingletonFactory {
        DeleteCategory(get(),get())
    }

    addSingletonFactory {
        ReorderCategory(get())
    }

    addSingletonFactory {
        SetAnimeCategories(get())
    }

    addSingletonFactory {
        SetCategorySort(get(),get())
    }

    // Sources interactors

    addSingletonFactory {
        GetSourcesWithNonLibraryAnime(get())
    }

    addSingletonFactory {
        GetEnabledSources(get(),get())
    }

    addSingletonFactory {
        GetLanguagesWithSources(get(),get())
    }

    // History interactors

    addSingletonFactory {
        GetHistoryInteractor(get())
    }

    addSingletonFactory {
        UpdateHistoryInteractor(get())
    }

    addSingletonFactory {
        RemoveHistoryInteractor(get())
    }

    // Updates Interactors

    addSingletonFactory {
        GetUpdatesInteractor(get())
    }

    // Extensions Interactors

    addSingletonFactory {
        GetExtensionsByType(get(),get())
    }

    addSingletonFactory {
        GetExtensionLanguages(get(),get())
    }

    addSingletonFactory {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
