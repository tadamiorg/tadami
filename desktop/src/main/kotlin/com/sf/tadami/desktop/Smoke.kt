package com.sf.tadami.desktop

import com.sf.tadami.desktop.extension.ExtensionManager
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.model.SAnime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Installs every extension of the official repo, then runs each source end to end: latest, search,
 * details, episodes, streams. Prints what each step gives. `gradlew :desktop:smoke [-Psmoke.only=animesama]`
 */
fun main(args: Array<String>) = runBlocking {
    DesktopModule.install()
    val manager = Injekt.get<ExtensionManager>()
    manager.refreshAvailable()
    val filter = args.firstOrNull()
    for (entry in manager.available.value.filter { filter == null || it.pkg.endsWith(filter) }) {
        println("=== ${entry.name} ${entry.version}")
        val installed = runCatching { manager.install(entry) }.getOrElse {
            println("  INSTALL FAILED: $it")
            null
        } ?: continue
        if (installed.error != null) {
            println("  LOAD FAILED: ${installed.error}")
            continue
        }
        for (source in installed.sources.filterIsInstance<AnimeCatalogueSource>()) {
            step("latest") { source.getLatestAnime(1).let { "${it.animes.size} animes, next ${it.hasNextPage}: ${it.animes.take(3).map(SAnime::title)}" } }
            val search = step("search") {
                source.getSearchAnime(1, "one piece", source.getFilterList()).also { page ->
                    println("    ${page.animes.size} results: ${page.animes.take(3).map { it.title }}")
                }
            }
            val anime = search?.animes?.firstOrNull() ?: continue
            step("details") { source.getAnimeDetails(anime).let { "${it.title} | ${it.status} | ${it.description?.take(80)}" } }
            val episodes = step("episodes") {
                source.getEpisodeList(anime).also { println("    ${it.size} episodes, first: ${it.lastOrNull()?.name}") }
            }
            val episode = episodes?.lastOrNull() ?: continue
            step("streams") {
                source.getEpisodeSources(episode).joinToString("\n      ", prefix = "\n      ") {
                    "${it.fullName} ${it.url.take(100)} headers=${it.headers?.names()}"
                }
            }
        }
    }
    System.exit(0)
}

private suspend fun <T> step(name: String, block: suspend () -> T): T? = try {
    val value = withTimeout(90_000) { block() }
    if (value !is Unit && value !is com.sf.tadami.source.AnimesPage && value !is List<*>) println("  $name: $value")
    else println("  $name: ok")
    value
} catch (e: Throwable) {
    println("  $name FAILED: $e")
    e.stackTrace.take(4).forEach { println("      at $it") }
    null
}
