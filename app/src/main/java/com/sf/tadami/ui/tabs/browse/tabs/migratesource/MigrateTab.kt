package com.sf.tadami.ui.tabs.browse.tabs.migratesource

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.sf.tadami.ui.components.screens.ScreenTabContent

@Composable
fun migrateSourceTab(): ScreenTabContent {
    val uriHandler = LocalUriHandler.current
    val screenModel = rememberScreenModel { MigrateSourceScreenModel() }
    val state by screenModel.state.collectAsState()

    return ScreenTabContent(
        titleRes = MR.strings.label_migration,
        actions = persistentListOf(
            AppBar.Action(
                title = stringResource(MR.strings.migration_help_guide),
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = {
                    uriHandler.openUri("https://mihon.app/docs/guides/source-migration")
                },
            ),
        ),
        content = { contentPadding, _ ->
            MigrateSourceScreen(
                state = state,
                contentPadding = contentPadding,
                onClickItem = { source ->
                    navigator.push(MigrateMangaScreen(source.id))
                },
                onToggleSortingDirection = screenModel::toggleSortingDirection,
                onToggleSortingMode = screenModel::toggleSortingMode,
            )
        },
    )
}