package com.sf.tadami.ui.discover.migrate.dialog

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DialogCheckBoxRow
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.utils.launchIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MigrateDialog(
    opened: Boolean = false,
    oldAnime: Anime?,
    newAnime : Anime?,
    onDismissRequest: () -> Unit = {},
    onClickTitle: () -> Unit = {},
    onMigrate : (MigrationState) -> Unit,
    enableShowTitle : Boolean = true,
    migrateDialogViewModel: MigrateDialogViewModel = viewModel()
) {
    val flags = remember { MigrationFlags.getFlags() }
    val selectedFlags = remember { flags.map { it.isDefaultSelected }.toMutableStateList() }
    val state by migrateDialogViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    if (opened) {
        if(state.migrationState == MigrationState.RUNNING){
            BackHandler {}
            ContentLoader(
                isLoading = true,
                subtitle = {
                    Text(textAlign = TextAlign.Center,modifier = Modifier.padding(vertical = MaterialTheme.padding.tiny, horizontal = MaterialTheme.padding.small),text = stringResource(
                        id = R.string.label_migration
                    ))
                    Text(textAlign = TextAlign.Center,modifier = Modifier.padding(horizontal = MaterialTheme.padding.small),text = oldAnime?.title ?: "")
                },
                strokeWidth = 4.dp
            ) {}
        }else{
            CustomAlertDialog(
                onDismissRequest = onDismissRequest,
                title = {
                    Text(text = stringResource(id = R.string.label_migration))
                },
                confirmButton = {}
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState()),
                ) {
                    flags.forEachIndexed { index, flag ->
                        DialogCheckBoxRow(
                            label = stringResource(flag.titleId),
                            isSelected = selectedFlags[index],
                            onSelected = { selectedFlags[index] = it.not() },
                        )
                    }
                    TextButton(
                        enabled = enableShowTitle,
                        onClick = {
                            onDismissRequest()
                            onClickTitle()
                        },
                    ) {
                        Text(text = stringResource(R.string.display_anime))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall, Alignment.End)) {
                        TextButton(
                            onClick = {
                                if(oldAnime == null || newAnime == null){
                                    Log.e("OldOrNew",oldAnime.toString())
                                }else{
                                    coroutineScope.launchIO {
                                        migrateDialogViewModel.migrateManga(
                                            oldAnime,
                                            newAnime,
                                            false,
                                            MigrationFlags.getSelectedFlagsBitMap(selectedFlags,flags)
                                        )
                                        withContext(Dispatchers.Main){
                                            onMigrate(state.migrationState)
                                        }
                                    }
                                }

                            },
                        ) {
                            Text(text = stringResource(R.string.action_copy))
                        }
                        TextButton(
                            onClick = {
                                if(oldAnime == null || newAnime == null){
                                    Log.e("OldOrNew",oldAnime.toString())
                                }else{
                                    coroutineScope.launchIO {
                                        migrateDialogViewModel.migrateManga(
                                            oldAnime,
                                            newAnime,
                                            true,
                                            MigrationFlags.getSelectedFlagsBitMap(selectedFlags,flags)
                                        )
                                        withContext(Dispatchers.Main){
                                            onMigrate(state.migrationState)
                                        }
                                    }
                                }
                            },
                        ) {
                            Text(text = stringResource(R.string.action_move))
                        }
                    }
                }
            }
        }
    }
}