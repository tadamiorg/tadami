package com.sf.tadami.ui.tabs.more.settings.screens.data.backup

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.sf.tadami.data.backup.BackupOptions
import com.sf.tadami.notifications.backup.BackupCreateWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CreateBackupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreateBackupUiState())
    val uiState: StateFlow<CreateBackupUiState> = _uiState.asStateFlow()

    fun toggle(setter: (BackupOptions, Boolean) -> BackupOptions, enabled: Boolean) {
        _uiState.update {
            it.copy(
                options = setter(it.options, enabled),
            )
        }
    }

    fun createBackup(context: Context, uri: Uri) {
        BackupCreateWorker.startNow(context, uri, uiState.value.options)
    }
}

@Immutable
data class CreateBackupUiState(
    val options: BackupOptions = BackupOptions(),
)

