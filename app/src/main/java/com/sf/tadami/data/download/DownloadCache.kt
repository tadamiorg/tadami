package com.sf.tadami.data.download

import com.hippo.unifile.UniFile
import com.sf.tadami.domain.storage.StorageManager
import com.sf.tadami.utils.logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority

private typealias Tree = Map<String, Map<String, Set<String>>>

/**
 * The downloaded episodes, as directory names: source, anime, episodes. Filled by one scan of the
 * downloads folder, then kept up to date by the downloads and deletions, so the episode lists
 * never query the storage per episode. Rescanned when the storage folder changes.
 */
class DownloadCache(
    private val provider: DownloadProvider,
    storageManager: StorageManager,
    private val scope: CoroutineScope,
) {

    @Volatile
    private var tree: Tree = emptyMap()

    private val _changes = MutableStateFlow(0L)

    /** Changes each time the downloaded episodes do */
    val changes: StateFlow<Long> = _changes.asStateFlow()

    private var scanJob: Job? = null

    init {
        rescan()
        storageManager.changes
            .onEach { rescan() }
            .launchIn(scope)
    }

    fun isEpisodeDownloaded(sourceDir: String, animeDir: String, episodeDir: String): Boolean {
        return tree[sourceDir]?.get(animeDir)?.contains(episodeDir) == true
    }

    fun addEpisode(sourceDir: String, animeDir: String, episodeDir: String) = edit { tree ->
        val animes = tree[sourceDir].orEmpty()
        tree + (sourceDir to animes + (animeDir to animes[animeDir].orEmpty() + episodeDir))
    }

    fun removeEpisode(sourceDir: String, animeDir: String, episodeDir: String) = edit { tree ->
        val animes = tree[sourceDir] ?: return@edit tree
        tree + (sourceDir to animes + (animeDir to animes[animeDir].orEmpty() - episodeDir))
    }

    fun removeAnime(sourceDir: String, animeDir: String) = edit { tree ->
        val animes = tree[sourceDir] ?: return@edit tree
        tree + (sourceDir to animes - animeDir)
    }

    /** Reads the whole downloads folder again, in the background. */
    @Synchronized
    fun rescan() {
        scanJob?.cancel()
        scanJob = scope.launch {
            while (true) {
                val version = _changes.value
                val scanned = try {
                    scan(provider.downloadsDir)
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e, tag = "DownloadCache") { "Couldn't scan the downloads" }
                    return@launch
                }
                // Unless a download or a deletion changed the folder meanwhile: scan again then
                if (replace(scanned, version)) return@launch
            }
        }
    }

    @Synchronized
    private fun replace(scanned: Tree, version: Long): Boolean {
        if (_changes.value != version) return false
        edit { scanned }
        return true
    }

    @Synchronized
    private fun edit(transform: (Tree) -> Tree) {
        tree = transform(tree)
        _changes.update { it + 1 }
    }

    private fun scan(root: UniFile?): Tree {
        return root?.listFiles().orEmpty()
            .filter { it.isDirectory }
            .associate { sourceDir ->
                sourceDir.name.orEmpty() to sourceDir.listFiles().orEmpty()
                    .filter { it.isDirectory }
                    .associate { animeDir ->
                        animeDir.name.orEmpty() to animeDir.listFiles().orEmpty()
                            .filter { it.isDirectory }
                            .mapNotNull { it.name }
                            // A download in progress isn't downloaded
                            .filterNot { it.endsWith(TadamiDownloadManager.TMP_DIR_SUFFIX) }
                            .toSet()
                    }
            }
    }
}
