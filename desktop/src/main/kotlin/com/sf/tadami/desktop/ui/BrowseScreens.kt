package com.sf.tadami.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sf.tadami.desktop.extension.ExtensionManager
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun SourcesScreen(onOpen: (AnimeCatalogueSource) -> Unit) {
    val manager = remember { Injekt.get<ExtensionManager>() }
    val installed by manager.installed.collectAsState()
    LaunchedEffect(Unit) { manager.loadInstalled() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sources", style = MaterialTheme.typography.headlineMedium)
        if (installed.isEmpty()) Text("No extension installed: install some in Extensions.")
        LazyColumn {
            items(installed.flatMap { ext -> ext.sources.filterIsInstance<AnimeCatalogueSource>().map { ext to it } }) { (ext, source) ->
                ListItem(
                    modifier = Modifier.clickable { onOpen(source) },
                    leadingContent = { AsyncImage(ext.icon, null, Modifier.size(40.dp)) },
                    headlineContent = { Text(source.name) },
                    supportingContent = { Text(source.lang.displayName) },
                )
            }
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
fun BrowseScreen(source: AnimeCatalogueSource, onBack: () -> Unit, onOpen: (SAnime) -> Unit) {
    val animes = remember(source) { mutableStateListOf<SAnime>() }
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(1) }
    var hasNext by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load(reset: Boolean) {
        if (reset) {
            animes.clear(); page = 1
        }
        loading = true
        error = null
        scope.launch {
            runCatching {
                if (submitted.isBlank()) source.getLatestAnime(page) else source.getSearchAnime(page, submitted, source.getFilterList())
            }.onSuccess {
                animes.addAll(it.animes)
                hasNext = it.hasNextPage
                page++
            }.onFailure { error = it.toString() }
            loading = false
        }
    }
    LaunchedEffect(source, submitted) { load(reset = true) }

    Column(Modifier.fillMaxSize()) {
        TopBar(source.name, onBack) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitted = query }),
                modifier = Modifier.width(320.dp),
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(animes) { index, anime ->
                if (index == animes.lastIndex && hasNext && !loading) LaunchedEffect(index) { load(reset = false) }
                Card(Modifier.clickable { onOpen(anime) }) {
                    Box {
                        AsyncImage(
                            anime.thumbnailUrl,
                            null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Text(anime.title, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (loading) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
fun DetailsScreen(source: AnimeCatalogueSource, anime: SAnime, onBack: () -> Unit, onPlay: (SEpisode) -> Unit) {
    var details by remember { mutableStateOf(anime) }
    var episodes by remember { mutableStateOf<List<SEpisode>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(anime) {
        runCatching { details = source.getAnimeDetails(anime) }.onFailure { error = it.toString() }
        runCatching { episodes = source.getEpisodeList(anime) }.onFailure { error = it.toString() }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(details.title, onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                Row {
                    AsyncImage(details.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.width(180.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(details.title, style = MaterialTheme.typography.headlineSmall)
                        Text(listOfNotNull(details.release, details.studio, details.status.name).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(details.genres?.joinToString() ?: "", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(details.description ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(16.dp))
                Text("${episodes?.size ?: "…"} episodes", style = MaterialTheme.typography.titleMedium)
            }
            if (episodes == null && error == null) item { CircularProgressIndicator() }
            items(episodes.orEmpty()) { episode ->
                ListItem(
                    modifier = Modifier.clickable { onPlay(episode) },
                    headlineContent = { Text(episode.name) },
                    trailingContent = { Icon(Icons.Default.PlayArrow, null) },
                )
                HorizontalDivider()
            }
        }
    }
}
