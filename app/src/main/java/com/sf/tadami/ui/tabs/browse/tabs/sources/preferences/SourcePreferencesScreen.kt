package com.sf.tadami.ui.tabs.browse.tabs.sources.preferences

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.preferences.model.rememberUnknownDataStoreState

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcePreferencesScreen(
    navController: NavHostController,
    sourcePreferencesViewModel: SourcePreferencesViewModel = viewModel()
){
    val uiState by sourcePreferencesViewModel.uiState.collectAsState()
    val datastoreState = rememberUnknownDataStoreState(unknownDataStore = sourcePreferencesViewModel.dataStore)
    val sourcePreferences by datastoreState.value.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.tabTitle) },
                navigationIcon = {

                    IconButton(onClick = {navController.navigateUp()}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.stub_text),
                        )
                    }

                }
            )
        },
        content = { contentPadding ->
            SourcePreferenceParser(
                modifier = Modifier.padding(contentPadding),
                items = uiState.preferencesItems,
                prefs = sourcePreferences,
                onPrefChanged = { key, value ->
                    datastoreState.setValue(value,key as Preferences.Key<Any>)
                }
            )
        },
    )
}