package com.sf.tadami.ui.tabs.browse.tabs.extensions.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.extension.ExtensionManager
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.source.online.AnimeHttpSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionDetailsViewModel(
    stateHandle: SavedStateHandle
) : ViewModel() {

    private val pkgName = checkNotNull(stateHandle.get<String>("pkgName"))
    private val network: NetworkHelper = Injekt.get()
    private val extensionManager: ExtensionManager = Injekt.get()

    private var _uiState = MutableStateFlow(ExtensionDetailsUiState())
    var uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                extensionManager.installedExtensionsFlow
                    .map { it.firstOrNull { extension -> extension.pkgName == pkgName } }
                    .collectLatest { extension ->
                        if (extension == null) {
                            return@collectLatest
                        }
                        _uiState.update { state ->
                            state.copy(extension = extension)
                        }
                    }
            }
        }
    }

    fun clearCookies() {
        val extension = _uiState.value.extension ?: return

        val urls = extension.sources
            .filterIsInstance<AnimeHttpSource>()
            .mapNotNull { it.baseUrl.takeUnless { url -> url.isEmpty() } }
            .distinct()

        val cleared = urls.sumOf {
            try {
                network.cookieManager.remove(it.toHttpUrl())
            } catch (e: Exception) {
                Log.d("ClearCookies","Failed to clear cookies for $it")
                0
            }
        }

        Log.i("ClearCookies","Cleared $cleared cookies for: ${urls.joinToString()}")
    }

    fun uninstallExtension() {
        val extension = _uiState.value.extension ?: return
        extensionManager.uninstallExtension(extension)
    }
}
