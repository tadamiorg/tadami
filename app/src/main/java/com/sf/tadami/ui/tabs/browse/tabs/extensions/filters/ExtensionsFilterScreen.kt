package com.sf.tadami.ui.tabs.browse.tabs.extensions.filters

import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsFilterScreen(
    navController: NavHostController,
    extensionsFilterViewModel: ExtensionsFilterViewModel = viewModel()
) {

    val uiState by extensionsFilterViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TadaTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.label_extensions),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = listOf(),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                        )
                    }
                }
            )
        }
    ) {
        ExtensionsFilterComponent(
            contentPadding = it,
            uiState = uiState,
            onClickToggle = extensionsFilterViewModel::toggle
        )
    }
}