package com.sf.tadami.ui.tabs.more.settings.screens.data.backup

import com.sf.tadami.BackgroundJobs
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.backup.RestoreOptions
import com.sf.tadami.notifications.backup.BackupFileValidator
import com.sf.tadami.utils.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RestoreBackupViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val uri: String = checkNotNull(savedStateHandle["uri"])

    private val _uiState = MutableStateFlow(RestoreBackupUiState())
    val uiState: StateFlow<RestoreBackupUiState> = _uiState.asStateFlow()

    init {
        // Decodes the whole backup file: too slow for the main thread
        viewModelScope.launchIO {
            validate(uri)
        }
    }


    fun toggle(setter: (RestoreOptions, Boolean) -> RestoreOptions, enabled: Boolean) {
        _uiState.update {
            it.copy(
                options = setter(it.options, enabled),
            )
        }
    }

    fun startRestore() {
        Injekt.get<BackgroundJobs>().startRestore(
            uri = uri,
            options = uiState.value.options,
        )
    }

    private fun validate(uri: String) {
        val results = try {
            BackupFileValidator().validate(uri)
        } catch (e: Exception) {
            setError(
                error = InvalidRestore(uri, e.message.toString()),
                canRestore = false,
            )
            return
        }

        if (results.missingSources.isNotEmpty()) {
            setError(
                error = MissingRestoreComponents(uri, results.missingSources),
                canRestore = true,
            )
            return
        }

        setError(error = null, canRestore = true)
    }

    private fun setError(error: Any?, canRestore: Boolean) {
        _uiState.update {
            it.copy(
                error = error,
                canRestore = canRestore,
            )
        }
    }
}

data class MissingRestoreComponents(
    val uri: String,
    val sources: List<String>
)

data class InvalidRestore(
    val uri: String? = null,
    val message: String,
)

@Immutable
data class RestoreBackupUiState(
    val error: Any? = null,
    val canRestore: Boolean = false,
    val options: RestoreOptions = RestoreOptions(),
)

