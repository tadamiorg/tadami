package com.sf.tadami.ui.tabs.more.settings.screens.advanced.background

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import com.sf.tadami.R
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.components.topappbar.ActionItem
import com.sf.tadami.ui.tabs.more.settings.components.PreferenceScreen
import com.sf.tadami.ui.utils.formatDate
import com.sf.tadami.ui.utils.plus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class WorkerInfosScreen(navController : NavHostController) : PreferenceScreen {
    override val title: Int = R.string.advanced_worker_infos
    override val backHandler: (() -> Unit) = { navController.navigateUp() }

    @Composable
    override fun getPreferences(): List<Preference> {
        return emptyList()
    }

    @Composable
    private fun SectionTitle(title: String) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }

    @Composable
    private fun SectionText(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    @Composable
    fun WorkerInfosString(
        contentPadding: PaddingValues,
        workerInfosViewModel: WorkerInfosViewModel = viewModel()
    ) {
        val finishedWorkers by workerInfosViewModel.finished.collectAsState("")
        val runningWorkers by workerInfosViewModel.running.collectAsState("")
        val enqueuedWorkers by workerInfosViewModel.enqueued.collectAsState("")

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding + PaddingValues(horizontal = 16.dp),
        ) {
            item { SectionTitle(title = "Enqueued") }
            item { SectionText(text = enqueuedWorkers) }

            item { SectionTitle(title = "Finished") }
            item { SectionText(text = finishedWorkers) }

            item { SectionTitle(title = "Running") }
            item { SectionText(text = runningWorkers) }
        }

    }


    class WorkerInfosViewModel() : ViewModel() {
        val app = Injekt.get<Application>()
        private val workManager = WorkManager.getInstance(app)

        val finished = workManager
            .getWorkInfosLiveData(
                WorkQuery.fromStates(
                    WorkInfo.State.SUCCEEDED,
                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED
                )
            )
            .asFlow()
            .map(::mapWorkersInfo)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

        val running = workManager
            .getWorkInfosLiveData(WorkQuery.fromStates(WorkInfo.State.RUNNING))
            .asFlow()
            .map(::mapWorkersInfo)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

        val enqueued = workManager
            .getWorkInfosLiveData(WorkQuery.fromStates(WorkInfo.State.ENQUEUED))
            .asFlow()
            .map(::mapWorkersInfo)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

        private fun mapWorkersInfo(list: List<WorkInfo>) = buildString {
            if (list.isEmpty()) {
                appendLine("-")
            } else {
                list.forEach { workInfo ->
                    appendLine("Id: ${workInfo.id}")
                    appendLine("Tags:")
                    workInfo.tags.forEach {
                        appendLine(" - $it")
                    }
                    appendLine("State: ${workInfo.state}")
                    if(workInfo.state == WorkInfo.State.ENQUEUED){
                        appendLine("Next run: ${workInfo.nextScheduleTimeMillis.formatDate()}")
                    }
                    appendLine()
                }
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = getTitle()) },
                    navigationIcon = {
                        IconButton(onClick = backHandler) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.stub_text),
                            )
                        }
                    },
                    actions = {
                        topBarActions.forEach {
                            ActionItem(action = it)
                        }
                    },
                )
            },
            content = { contentPadding ->
                WorkerInfosString(contentPadding)
            },
        )
    }
}