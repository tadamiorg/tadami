package com.sf.tadami.ui.tabs.more.about

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.R
import com.sf.tadami.data.update.AppUpdate
import com.sf.tadami.data.update.AppUpdater
import com.sf.tadami.data.update.GithubUpdate
import com.sf.tadami.ui.utils.UiToasts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutViewModel : ViewModel() {

    // The newer release found by the last check, until its screen is opened
    private val _newRelease = MutableStateFlow<GithubUpdate?>(null)
    val newRelease = _newRelease.asStateFlow()

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
                        _newRelease.value = result.release
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

    fun onNewReleaseShown(){
        _newRelease.value = null
    }
}