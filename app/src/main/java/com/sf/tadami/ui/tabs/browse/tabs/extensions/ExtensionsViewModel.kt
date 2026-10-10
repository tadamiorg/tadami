package com.sf.tadami.ui.tabs.browse.tabs.extensions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.R
import com.sf.tadami.data.interactors.extension.GetExtensionsByType
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.extension.model.InstallStep
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.source.online.AnimeHttpSource
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import com.sf.tadami.utils.launchIO
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class ExtensionsViewModel : ViewModel() {

    private val extensionManager: ExtensionManager = Injekt.get()
    private val getExtensions: GetExtensionsByType = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private val _uiState: MutableStateFlow<ExtensionsUiState> = MutableStateFlow(ExtensionsUiState())
    val uiState = _uiState.asStateFlow()

    private var _currentDownloads = MutableStateFlow<Map<String, InstallStep>>(hashMapOf())

    init {
        val extensionMapper: (Map<String, InstallStep>) -> ((Extension) -> ExtensionUiModel.Item) =
            { map ->
                {
                    ExtensionUiModel.Item(it, map[it.uiKey] ?: InstallStep.Idle)
                }
            }
        val queryFilter: (String) -> ((Extension) -> Boolean) = { query ->
            filter@{ extension ->
                if (query.isEmpty()) return@filter true
                query.split(",").any { _input ->
                    val input = _input.trim()
                    if (input.isEmpty()) return@any false
                    when (extension) {
                        is Extension.Available -> {
                            extension.sources.any {
                                it.name.contains(input, ignoreCase = true) ||
                                        it.baseUrl.contains(input, ignoreCase = true) ||
                                        it.id == input.toLongOrNull()
                            } || extension.name.contains(input, ignoreCase = true)
                        }

                        is Extension.Installed -> {
                            extension.sources.any {
                                it.name.contains(input, ignoreCase = true) ||
                                        it.id == input.toLongOrNull() ||
                                        if (it is AnimeHttpSource) {
                                            it.baseUrl.contains(input, ignoreCase = true)
                                        } else false
                            } || extension.name.contains(input, ignoreCase = true)
                        }
                    }
                }
            }
        }

        viewModelScope.launchIO {
            combine(
                uiState.map { it.searchQuery }.distinctUntilChanged().debounce(250L),
                _currentDownloads,
                getExtensions.subscribe(),
            ) { query, downloads, (_updates, _installed, _available) ->

                val itemsGroups: ItemGroups = mutableMapOf()

                val updates =
                    _updates.filter(queryFilter(query)).map(extensionMapper(downloads))
                if (updates.isNotEmpty()) {
                    itemsGroups[ExtensionUiModel.Header.Resource(R.string.ext_updates_pending)] =
                        updates
                }

                val installed =
                    _installed.filter(queryFilter(query)).map(extensionMapper(downloads))
                if (installed.isNotEmpty()) {
                    itemsGroups[ExtensionUiModel.Header.Resource(R.string.ext_installed)] =
                        installed
                }

                val languagesWithExtensions = _available
                    .filter(queryFilter(query))
                    .groupBy { it.lang }
                    .toSortedMap()
                    .map { (lang, exts) ->
                        ExtensionUiModel.Header.Text(lang.getRes()) to exts.map(
                            extensionMapper(
                                downloads
                            )
                        )
                    }
                if (languagesWithExtensions.isNotEmpty()) {
                    itemsGroups.putAll(languagesWithExtensions)
                }
                itemsGroups

            }.collectLatest {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        items = it,
                    )
                }
            }
        }

        viewModelScope.launchIO { findAvailableExtensions() }

        extensionManager.failedExtensionsFlow.onEach { failed ->
            _uiState.update { state -> state.copy(failedExtensions = failed) }
        }.launchIn(viewModelScope)

        extensionManager.redundantPackagesFlow.onEach { packages ->
            _uiState.update { state -> state.copy(redundantPackages = packages) }
        }.launchIn(viewModelScope)

        dataStore.getPreferencesGroupAsFlow(SourcesPreferences).onEach {
            _uiState.update { state -> state.copy(updates = it.extensionUpdatesCount) }
        }.launchIn(viewModelScope)

    }

    fun search(query: String?) {
        _uiState.update {
            it.copy(searchQuery = query ?: "")
        }
    }

    fun updateAllExtensions() {
        viewModelScope.launchIO {
            _uiState.value.items.values.flatten()
                .map { it.extension }
                .filterIsInstance<Extension.Installed>()
                .filter { it.hasUpdate }
                .forEach(::updateExtension)
        }
    }

    fun installExtension(extension: Extension.Available) {
        viewModelScope.launchIO {
            extensionManager.installExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun updateExtension(extension: Extension.Installed) {
        viewModelScope.launchIO {
            extensionManager.updateExtension(extension).collectToInstallUpdate(extension)
        }
    }

    fun cancelInstallUpdateExtension(extension: Extension) {
        extensionManager.cancelInstallUpdateExtension(extension)
    }

    private fun addDownloadState(extension: Extension, installStep: InstallStep) {
        _currentDownloads.update { it + Pair(extension.uiKey, installStep) }
    }

    private fun removeDownloadState(extension: Extension) {
        _currentDownloads.update { it - extension.uiKey }
    }

    private suspend fun Flow<InstallStep>.collectToInstallUpdate(extension: Extension) =
        this
            .onEach { installStep -> addDownloadState(extension, installStep) }
            .onCompletion { removeDownloadState(extension) }
            .collect()

    fun uninstallExtension(extension: Extension) {
        extensionManager.uninstallExtension(extension)
    }

    fun retryFailedExtension(pkgName: String) {
        extensionManager.retryFailedExtension(pkgName)
    }

    /** True while [removeAllLegacyPackages] is still working through the list. */
    private var cleaningUpLegacy = false
    private var lastRequestedLegacyUninstall: String? = null

    /**
     * Re-checks which old extension packages are still installed, and advances the cleanup if one
     * is running. Called when the tab resumes — Android confirms each uninstall in its own screen
     * and reports nothing back, so coming back to the foreground is the only signal available.
     */
    fun refreshRedundantPackages() {
        viewModelScope.launchIO {
            if (cleaningUpLegacy) continueLegacyCleanup() else extensionManager.refreshRedundantPackages()
        }
    }

    /**
     * Removes every leftover extension package, one system dialog at a time. They can't be batched
     * — Android has no bulk uninstall — so this queues them behind a single confirmation instead of
     * making the user find each one.
     */
    fun removeAllLegacyPackages() {
        cleaningUpLegacy = true
        lastRequestedLegacyUninstall = null
        viewModelScope.launchIO { continueLegacyCleanup() }
    }

    private suspend fun continueLegacyCleanup() {
        val next = extensionManager.refreshRedundantPackages().firstOrNull()
        when (next) {
            // Everything is gone.
            null -> cleaningUpLegacy = false
            // Still at the head of the list after we asked for it means the user declined; asking
            // again would loop the same dialog forever.
            lastRequestedLegacyUninstall -> cleaningUpLegacy = false
            else -> {
                lastRequestedLegacyUninstall = next
                extensionManager.requestLegacyUninstall(next)
            }
        }
    }

    /** Accepts an unrecognised signing certificate, then loads the extension it belongs to. */
    fun trustExtension(pkgName: String, fingerprint: String) {
        extensionManager.trustExtension(pkgName, fingerprint)
    }

    /** Failed extensions never produced an [Extension], so they can only be addressed by package. */
    fun uninstallExtension(pkgName: String) {
        extensionManager.uninstallExtension(pkgName)
    }

    fun findAvailableExtensions() {
        viewModelScope.launchIO {
            _uiState.update { it.copy(isRefreshing = true) }

            extensionManager.findAvailableExtensions()

            // Fake slower refresh so it doesn't seem like it's not doing anything
            delay(1.seconds)

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}