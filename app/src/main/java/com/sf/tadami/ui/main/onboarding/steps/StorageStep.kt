package com.sf.tadami.ui.main.onboarding.steps

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.R
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.storage.StoragePreferences
import com.sf.tadami.ui.tabs.more.settings.screens.data.DataPreferencesScreen
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.utils.isSet
import kotlinx.coroutines.flow.collectLatest
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class StorageStep : OnboardingStep {

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    private val dataStore : DataStore<Preferences> = Injekt.get()

    @Composable
    override fun Content() {
        val storagePreferencesState = rememberDataStoreState(StoragePreferences)
        val storagePreferences by storagePreferencesState.value.collectAsState()

        val pickStorageLocation = DataPreferencesScreen.storageLocationPicker(storagePreferences,storagePreferencesState)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Text(
                stringResource(
                    R.string.onboarding_storage_info,
                    stringResource(R.string.app_name),
                    DataPreferencesScreen.storageLocationText(storagePreferences),
                ),
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    try {
                        pickStorageLocation.launch(null)
                    } catch (e: ActivityNotFoundException) {
                        UiToasts.showToast(R.string.file_picker_error)
                    }
                },
            ) {
                Text(stringResource(R.string.onboarding_storage_action_select))
            }
        }

        LaunchedEffect(Unit) {
            dataStore.isSet(StoragePreferences.STORAGE_DIR).collectLatest { _isComplete = it  }
        }
    }
}