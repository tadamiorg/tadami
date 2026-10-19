package com.sf.tadami.ui.tabs.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.animeInfos.AnimeInfosRoutes
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.navigation.graphs.settings.SettingsRoutes
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.library.LibrarySort
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.category.CategoryCheckbox
import com.sf.tadami.ui.category.SetCategoriesDialog
import com.sf.tadami.ui.category.visualName
import com.sf.tadami.ui.components.bottombar.ContextualBottomBar
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.DropDownAction
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.components.filters.TadaBottomSheetScaffold
import com.sf.tadami.ui.components.screens.TabText
import com.sf.tadami.ui.components.topappbar.ContextualSearchTopAppBar
import com.sf.tadami.ui.tabs.library.bottomsheet.LibrarySheetContent
import com.sf.tadami.ui.tabs.library.bottomsheet.downloadedOnly
import com.sf.tadami.ui.tabs.library.bottomsheet.libraryFilters
import com.sf.tadami.ui.tabs.library.bottomsheet.sortComparator
import com.sf.tadami.ui.themes.colorschemes.active
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    setNavDisplay: (display: Boolean) -> Unit,
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current

    val initLoaded by libraryViewModel.initLoaded.collectAsState()
    val libraryList by libraryViewModel.libraryList.collectAsState()
    val categories by libraryViewModel.categories.collectAsState()
    val activeCategoryId by libraryViewModel.activeCategoryId.collectAsState()
    val searchFilter by libraryViewModel.searchFilter.collectAsState()
    val downloadedIds by libraryViewModel.downloadedIds.collectAsState()
    val libraryPreferences by rememberDataStoreState(customPrefs = LibraryPreferences).value.collectAsState()
    val tabs = remember(libraryList, categories) { libraryTabs(categories, libraryList) }
    // The search, the filters and the sort apply inside each tab
    val displayedTabs = remember(tabs, libraryPreferences, searchFilter, downloadedIds) {
        tabs.map { tab ->
            val sort = categorySort(
                category = tab.category,
                perCategory = libraryPreferences.categorizedSort,
                librarySort = libraryPreferences.sortFlags,
                defaultCategoryFlags = libraryPreferences.defaultCategorySortFlags
            )
            tab.copy(items = tab.items.addFilters(libraryPreferences, sort, searchFilter, downloadedIds))
        }
    }
    val displayedIds = remember(displayedTabs) {
        displayedTabs.flatMapTo(HashSet()) { tab -> tab.items.map { it.anime.id } }
    }
    val activeTab = remember(displayedTabs, activeCategoryId) {
        displayedTabs.find { it.category.id == activeCategoryId } ?: displayedTabs.firstOrNull()
    }
    val searching = searchFilter.isNotEmpty()
    val showTabRow = displayedTabs.showTabRow(libraryPreferences.showCategoryTabs, searching)
    val badgeCount: (LibraryTab) -> Int? = { tab ->
        tab.badgeCount(libraryPreferences.showCategoryNumberOfItems, searching)
    }
    // Without the tab row, the title names the category shown
    val titleTab = displayedTabs.titleCategory(activeCategoryId, showTabRow)?.let { activeTab }
    // Select all and invert act on the tab shown
    val pageIds = remember(activeTab) {
        activeTab?.items.orEmpty().mapTo(HashSet()) { it.anime.id }
    }
    LaunchedEffect(displayedIds) {
        libraryViewModel.keepDisplayedSelected(displayedIds)
    }
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val librarySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val filterTint = if (libraryPreferences.filterFlags.isFiltered) MaterialTheme.colorScheme.active else LocalContentColor.current
    // The category shown, as the pull to refresh. Without any category, the library
    val refreshDetails: () -> Unit = {
        val category = activeTab?.category?.takeIf { categories.isNotEmpty() }
        val started = libraryViewModel.refreshLibraryDetails(context, category?.id)
        val msg = when {
            !started -> context.getString(R.string.update_running)
            category != null -> context.getString(
                R.string.refresh_details_starting_category,
                if (category.isSystemCategory) context.getString(R.string.category_default) else category.name
            )
            else -> context.getString(R.string.refresh_details_starting)
        }
        coroutineScope.launch {
            snackBarHostState.currentSnackbarData?.dismiss()
            snackBarHostState.showSnackbar(msg)
        }
    }
    val refreshDetailsTitle = stringResource(R.string.action_refresh_library_details)
    val actions = remember(libraryPreferences.filterFlags.isFiltered, activeTab, categories) {
        listOf(
            Action.Drawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_filter,
                tint = filterTint,
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showBottomSheet = true
                }
            ),
            Action.CastButton(),
            Action.DropDownVector(
                title = R.string.stub_text,
                icon = Icons.Outlined.MoreVert,
                onClick = {},
                items = listOf(
                    DropDownAction(
                        title = refreshDetailsTitle,
                        onClick = refreshDetails,
                    ),
                ),
            ),
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

    var isSearchMode by rememberSaveable {
        mutableStateOf(false)
    }

    var setCategoriesSelection by remember { mutableStateOf<List<CategoryCheckbox>?>(null) }
    setCategoriesSelection?.let { selection ->
        SetCategoriesDialog(
            initialSelection = selection,
            // Like the other actions of the bar, it ends the selection: the bar stays disabled after one
            onDismissRequest = {
                setCategoriesSelection = null
                libraryViewModel.toggleAllSelected(false)
            },
            onConfirm = { add, remove -> libraryViewModel.setSelectedCategories(add, remove) },
            onEditCategories = { navController.navigate(SettingsRoutes.CATEGORIES) }
        )
    }

    val isRefreshing by libraryViewModel.isRefreshing.collectAsState()
    val scaffoldInsets =  WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)

    TadaBottomSheetScaffold(
        modifier = modifier,
        sheetState = librarySheetState,
        showSheet = showBottomSheet,
        onDismissRequest = {
            showBottomSheet = false
        },
        sheetContent = {
            LibrarySheetContent(
                activeCategory = activeTab?.category,
                onCategorySortSelected = libraryViewModel::setCategorySort
            )
        },
        topBar = {
            ContextualSearchTopAppBar(
                title = {
                    if (titleTab != null) {
                        TabText(text = titleTab.category.visualName(), badgeCount = badgeCount(titleTab))
                    } else {
                        Text(
                            text = stringResource(id = R.string.library_tab_title)
                        )
                    }
                },
                actions = actions,
                actionModeCounter = isActionMode,
                onCloseActionModeClicked = {
                    libraryViewModel.toggleAllSelected(false)
                },
                onToggleAll = {
                    libraryViewModel.selectAllDisplayed(displayedIds, pageIds)
                },
                onInverseAll = {
                    libraryViewModel.inverseSelected(displayedIds, pageIds)
                },
                onSearchChange = {
                    libraryViewModel.updateSearchFilter(it)
                },
                isSearchMode = isSearchMode,
                onSearchCancel = {
                    isSearchMode = false
                },
                onSearchClicked = {
                    isSearchMode = true
                }
            )
        },
        bottomBar = {
            ContextualBottomBar(
                modifier = Modifier.windowInsetsPadding(scaffoldInsets),
                visible = libraryList.fastAny { it.selected },
                actions = listOf(
                    Action.Vector(
                        title = R.string.action_set_categories,
                        icon = Icons.AutoMirrored.Outlined.Label,
                        onClick = {
                            setCategoriesSelection = libraryViewModel.selectedCategoryCheckboxes()
                        },
                    ),
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.DoneAll,
                        onClick = {
                            libraryViewModel.setSeenStatus(true)
                        },
                    ),
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.RemoveDone,
                        onClick = {
                            libraryViewModel.setSeenStatus(false)
                        },
                    ),
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.DeleteOutline,
                        onClick = {
                            libraryViewModel.unFavorite()
                        }
                    )
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { contentPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val pageModifier = Modifier
            .fillMaxSize()
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding(),
            )
        // Before the search and the filters: an empty category says so, a search without results doesn't
        val categorySizes = remember(tabs) { tabs.associate { it.category.id to it.items.size } }
        val libraryPage: @Composable (LibraryTab?) -> Unit = { tab ->
            LibraryComponent(
                modifier = pageModifier,
                libraryList = tab?.items.orEmpty(),
                librarySize = libraryList.size,
                categoryEmpty = tab != null && categorySizes[tab.category.id] == 0,
                initLoaded = initLoaded,
                onAnimeClicked = { libraryItem ->
                    when {
                        libraryItem.selected -> {
                            libraryViewModel.toggleSelected(libraryItem, false)
                        }

                        libraryList.fastAny { it.selected } -> {
                            libraryViewModel.toggleSelected(libraryItem, true)
                        }

                        else -> {
                            navController.navigate("${AnimeInfosRoutes.DETAILS}/${libraryItem.anime.source}/${libraryItem.anime.id}")
                        }
                    }
                },
                onAnimeLongCLicked = { libraryItem ->
                    setNavDisplay(false)
                    libraryViewModel.toggleSelected(libraryItem, true)
                },
                isRefreshing = isRefreshing,
                onRefresh = {
                    // The category shown, as Mihon. Without any category, the library
                    val category = activeTab?.category?.takeIf { categories.isNotEmpty() }
                    val started = libraryViewModel.refreshLibrary(context, category?.id)
                    val msgRes = when {
                        !started -> context.getString(R.string.update_running)
                        category != null -> context.getString(
                            R.string.update_starting_category,
                            if (category.isSystemCategory) context.getString(R.string.category_default) else category.name
                        )
                        else -> context.getString(R.string.update_starting)
                    }
                    coroutineScope.launch {
                        snackBarHostState.currentSnackbarData?.dismiss()
                        snackBarHostState.showSnackbar(msgRes)
                    }
                },
                onEmptyRefreshClicked = {
                    navController.navigate(HomeNavItems.Browse.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }

                }
            )
        }

        val tabsModifier = Modifier
            .padding(top = contentPadding.calculateTopPadding())
            .consumeWindowInsets(contentPadding)
        if (!initLoaded) {
            // The loading indicator, until the library and the categories are read
            Box(modifier = tabsModifier) {
                libraryPage(null)
            }
        } else {
            LibraryPager(
                modifier = tabsModifier,
                tabs = displayedTabs,
                showTabRow = showTabRow,
                activeCategoryId = activeCategoryId,
                onActiveCategoryChanged = libraryViewModel::setActiveCategory,
                badgeCount = badgeCount,
                pageContent = { tab -> libraryPage(tab) }
            )
        }
    }
}

private fun List<LibraryItem>.addFilters(
    prefs: LibraryPreferences,
    sort: LibrarySort,
    searchFilter: String,
    downloadedIds: Set<Long>?
): List<LibraryItem> {
    return this
        .filter {
            it.anime.title.contains(searchFilter, true)
        }
        .downloadedOnly(downloadedIds)
        .libraryFilters(prefs.filterFlags)
        .sortedWith { a1, a2 -> sortComparator(sort).invoke(a1, a2) }
}
