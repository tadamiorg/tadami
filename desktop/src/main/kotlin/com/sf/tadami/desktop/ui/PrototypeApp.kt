package com.sf.tadami.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sf.tadami.desktop.extension.ExtensionManager
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.ui.utils.UiToasts
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** The screens of the prototype, a back stack. */
sealed interface Screen {
    data object Extensions : Screen
    data object Sources : Screen
    data class Browse(val source: AnimeCatalogueSource) : Screen
    data class Details(val source: AnimeCatalogueSource, val anime: SAnime) : Screen
    data class Player(val source: AnimeCatalogueSource, val anime: SAnime, val episode: SEpisode) : Screen
}

@Composable
fun PrototypeApp() {
    val stack = remember { mutableStateListOf<Screen>(Screen.Sources) }
    var tab by remember { mutableStateOf<Screen>(Screen.Sources) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        UiToasts.messages.collect { snackbar.showSnackbar(it) }
    }
    fun open(screen: Screen) = stack.add(screen)
    // TADAMI_AUTOPILOT=animesama;one piece;0;0: opens that source, searches, opens the result and
    // plays the episode (index from the first one): a run without clicks, for testing
    LaunchedEffect(Unit) {
        val (pkg, query, animeIndex, episodeIndex) = System.getenv("TADAMI_AUTOPILOT")?.split(';') ?: return@LaunchedEffect
        val manager = Injekt.get<ExtensionManager>()
        manager.loadInstalled()
        if (manager.installed.value.none { it.meta.pkgName.endsWith(pkg) }) {
            manager.refreshAvailable()
            manager.install(manager.available.value.first { it.pkg.endsWith(pkg) })
        }
        val source = manager.installed.value.first { it.meta.pkgName.endsWith(pkg) }.sources.filterIsInstance<AnimeCatalogueSource>().first()
        val anime = source.getSearchAnime(1, query, source.getFilterList()).animes[animeIndex.toInt()]
        val episode = source.getEpisodeList(anime).reversed()[episodeIndex.toInt()]
        println("Autopilot: ${source.name} / ${anime.title} / ${episode.name}")
        open(Screen.Player(source, anime, episode))
    }
    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    val current = stack.last()
    if (current is Screen.Player) {
        PlayerScreen(current.source, current.anime, current.episode, onBack = ::back)
        return
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail {
                NavigationRailItem(
                    selected = tab == Screen.Sources,
                    onClick = { tab = Screen.Sources; stack.clear(); stack.add(Screen.Sources) },
                    icon = { Icon(Icons.Default.Explore, null) },
                    label = { Text("Browse") },
                )
                NavigationRailItem(
                    selected = tab == Screen.Extensions,
                    onClick = { tab = Screen.Extensions; stack.clear(); stack.add(Screen.Extensions) },
                    icon = { Icon(Icons.Default.Extension, null) },
                    label = { Text("Extensions") },
                )
            }
            Box(Modifier.fillMaxSize()) {
                when (current) {
                    Screen.Extensions -> ExtensionsScreen()
                    Screen.Sources -> SourcesScreen(onOpen = { open(Screen.Browse(it)) })
                    is Screen.Browse -> BrowseScreen(current.source, onBack = ::back, onOpen = { open(Screen.Details(current.source, it)) })
                    is Screen.Details -> DetailsScreen(
                        current.source,
                        current.anime,
                        onBack = ::back,
                        onPlay = { open(Screen.Player(current.source, current.anime, it)) },
                    )
                    is Screen.Player -> Unit
                }
            }
        }
    }
}
