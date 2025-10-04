package com.sf.tadami.ui.tabs.more.about

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.R
import com.sf.tadami.data.update.AppUpdate
import com.sf.tadami.data.update.AppUpdater
import com.sf.tadami.ui.main.AppUpdaterUiState
import com.sf.tadami.ui.utils.UiToasts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutViewModel : ViewModel() {

    private val _appUpdaterUiState: MutableStateFlow<AppUpdaterUiState> = MutableStateFlow(
        AppUpdaterUiState()
    )
    val appUpdaterUiState = _appUpdaterUiState.asStateFlow()

    fun checkVersion(
        onFinish: () -> Unit,
    ) {
        val updateChecker = AppUpdater()
        viewModelScope.launch(Dispatchers.Main) {
            try {
                when (
                    val result = withContext(Dispatchers.IO) {
                        updateChecker.checkForUpdate(true)
                    }
                ) {
                    is AppUpdate.NewUpdate -> {
                        _appUpdaterUiState.update {
                            it.copy(
                                updateInfos = result.release,
                                shouldShowUpdateDialog = true
                            )
                        }
                    }

                    is AppUpdate.NoNewUpdate -> {
                        UiToasts.showToast(R.string.update_check_no_new_updates)
                    }
                }
            } catch (e: Exception) {
                e.message?.let { UiToasts.showToast(it) }
                Log.d("Check version", e.stackTraceToString())
            } finally {
                onFinish()
            }
        }
    }

    fun hideDialog(){
        _appUpdaterUiState.update {
            it.copy(
                shouldShowUpdateDialog = false
            )
        }
    }
}