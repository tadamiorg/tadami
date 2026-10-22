package com.sf.tadami.ui.discover.migrate.dialog

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.components.modal.ModalAction
import com.sf.tadami.ui.components.modal.ModalCheckboxRow
import com.sf.tadami.ui.components.modal.TadaDialog
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
    val flags = remember(oldAnime) { MigrationFlags.getFlags(oldAnime?.let(migrateDialogViewModel::hasDownloads) == true) }
    val selectedFlags = remember(flags) { flags.map { it.isDefaultSelected }.toMutableStateList() }
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
            val migrate = { replace: Boolean ->
                if(oldAnime == null || newAnime == null){
                    Log.d("OldOrNew",oldAnime.toString())
                }else{
                    coroutineScope.launchIO {
                        migrateDialogViewModel.migrateAnime(
                            oldAnime,
                            newAnime,
                            replace,
                            MigrationFlags.getSelectedFlagsBitMap(selectedFlags,flags)
                        )
                        withContext(Dispatchers.Main){
                            onMigrate(state.migrationState)
                        }
                    }
                }
            }
            TadaDialog(
                onDismissRequest = onDismissRequest,
                title = stringResource(id = R.string.label_migration),
                neutral = ModalAction(stringResource(R.string.display_anime), enabled = enableShowTitle) {
                    onDismissRequest()
                    onClickTitle()
                },
                dismiss = ModalAction(stringResource(R.string.action_copy)) { migrate(false) },
                confirm = ModalAction(stringResource(R.string.action_move)) { migrate(true) },
            ) {
                flags.forEachIndexed { index, flag ->
                    ModalCheckboxRow(
                        label = stringResource(flag.titleId),
                        checked = selectedFlags[index],
                        onCheckedChange = { selectedFlags[index] = it },
                    )
                }
            }
        }
    }
}