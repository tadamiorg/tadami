package com.sf.tadami.desktop.cast

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.download.isLocalFile
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.episode.isEpisodeWatched
import com.sf.tadami.domain.episode.toSEpisode
import com.sf.tadami.extension.util.callGuarded
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.cast.CastProtocol
import com.sf.tadami.ui.animeinfos.episode.cast.CastRemoteState
import com.sf.tadami.ui.animeinfos.episode.cast.LocalCast
import com.sf.tadami.ui.animeinfos.episode.cast.channels.TvControlMessage
import com.sf.tadami.ui.animeinfos.episode.cast.channels.toCastSubtitleStyle
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.CastProxyServer
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.LocalFileOpener
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.utils.Log
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Casting from the desktop, what the Cast SDK and CastControlService do on Android: a session with a
 * device running the web receiver, the proxy it streams the header-bound sources and the downloaded
 * episodes through, and the TV's requests (progress to save, next, previous, another episode).
 */
object DesktopCast {

    private const val TAG = "DesktopCast"

    // The receiver of the Android release build
    private const val APP_ID = "DA2F4B1A"
    private const val NS_CONTROL = "urn:x-cast:com.sf.tadami.control"
    private const val NS_HANDSHAKE = "urn:x-cast:com.sf.tadami.handshake"
    private const val LAUNCH_TIMEOUT_MILLIS = 30_000L
    private const val STATUS_POLL_MILLIS = 1000L

    /** What the TV plays: [positionMs] when the status came, the SDK's approximate position aside. */
    data class Media(
        val state: String,
        val positionMs: Long,
        val durationMs: Long,
        val mediaSessionId: Int,
        val customData: JsonObject?,
        val idleReason: String?,
    ) {
        val isPlaying: Boolean get() = state == "PLAYING" || state == "BUFFERING"
        val episodeId: Long? get() = customData?.get("episodeId")?.jsonPrimitive?.longOrNull
    }

    class Session internal constructor(
        val device: CastDevice,
        internal val channel: CastChannel,
        internal val transportId: String,
        internal val appSessionId: String,
    ) {
        internal val mutableMedia = MutableStateFlow<Media?>(null)
        val media: StateFlow<Media?> = mutableMedia.asStateFlow()
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requestIds = AtomicInteger(1)
    private val pending = ConcurrentHashMap<Int, CompletableDeferred<JsonObject>>()

    private val mutableSession = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = mutableSession.asStateFlow()

    private val mutableConnecting = MutableStateFlow<CastDevice?>(null)
    val connecting: StateFlow<CastDevice?> = mutableConnecting.asStateFlow()

    val devices: StateFlow<List<CastDevice>> get() = CastDiscovery.devices

    private val proxy by lazy {
        CastProxyServer(
            localAddress = ::lanAddress,
            client = OkHttpClient.Builder().build(),
            // The downloads are plain files on the desktop
            localFiles = LocalFileOpener { uri ->
                runCatching { File(URI(uri)) }.getOrNull()?.takeIf { it.isFile }?.let(::FileInputStream)
            },
        )
    }

    private var pollJob: Job? = null
    private var fetchJob: Job? = null

    fun startDiscovery() = CastDiscovery.start()

    fun connect(device: CastDevice) {
        scope.launch {
            disconnect()
            mutableConnecting.value = device
            try {
                open(device)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not cast to ${device.name}", e)
            } finally {
                mutableConnecting.value = null
            }
        }
    }

    private suspend fun open(device: CastDevice) {
        var opened: Session? = null
        val channel = CastChannel(
            host = device.host,
            port = device.port,
            onMessage = { message -> opened?.let { onMessage(it, message) } ?: onDeviceMessage(message) },
            onClosed = { opened?.let { closeSession(it) } },
        )
        try {
            opened = launchReceiver(device, channel)
        } catch (e: Exception) {
            channel.close()
            throw e
        }
    }

    private suspend fun launchReceiver(device: CastDevice, channel: CastChannel): Session {
        channel.send(CastChannel.RECEIVER, CastChannel.NS_CONNECTION, """{"type":"CONNECT"}""")
        val app = withTimeout(LAUNCH_TIMEOUT_MILLIS) {
            var status = request(channel, CastChannel.RECEIVER, CastChannel.NS_RECEIVER, buildJsonObject {
                put("type", "LAUNCH")
                put("appId", APP_ID)
            })
            // The answer may come before the app runs
            while (status.runningApp() == null) {
                delay(500)
                status = request(channel, CastChannel.RECEIVER, CastChannel.NS_RECEIVER, buildJsonObject { put("type", "GET_STATUS") })
            }
            status.runningApp()!!
        }
        val transportId = app["transportId"]!!.jsonPrimitive.content
        channel.send(transportId, CastChannel.NS_CONNECTION, """{"type":"CONNECT"}""")
        proxy.fallbackUserAgent = Injekt.get<NetworkHelper>().advancedPreferences.userAgent
        proxy.startSafely()
        val session = Session(device, channel, transportId, app["sessionId"]!!.jsonPrimitive.content)
        mutableSession.value = session
        Log.i(TAG, "Casting to ${device.name}, proxy ${proxy.baseUrl()}")

        sendHandshake(session)
        scope.launch {
            delay(1500)
            sendHandshake(session)
        }
        send(session, CastChannel.NS_MEDIA, buildJsonObject { put("type", "GET_STATUS") })
        pollJob = scope.launch {
            // The TV only tells its state changes: the position follows from its status
            while (isActive && mutableSession.value === session) {
                delay(STATUS_POLL_MILLIS)
                if (session.media.value?.isPlaying == true) {
                    runCatching { send(session, CastChannel.NS_MEDIA, buildJsonObject { put("type", "GET_STATUS") }) }
                }
            }
        }
        return session
    }

    /** Ends the session, the receiver app with it unless [stopApp] is false (the TV went first). */
    fun disconnect(stopApp: Boolean = true) {
        val session = mutableSession.value ?: return
        session.media.value?.let { saveTime(it.episodeId, it.customData.isSeen(), it.durationMs, it.positionMs) }
        if (stopApp) {
            runCatching {
                session.channel.send(CastChannel.RECEIVER, CastChannel.NS_RECEIVER, buildJsonObject {
                    put("type", "STOP")
                    put("sessionId", session.appSessionId)
                    put("requestId", requestIds.getAndIncrement())
                }.toString())
            }
        }
        closeSession(session)
    }

    private fun closeSession(session: Session) {
        if (!mutableSession.compareAndSet(session, null) && mutableSession.value != null) return
        pollJob?.cancel()
        fetchJob?.cancel()
        runCatching { session.channel.close() }
        proxy.stop()
        CastRemoteState.selection.value = null
        Log.i(TAG, "Casting to ${session.device.name} ended")
    }

    /**
     * Plays [episode] on the TV from [startMs]: its downloaded files through the proxy when [selected]
     * is one, else [selected] of [sources]. False when the downloaded files can't be cast: the caller
     * casts the source's streams instead, as the Android app does.
     */
    suspend fun loadEpisode(
        episode: Episode,
        anime: Anime?,
        selected: StreamSource,
        sources: List<StreamSource>,
        episodes: List<Episode>,
        startMs: Long,
    ): Boolean {
        val session = mutableSession.value ?: return false
        val payload = if (selected.isLocalFile) {
            val local = LocalCast.resolve(episode, proxy) ?: return false
            payload(episode, anime, local.source, listOf(local.source), episodes, startMs, local.contentUrl)
        } else {
            payload(episode, anime, selected, sources, episodes, startMs, selected.url)
        }
        send(session, CastChannel.NS_MEDIA, payload)
        return true
    }

    private suspend fun payload(
        episode: Episode,
        anime: Anime?,
        selected: StreamSource,
        sources: List<StreamSource>,
        episodes: List<Episode>,
        startMs: Long,
        contentUrl: String,
    ): JsonObject {
        val playerPreferences = Injekt.get<DataStore<Preferences>>().getPreferencesGroup(PlayerPreferences)
        return castLoadPayload(
            episode = episode,
            anime = anime,
            availableSources = sources,
            selectedSource = selected,
            episodes = episodes,
            userAgent = Injekt.get<NetworkHelper>().advancedPreferences.userAgent,
            subtitlePrefLanguages = playerPreferences.subtitlePrefLanguages.split(",").filter { it.isNotBlank() },
            subtitlesEnabled = playerPreferences.subtitlesEnabled,
            resumeTimeMs = startMs,
            proxyBaseUrl = proxy.baseUrl(),
            subtitleStyle = playerPreferences.toCastSubtitleStyle(),
            contentUrl = contentUrl,
        )
    }

    fun play() = mediaCommand("PLAY")

    fun pause() = mediaCommand("PAUSE")

    fun seekTo(positionMs: Long) = mediaCommand("SEEK") { put("currentTime", positionMs.coerceAtLeast(0L) / 1000.0) }

    /** The subtitle the receiver shows, -1 for none: over the control channel, as the Android app. */
    fun selectSubtitle(index: Int) {
        val session = mutableSession.value ?: return
        scope.launch {
            runCatching {
                session.channel.send(
                    session.transportId,
                    NS_CONTROL,
                    json.encodeToString(TvControlMessage(type = "selectSubtitle", subtitleIndex = index)),
                )
            }
        }
    }

    private fun mediaCommand(type: String, extra: JsonObjectBuilder.() -> Unit = {}) {
        val session = mutableSession.value ?: return
        val mediaSessionId = session.media.value?.mediaSessionId ?: return
        scope.launch {
            runCatching {
                send(session, CastChannel.NS_MEDIA, buildJsonObject {
                    put("type", type)
                    put("mediaSessionId", mediaSessionId)
                    extra()
                })
            }
        }
    }

    private fun sendHandshake(session: Session) {
        val message = buildJsonObject {
            put("senderProtocol", CastProtocol.SENDER_VERSION)
            put("minReceiverProtocol", CastProtocol.MIN_RECEIVER_VERSION)
            proxy.baseUrl()?.let { put("proxyBaseUrl", it) }
        }
        runCatching { session.channel.send(session.transportId, NS_HANDSHAKE, message.toString()) }
    }

    private fun send(session: Session, namespace: String, message: JsonObject) {
        val withIds = JsonObject(
            message + mapOf(
                "requestId" to JsonPrimitive(requestIds.getAndIncrement()),
                "sessionId" to JsonPrimitive(session.appSessionId),
            ),
        )
        session.channel.send(session.transportId, namespace, withIds.toString())
    }

    private suspend fun request(channel: CastChannel, destination: String, namespace: String, message: JsonObject): JsonObject {
        val id = requestIds.getAndIncrement()
        val answer = CompletableDeferred<JsonObject>()
        pending[id] = answer
        try {
            channel.send(destination, namespace, JsonObject(message + ("requestId" to JsonPrimitive(id))).toString())
            return answer.await()
        } finally {
            pending.remove(id)
        }
    }

    private fun JsonObject.runningApp(): JsonObject? =
        this["status"]?.jsonObject?.get("applications")?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { it["appId"]?.jsonPrimitive?.content == APP_ID }

    private fun parse(payload: String): JsonObject? = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()

    /** Before the session: the device's answers to the launch. */
    private fun onDeviceMessage(message: CastChannel.Message) {
        val payload = parse(message.payload) ?: return
        payload["requestId"]?.jsonPrimitive?.intOrNull?.let { pending[it]?.complete(payload) }
    }

    private fun onMessage(session: Session, message: CastChannel.Message) {
        val payload = parse(message.payload) ?: return
        payload["requestId"]?.jsonPrimitive?.intOrNull?.let { pending[it]?.complete(payload) }
        when (message.namespace) {
            CastChannel.NS_RECEIVER -> {
                // Stopped from the TV, the phone or Google Home
                if (payload["type"]?.jsonPrimitive?.content == "RECEIVER_STATUS" && payload.runningApp() == null) {
                    closeSession(session)
                }
            }
            CastChannel.NS_CONNECTION -> {
                if (payload["type"]?.jsonPrimitive?.content == "CLOSE" && message.source == session.transportId) {
                    closeSession(session)
                }
            }
            CastChannel.NS_MEDIA -> if (payload["type"]?.jsonPrimitive?.content == "MEDIA_STATUS") onMediaStatus(session, payload)
            NS_CONTROL -> runCatching { json.decodeFromString<TvControlMessage>(message.payload) }
                .onSuccess { onControl(session, it) }
        }
    }

    private fun onMediaStatus(session: Session, payload: JsonObject) {
        val status = payload["status"]?.jsonArray?.firstOrNull()?.jsonObject ?: return
        val previous = session.media.value
        val mediaSessionId = status["mediaSessionId"]?.jsonPrimitive?.intOrNull ?: return
        // The media is only there when it changed
        val media = status["media"]?.jsonObject
        val sameMedia = previous?.mediaSessionId == mediaSessionId
        session.mutableMedia.value = Media(
            state = status["playerState"]?.jsonPrimitive?.content ?: "IDLE",
            positionMs = ((status["currentTime"]?.jsonPrimitive?.doubleOrNull ?: 0.0) * 1000).toLong(),
            durationMs = media?.get("duration")?.jsonPrimitive?.doubleOrNull?.let { (it * 1000).toLong() }
                ?: previous?.durationMs?.takeIf { sameMedia } ?: 0L,
            mediaSessionId = mediaSessionId,
            customData = media?.get("customData")?.jsonObject ?: previous?.customData?.takeIf { sameMedia },
            idleReason = status["idleReason"]?.jsonPrimitive?.content,
        )
    }

    /** The TV's requests: save the watch time, switch episodes, mirror its track choices. */
    private fun onControl(session: Session, message: TvControlMessage) {
        if (message.type == "state") {
            CastRemoteState.selection.value = CastRemoteState.Selection(
                sourceIndex = message.sourceIndex,
                subtitleIndex = message.subtitleIndex,
                audioIndex = message.audioIndex,
            )
            return
        }
        val customData = session.media.value?.customData ?: return
        val episodeId = customData["episodeId"]?.jsonPrimitive?.longOrNull ?: return
        when (message.type) {
            "progress", "save" -> saveTime(episodeId, customData.isSeen(), message.duration, message.position)
            "next" -> switchNeighbour(session, customData, forward = true)
            "previous" -> switchNeighbour(session, customData, forward = false)
            "selectEpisode" -> message.episodeId?.let { targetId ->
                scope.launch {
                    val target = runCatching { Injekt.get<EpisodeRepository>().getEpisodeById(targetId) }.getOrNull()
                    if (target != null) switchEpisode(session, target)
                }
            }
        }
    }

    private fun switchNeighbour(session: Session, customData: JsonObject, forward: Boolean) {
        val animeId = customData["animeId"]?.jsonPrimitive?.longOrNull ?: return
        val currentId = customData["episodeId"]?.jsonPrimitive?.longOrNull ?: return
        scope.launch {
            val episodes = runCatching { Injekt.get<EpisodeRepository>().getEpisodesByAnimeId(animeId) }
                .getOrNull()?.sortedBy { it.sourceOrder } ?: return@launch
            val index = episodes.indexOfFirst { it.id == currentId }
            if (index < 0) return@launch
            val target = if (forward) episodes.getOrNull(index - 1) else episodes.getOrNull(index + 1)
            if (target != null) switchEpisode(session, target)
        }
    }

    /** Loads [target] on the TV at its saved time, its sources fetched here: the player screen may be closed. */
    private suspend fun switchEpisode(session: Session, target: Episode) {
        session.media.value?.let { saveTime(it.episodeId, it.customData.isSeen(), it.durationMs, it.positionMs) }
        val anime = runCatching { Injekt.get<AnimeRepository>().getAnimeById(target.animeId) }.getOrNull() ?: return
        val episodeRepository = Injekt.get<EpisodeRepository>()
        val fresh = runCatching { episodeRepository.getEpisodeById(target.id) }.getOrNull() ?: target
        val episodes = runCatching { episodeRepository.getEpisodesByAnimeId(target.animeId) }
            .getOrNull()?.sortedBy { it.sourceOrder } ?: listOf(fresh)
        val resume = if (fresh.seen) 0L else fresh.timeSeen

        fetchJob?.cancel()
        LocalCast.resolve(fresh, proxy)?.let { local ->
            send(session, CastChannel.NS_MEDIA, payload(fresh, anime, local.source, listOf(local.source), episodes, resume, local.contentUrl))
            return
        }
        fetchJob = scope.launch {
            val sources = try {
                Injekt.get<SourceManager>().getOrStub(anime.source).callGuarded { getEpisodeSources(fresh.toSEpisode()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "No sources for ${fresh.name}", e)
                return@launch
            }
            val selected = sources.firstOrNull() ?: return@launch
            send(session, CastChannel.NS_MEDIA, payload(fresh, anime, selected, sources, episodes, resume, selected.url))
        }
    }

    private fun JsonObject?.isSeen(): Boolean = this?.get("seen")?.jsonPrimitive?.booleanOrNull == true

    private fun saveTime(episodeId: Long?, seen: Boolean, totalTime: Long, timeSeen: Long) {
        if (episodeId == null || episodeId == 0L || seen || totalTime <= 0L || timeSeen <= 999L) return
        scope.launch {
            val threshold = Injekt.get<DataStore<Preferences>>().getPreferencesGroup(PlayerPreferences).seenThreshold
            val interactor = Injekt.get<UpdateAnimeInteractor>()
            if (isEpisodeWatched(timeSeen, totalTime, threshold)) {
                interactor.awaitSeenEpisodeUpdate(setOf(episodeId), true)
            } else {
                interactor.awaitSeenEpisodeTimeUpdate(Episode.create().copy(id = episodeId), totalTime, timeSeen)
            }
        }
    }

    /** The sources of the media playing, from its customData: for the player screen following the TV. */
    fun castSources(media: Media): Pair<StreamSource, List<StreamSource>>? {
        val customData = media.customData ?: return null
        fun string(key: String): String? = (customData[key] as? JsonPrimitive)?.content
        return runCatching {
            val selected = json.decodeFromString<StreamSource>(string("selectedSource")!!)
            val sources = json.decodeFromString<List<StreamSource>>(string("availableSources")!!)
            selected to sources
        }.getOrNull()
    }
}
