package com.sf.tadami.ui.tabs.browse.tabs.extensions.repos

import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.BuildConfig
import com.sf.tadami.R
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.extension.api.ExtensionRepos
import com.sf.tadami.extension.api.FetchedRepo
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import com.sf.tadami.utils.launchIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionReposViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val extensionManager: ExtensionManager = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()

    /** The repos, the official one first. */
    val repos: StateFlow<List<String>> = dataStore.getPreferencesGroupAsFlow(SourcesPreferences)
        .map { preferences ->
            preferences.extensionsRepos.sortedWith(compareBy<String> { !ExtensionRepos.isOfficial(it) }.thenBy { it })
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val repoMetas = extensionManager.repoMetasFlow

    private val _dialog = MutableStateFlow<RepoDialog?>(null)
    val dialog = _dialog.asStateFlow()

    private var checkJob: Job? = null

    init {
        // Opened by a link to add a repo, see DiscoverRoutes.EXTENSION_REPOS
        savedStateHandle.get<String>("url")?.takeIf { it.isNotBlank() }?.let(::submit)
    }

    fun showAddDialog() {
        _dialog.value = RepoDialog.Add()
    }

    fun dismissDialog() {
        checkJob?.cancel()
        _dialog.value = null
    }

    /** Checks the repo [input] the user typed, then asks to confirm it, see [RepoDialog.ConfirmAdd]. */
    fun submit(input: String) {
        val candidates = when (val parsed = ExtensionRepos.parse(input, allowHttp = BuildConfig.DEBUG)) {
            ExtensionRepos.Input.Invalid -> return addError(input, R.string.ext_repo_error_invalid)
            ExtensionRepos.Input.NotHttps -> return addError(input, R.string.ext_repo_error_https)
            is ExtensionRepos.Input.Candidates -> parsed.urls
        }
        _dialog.value = RepoDialog.Add(input, checking = true)
        checkJob?.cancel()
        checkJob = viewModelScope.launchIO {
            val repos = dataStore.getPreferencesGroup(SourcesPreferences).extensionsRepos.map(ExtensionRepos::normalize)
            if (candidates.any { it in repos }) return@launchIO addError(input, R.string.ext_repo_error_duplicate)
            val repo = try {
                extensionManager.fetchRepo(candidates)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@launchIO addError(input, R.string.ext_repo_error_unreadable, e.message ?: e.javaClass.simpleName)
            }
            _dialog.value = RepoDialog.ConfirmAdd(repo)
        }
    }

    private fun addError(input: String, @StringRes error: Int, detail: String? = null) {
        _dialog.value = RepoDialog.Add(input, error = error, errorDetail = detail)
    }

    fun addRepo(repo: FetchedRepo, trustKey: Boolean) {
        _dialog.value = null
        viewModelScope.launchIO { extensionManager.addRepo(repo, trustKey) }
    }

    /** Asks to add the official repo back, once the user removed it. */
    fun addOfficialRepo() {
        submit(SourcesPreferences.DEFAULT_EXTENSIONS_REPO)
    }

    fun showRemoveDialog(repoUrl: String) {
        _dialog.value = RepoDialog.Remove(repoUrl)
    }

    fun removeRepo(repoUrl: String) {
        _dialog.value = null
        viewModelScope.launchIO { extensionManager.removeRepo(repoUrl) }
    }
}

sealed interface RepoDialog {
    /** The URL to add, [checking] while its index is read, with why it was refused otherwise. */
    data class Add(
        val input: String = "",
        val checking: Boolean = false,
        @StringRes val error: Int? = null,
        val errorDetail: String? = null,
    ) : RepoDialog

    /** The repo read, to be confirmed before it's added. */
    data class ConfirmAdd(val repo: FetchedRepo) : RepoDialog

    data class Remove(val repoUrl: String) : RepoDialog
}
