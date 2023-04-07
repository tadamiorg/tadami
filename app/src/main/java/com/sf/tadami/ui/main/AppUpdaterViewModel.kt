package com.sf.tadami.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.update.AppUpdate
import com.sf.tadami.data.update.AppUpdater
import com.sf.tadami.data.update.GithubUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppUpdaterViewModel : ViewModel() {

    private val _updateInfos : MutableStateFlow<GithubUpdate?> = MutableStateFlow(null)
    val updateInfos = _updateInfos.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val update = AppUpdater().checkForUpdate()

            if(update is AppUpdate.NewUpdate){
                _updateInfos.update { update.release }
            }
        }
    }
}