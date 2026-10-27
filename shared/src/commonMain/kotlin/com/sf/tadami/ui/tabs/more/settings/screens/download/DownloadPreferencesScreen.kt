package com.sf.tadami.ui.tabs.more.settings.screens.download

import com.sf.tadami.storage.StorageFiles
import org.jetbrains.compose.resources.StringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import com.sf.tadami.storage.StorageFile
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.providers.FoldersProvider
import com.sf.tadami.preferences.download.DownloadPreferences
import com.sf.tadami.preferences.model.DataStoreState
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.storage.StoragePreferences
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.utils.displayablePath
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DownloadPreferencesScreen(
    navController: NavHostController
) : PreferenceScreen {
    override val title: StringResource = Res.string.preferences_download_title

    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @Composable
    override fun getPreferences(): List<Preference> {
        val downloadPreferencesState = rememberDataStoreState(DownloadPreferences)
        val downloadPreferences by downloadPreferencesState.value.collectAsState()

        val storagePreferencesState = rememberDataStoreState(StoragePreferences)
        val storagePreferences by storagePreferencesState.value.collectAsState()

        return listOf(
            getDownloadsGroup(prefState = downloadPreferencesState, prefs = downloadPreferences),
            getLocationPref(storagePreferences),
        )
    }

    @Composable
    private fun getDownloadsGroup(
        prefState: DataStoreState<DownloadPreferences>,
        prefs: DownloadPreferences
    ): Preference.PreferenceCategory {
        val downloadManager: TadamiDownloadManager = remember { Injekt.get() }
        return Preference.PreferenceCategory(
            title = stringResource(Res.string.preferences_download_title),
            preferenceItems = listOf(
                Preference.PreferenceItem.TogglePreference(
                    title = stringResource(Res.string.preferences_download_only_over_wifi),
                    value = prefs.onlyOverWifi,
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                onlyOverWifi = it
                            )
                        )
                        // The network constraint is read when the work is enqueued
                        downloadManager.restartIfRunning()
                        true
                    }
                ),
                Preference.PreferenceItem.SelectPreference(
                    value = prefs.parallelDownloads,
                    items = (DownloadPreferences.ParallelDownloadsItems.MIN..DownloadPreferences.ParallelDownloadsItems.MAX)
                        .associateWith { "$it" },
                    title = stringResource(Res.string.preferences_download_parallel),
                    subtitle = stringResource(Res.string.preferences_download_parallel_subtitle),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                parallelDownloads = it
                            )
                        )
                        true
                    }
                ),
                Preference.PreferenceItem.TogglePreference(
                    title = stringResource(Res.string.preferences_download_delete_after_watched),
                    value = prefs.deleteAfterWatched,
                    subtitle = stringResource(Res.string.preferences_download_delete_after_watched_subtitle),
                    onValueChanged = {
                        prefState.setValue(
                            prefs.copy(
                                deleteAfterWatched = it
                            )
                        )
                        true
                    }
                ),
            )
        )
    }

    /** Read only: the downloads follow the storage location of the data settings. */
    @Composable
    private fun getLocationPref(
        prefs: StoragePreferences,
        foldersProviders: FoldersProvider = Injekt.get()
    ): Preference.PreferenceItem.TextPreference {
        val location = remember(prefs) {
            if (prefs.storageDir == foldersProviders.path()) return@remember null
            Injekt.get<StorageFiles>().fromUri(prefs.storageDir)?.displayablePath?.let { "$it/$DOWNLOADS_DIR" }
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(Res.string.preferences_download_location),
            subtitle = location ?: stringResource(Res.string.no_location_set),
        )
    }

    private companion object {
        const val DOWNLOADS_DIR = "downloads"
    }
}
