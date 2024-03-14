package com.sf.tadami.ui.tabs.library

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.tabs.TabsNavItems
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.tabs.library.bottomsheet.libraryFilters
import com.sf.tadami.ui.tabs.library.bottomsheet.sortComparator
import com.sf.tadami.ui.themes.colorschemes.active
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.padding

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    setNavDisplay: (display: Boolean) -> Unit,
    bottomNavDisplay: Boolean,
    openAnimeDetails : (sourceId : Long,animeId : Long) -> Unit,
    libraryFocusedAnime : Long,
    setLibraryFocusedAnime : (animeId : Long) -> Unit,
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current

    val initLoaded by libraryViewModel.initLoaded.collectAsState()
    val libraryList by libraryViewModel.libraryList.collectAsState()
    val searchFilter by libraryViewModel.searchFilter.collectAsState()
    val libraryPreferences by rememberDataStoreState(customPrefs = LibraryPreferences).value.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val filterTint =
        if (libraryPreferences.filterFlags.isFiltered) MaterialTheme.colorScheme.active else LocalContentColor.current
    val actions = remember(libraryPreferences.filterFlags.isFiltered) {
        listOf(
            Action.Drawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_filter,
                tint = filterTint,
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            )
        )
    }

    val isActionMode by remember(libraryList) {
        derivedStateOf {
            val count = libraryList.count { it.selected }
            if (count == 0) {
                setNavDisplay(true)
            }
            count
        }
    }

    val isRefreshing by libraryViewModel.isRefreshing.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val libFocusRequesters by remember(libraryList) {
        derivedStateOf {
            libraryList.associate {
                it.anime.id to FocusRequester()
            }
        }
    }
//    LaunchedEffect(Unit) {
//        if(libraryFocusedAnime != -1L){
//            libFocusRequesters[libraryFocusedAnime]!!.requestFocus()
//        }else{
//            focusRequester.requestFocus()
//        }
//    }
    LibraryComponent(
        modifier = modifier
            .fillMaxSize(),
        libFocusRequesters = libFocusRequesters,
        libraryList = libraryList.addFilters(libraryPreferences, searchFilter),
        librarySize = libraryList.size,
        initLoaded = initLoaded,
        onFocusChanged = {
            setLibraryFocusedAnime(it)
        },
        onAnimeClicked = { libraryItem ->
            when {
                libraryItem.selected -> {
                    libraryViewModel.toggleSelected(libraryItem, false)
                }

                libraryList.fastAny { it.selected } -> {
                    libraryViewModel.toggleSelected(libraryItem, true)
                }

                else -> {
                    openAnimeDetails(libraryItem.anime.source,libraryItem.anime.id)
                }
            }
        },
        onAnimeLongCLicked = { libraryItem ->
            setNavDisplay(false)
            libraryViewModel.toggleSelected(libraryItem, true)
        },
        isRefreshing = isRefreshing,
        onRefresh = {
            val started = libraryViewModel.refreshLibrary(context)
            val msgRes =
                if (started) context.getString(R.string.update_starting) else context.getString(
                    R.string.update_running
                )
            UiToasts.showToast(msgRes)
        },
        onEmptyRefreshClicked = {
            navController.navigate(TabsNavItems.Browse.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }

        }
    )
}

private fun List<LibraryItem>.addFilters(
    prefs: LibraryPreferences,
    searchFilter: String
): List<LibraryItem> {
    return this
        .filter {
            it.anime.title.contains(searchFilter, true)
        }
        .libraryFilters(prefs.filterFlags)
        .sortedWith { a1, a2 -> sortComparator(prefs.sortFlags).invoke(a1, a2) }
}
