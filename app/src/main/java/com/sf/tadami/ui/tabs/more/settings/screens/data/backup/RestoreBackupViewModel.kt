package com.sf.tadami.ui.tabs.more.settings.screens.data.backup

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sf.tadami.App
import com.sf.tadami.data.backup.RestoreOptions
import com.sf.tadami.notifications.backup.BackupFileValidator
import com.sf.tadami.notifications.backup.BackupRestoreWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RestoreBackupViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val uri: String = checkNotNull(savedStateHandle["uri"])

    private val _uiState = MutableStateFlow(RestoreBackupUiState())
    val uiState: StateFlow<RestoreBackupUiState> = _uiState.asStateFlow()

    init {
        validate(uri.toUri())
    }


    fun toggle(setter: (RestoreOptions, Boolean) -> RestoreOptions, enabled: Boolean) {
        _uiState.update {
            it.copy(
                options = setter(it.options, enabled),
            )
        }
    }

    fun startRestore() {
        val context = App.getAppContext() ?: return;
        BackupRestoreWorker.start(
            context = context,
            uri = uri.toUri(),
            options = uiState.value.options,
        )
    }

    private fun validate(uri: Uri) {
        val context = App.getAppContext() ?: return;
        val results = try {
            BackupFileValidator().validate(context, uri)
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
    val uri: Uri,
    val sources: List<String>
)

data class InvalidRestore(
    val uri: Uri? = null,
    val message: String,
)

@Immutable
data class RestoreBackupUiState(
    val error: Any? = null,
    val canRestore: Boolean = false,
    val options: RestoreOptions = RestoreOptions(),
)

