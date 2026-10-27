package com.sf.tadami.ui.tabs.more.settings.screens.data.backup

import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.Injekt
import com.sf.tadami.BackgroundJobs
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.sf.tadami.data.backup.BackupOptions
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

    fun createBackup(uri: String) {
        Injekt.get<BackgroundJobs>().startBackup(uri, uiState.value.options)
    }
}

@Immutable
data class CreateBackupUiState(
    val options: BackupOptions = BackupOptions(),
)

