package com.sf.tadami.animesources.sources.fr.vostfree

import com.sf.tadami.R
import com.sf.tadami.animesources.extractors.DoodExtractor
import com.sf.tadami.animesources.extractors.OkruExtractor
import com.sf.tadami.animesources.sources.fr.vostfree.extractors.MyTvExtractor
import com.sf.tadami.animesources.sources.fr.vostfree.extractors.VudeoExtractor
import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.network.api.model.SAnime
import com.sf.tadami.network.api.model.SEpisode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.api.online.AnimeSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import com.sf.tadami.utils.Lang
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class VostFree : AnimeSource("VostFree") {

    override val name: String = "VostFree"

    override val baseUrl: String = "https://vostfree.ws"

    override val lang: Lang = Lang.FRENCH

    override val client: OkHttpClient = network.cloudflareClient

    override val supportSearch: Boolean = false

    override fun getIconRes(): Int {
        return R.drawable.vostfree
    }

    override fun latestSelector(): String = "div.last-episode"

    override fun latestAnimeNextPageSelector(): String = "div.navigation > a:has(span.next-page)"

    override fun latestAnimeFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        anime.title = element.selectFirst("div.title a")!!.text()
        anime.setUrlWithoutDomain(element.selectFirst("div.title a")!!.attr("href"))
        anime.thumbnailUrl = baseUrl + element.selectFirst("span.image img")?.attr("src")
        anime.release = element.selectFirst("ul.additional li:eq(5) > a")?.text()

        return anime
    }

    override fun latestAnimesRequest(page: Int): Request = GET("$baseUrl/last-episode.html/page/$page")

    override fun searchSelector(): String {
        TODO("Not yet implemented")
    }

    override fun searchAnimeFromElement(element: Element): SAnime {
        TODO("Not yet implemented")
    }

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        TODO("Not yet implemented")
    }

    override fun searchAnimeNextPageSelector(): String? {
        TODO("Not yet implemented")
    }

    override fun animeDetailsParse(document: Document): SAnime {
        val anime = SAnime.create()
        anime.title = document.selectFirst("div.slide-middle h1")!!.text()
        anime.description = document.selectFirst("div.slide-desc")?.text()
        anime.genres = document.select("div.slide-middle ul.slide-top li.right a").map { it.text() }
        anime.thumbnailUrl = baseUrl + document.selectFirst("div.slide-poster img")?.attr("src")
        anime.release = document.selectFirst("div.slide-info p a")?.text()

        return anime
    }

    override fun episodesSelector(): String = throw Exception("Not used")

    override fun episodeFromElement(element: Element): SEpisode = throw Exception("Not used")

    override fun episodesParse(response: Response): List<SEpisode> {
        val episodes = mutableListOf<SEpisode>()
        val jsoup = response.asJsoup()
        jsoup.select("select.new_player_selector option").forEachIndexed { index, it ->
            val epNum = it.text().replace("Episode", "").drop(2)

            if (it.text() == "Film") {
                val episode = SEpisode.create().apply {
                    episodeNumber = "1".toFloat()
                    name = "Film"
                }
                episode.url = ("?episode:${0}/${response.request.url}")
                episodes.add(episode)
            } else {
                val episode = SEpisode.create().apply {
                    episodeNumber = epNum.toFloat()
                    name = "Épisode $epNum"
                }
                episode.setUrlWithoutDomain("?episode:$index/${response.request.url}")
                episodes.add(episode)
            }
        }

        return episodes.reversed()
    }

    override fun streamSourcesSelector(): String = throw Exception("Not used")

    override fun streamSourcesFromElement(element: Element): List<StreamSource> = throw Exception("Not used")

    override fun episodeSourcesParse(response: Response): List<StreamSource> {
        val epNum = response.request.url.toString().substringAfter("$baseUrl/?episode:").substringBefore("/")
        val realUrl = response.request.url.toString().replace("$baseUrl/?episode:$epNum/", "")

        val document = client.newCall(GET(realUrl)).execute().asJsoup()
        val videoList = mutableListOf<StreamSource>()
        val allPlayerIds = document.select("div.tab-content div div.new_player_top div.new_player_bottom div.button_box")[epNum.toInt()]

        allPlayerIds.select("div").forEach {
            val server = it.text()
            if (server.lowercase() == "vudeo") {
                val headers = headers.newBuilder()
                    .set("referer", "https://vudeo.io/")
                    .build()
                val playerId = it.attr("id")
                val url = document.select("div#player-tabs div.tab-blocks div.tab-content div div#content_$playerId").text()
                try {
                    val video = VudeoExtractor(client).videosFromUrl(url, headers)
                    videoList.addAll(video)
                } catch (_: java.lang.Exception) {}
            }
            if (server.lowercase() == "ok") {
                val playerId = it.attr("id")
                val url = "https://ok.ru/videoembed/" + document.select("div#player-tabs div.tab-blocks div.tab-content div div#content_$playerId").text()
                val video = OkruExtractor(client).videosFromUrl(url, "", false)
                videoList.addAll(video)
            }
            if (server.lowercase() == "doodstream") {
                val playerId = it.attr("id")
                val url = document.select("div#player-tabs div.tab-blocks div.tab-content div div#content_$playerId").text()
                val video = DoodExtractor(client).videoFromUrl(url, "DoodStream", false)
                if (video != null) {
                    videoList.add(video)
                }
            }
            if (server.lowercase() == "mytv" || server.lowercase() == "stream") {
                val playerId = it.attr("id")
                val url = "https://www.myvi.tv/embed/" + document.select("div#player-tabs div.tab-blocks div.tab-content div div#content_$playerId").text()
                val video = MyTvExtractor(client).videosFromUrl(url)
                videoList.addAll(video)
            }
        }

        return videoList.sort()
    }

    private fun List<StreamSource>.sort(): List<StreamSource> {
        val server = "Mytv"

        return this.sortedWith(
            compareBy { it.quality.contains(server) }
        ).reversed()
    }





}