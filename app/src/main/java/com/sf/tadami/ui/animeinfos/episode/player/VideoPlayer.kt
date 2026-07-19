package com.sf.tadami.ui.animeinfos.episode.player

import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.network.player.PlayerNetworkHelper
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.animeinfos.episode.player.controls.PlayerControls
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.EpisodesDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.episodetooltip.EpisodeTooltipDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.settings.SettingsDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.audioselection.AudioSelectionDialog
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.tracksselection.TracksSelectionDialog
import com.sf.tadami.ui.animeinfos.episode.player.subtitles.PlayerSubtitleView
import com.sf.tadami.ui.animeinfos.episode.player.subtitles.SubtitleStyle
import com.sf.tadami.ui.animeinfos.episode.player.controls.dialogs.videoselection.VideoSelectionDialog
import com.sf.tadami.ui.animeinfos.episode.player.subtitles.CustomSubtitleParserFactory
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.convertToIetfLanguageTag
import com.sf.tadami.ui.utils.padding
import com.sf.tadami.ui.utils.safeDrawing
import kotlinx.coroutines.delay
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

/** Prefix used to tag each side-loaded subtitle so it can be selected by an explicit override. */
private const val SUBTITLE_ID_PREFIX = "tadami_sub_"

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    playerNetworkHelper: PlayerNetworkHelper = Injekt.get(),
    dispatcher: OnBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher,
    @SuppressLint("ContextCastToActivity") playerViewModel: PlayerViewModel = viewModel(LocalContext.current as EpisodeActivity),
    setPlayer: (ExoPlayer) -> Unit,
    onWebViewOpen: () -> Unit,
    setPipMode: () -> Unit,
    refreshPipUi: () -> Unit
) {
    val context = LocalContext.current

    val playerPreferences by rememberDataStoreState(customPrefs = PlayerPreferences).value.collectAsState()
    val advancedPreferences by rememberDataStoreState(customPrefs = AdvancedPreferences).value.collectAsState()

    val playerScreenLoading by playerViewModel.playerScreenLoading.collectAsState()
    val lockedControls by playerViewModel.lockedControls.collectAsState()
    val playerInitiatedPause by playerViewModel.playerInitiatedPause.collectAsState()

    val episodeUiState by playerViewModel.uiState.collectAsState()
    val currentEpisode by playerViewModel.currentEpisode.collectAsState()
    val episodes by playerViewModel.episodes.collectAsState()

    val isFetchingSources by playerViewModel.isFetchingSources.collectAsState()

    val anime by playerViewModel.anime.collectAsState()

    val hasNextIterator by playerViewModel.hasNextIterator.collectAsState()
    val hasPreviousIterator by playerViewModel.hasPreviousIterator.collectAsState()

    val upstreamDataSource = DefaultDataSource.Factory(context)

    val cacheDataSourceFactory = remember {
        CacheDataSource.Factory()
            .setCache(playerNetworkHelper.cache)
            .setUpstreamDataSourceFactory(upstreamDataSource)
    }

    val resolvingDataSource = ResolvingDataSource.Factory(
        cacheDataSourceFactory
    ) { dataSpec ->
        if (episodeUiState.selectedSource?.headers == null) {
            dataSpec.withRequestHeaders(
                mapOf(
                    Pair(
                        "User-Agent",
                        advancedPreferences.userAgent
                    )
                )
            )
        } else {
            dataSpec.withRequestHeaders(playerViewModel.getSourceHeaders())
        }
    }

    val dataSourceFactory = remember {
        DefaultMediaSourceFactory(context).apply {
            setDataSourceFactory(resolvingDataSource)
            setSubtitleParserFactory(CustomSubtitleParserFactory())
        }
    }

    val loadControl = remember {
        DefaultLoadControl.Builder().apply {
            // Keep a generous forward time target for flaky scraper streams, but cap the heap by bytes so
            // high-bitrate HLS can't fill ~10 min of samples and OOM (crash: DefaultAllocator.allocate ->
            // SampleDataQueue.preAppend). prioritizeTimeOverSizeThresholds=false makes the byte cap authoritative.
            setBufferDurationsMs(30_000, 180_000, 1_500, 3_500)
            setBackBuffer(30_000, true)
            setTargetBufferBytes(64 * 1024 * 1024)
            setPrioritizeTimeOverSizeThresholds(false)
        }.build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .apply {
                setSeekBackIncrementMs(playerPreferences.doubleTapLength)
                setSeekForwardIncrementMs(playerPreferences.doubleTapLength)
                setMediaSourceFactory(dataSourceFactory)
                setLoadControl(loadControl)

            }
            .build()
    }

    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    LaunchedEffect(exoPlayer) {
        setPlayer(exoPlayer)
    }

    LaunchedEffect(exoPlayer.isPlaying) {
        refreshPipUi()
    }

    val episodeNumber by remember(currentEpisode) { derivedStateOf { currentEpisode?.episodeNumber } }

    var shouldShowControls by remember { mutableStateOf(true) }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    var totalDuration by remember { mutableLongStateOf(0L) }

    var currentTime by remember { mutableLongStateOf(0L) }

    var bufferedPercentage by remember { mutableIntStateOf(0) }

    var playbackState by remember { mutableIntStateOf(exoPlayer.playbackState) }

    var openVideoSelectionDialog by remember { mutableStateOf(false) }

    var openTracksSelectionDialog by remember { mutableStateOf(false) }
    var openAudioSelectionDialog by remember { mutableStateOf(false) }

    var openSettingsDialog by remember { mutableStateOf(false) }

    var openEpisodesDialog by remember { mutableStateOf(false) }

    var openEpisodeTooltipDialog by remember { mutableStateOf(false) }

    fun updateTime() {
        (context as EpisodeActivity).setUpdateTimeJob(
            playerViewModel.updateTime(
                currentEpisode,
                totalDuration,
                currentTime,
                playerPreferences.seenThreshold
            )
        )
    }

    fun selectEpisode(episode: Episode) {
        updateTime()
        playerViewModel.setCurrentEpisode(episode)
    }

    LaunchedEffect(key1 = episodeUiState.loadError) {
        if (episodeUiState.loadError) {
            exoPlayer.release()
            dispatcher.onBackPressed()
        }
    }
    // Selects the subtitle track by an explicit override keyed on the config id (like the cast path
    // selects by track id), instead of ExoPlayer language-preference matching which is unreliable for
    // side-loaded subtitles. Kept fresh via rememberUpdatedState so the player listener sees current state.
    val selectSubtitle by rememberUpdatedState<(Tracks) -> Unit>(fun(tracks: Tracks) {
        val source = episodeUiState.selectedSource ?: return
        val subs = source.subtitleTracks.filter { it.mimeType != MimeTypes.TEXT_UNKNOWN }
        val enabled = playerPreferences.subtitlesEnabled
        if (!enabled || subs.isEmpty()) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
                .build()
            return
        }
        val manual = episodeUiState.selectedSubtitleTrack
        val prefs = playerPreferences.subtitlePrefLanguages.split(",")
        // Which subtitle we want: the manually chosen one, else the first that matches the
        // ordered subtitle-language preference.
        val desiredIndex = when {
            manual != null -> subs.indexOf(manual).takeIf { it >= 0 }
            else -> prefs.firstNotNullOfOrNull { pref ->
                subs.indexOfFirst { it.lang.convertToIetfLanguageTag().equals(pref, ignoreCase = true) }
                    .takeIf { it >= 0 }
            }
        } ?: return

        // Find the text track group whose format id is the one we tagged in the SubtitleConfiguration.
        // media3 prefixes ids with a "<period>:<group>:" tag when sources are merged (the audio
        // MergingMediaSource wrapping the subtitle-merged video), so match by suffix as well as exact.
        val targetId = "$SUBTITLE_ID_PREFIX$desiredIndex"
        fun matchesId(id: String?) = id == targetId || id?.endsWith(":$targetId") == true
        var group: Tracks.Group? = null
        var trackInGroup = 0
        for (g in tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }) {
            for (i in 0 until g.length) {
                if (matchesId(g.getTrackFormat(i).id)) {
                    group = g
                    trackInGroup = i
                    break
                }
            }
            if (group != null) break
        }

        val target = group
        if (target == null) {
            // Fallback (id not resolvable yet): use language preference so subtitles still show.
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .apply {
                    if (manual != null) setPreferredTextLanguage(manual.lang.convertToIetfLanguageTag())
                    else setPreferredTextLanguages(*prefs.toTypedArray())
                }
                .build()
            return
        }

        if (target.isSelected && target.isTrackSelected(trackInGroup)) return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(TrackSelectionOverride(target.mediaTrackGroup, trackInGroup))
            .build()
    })

    // Selects the audio track by overriding the merged audio track-group at the index of the chosen
    // audioTrack (default 0 = the preferred/Japanese track the extension puts first). Seamless — no rebuild.
    val selectAudio by rememberUpdatedState<(Tracks) -> Unit>(fun(tracks: Tracks) {
        val source = episodeUiState.selectedSource ?: return
        if (source.audioTracks.isEmpty()) return
        val desiredIndex = episodeUiState.selectedAudioTrack
            ?.let { sel -> source.audioTracks.indexOfFirst { it.lang == sel.lang } }
            ?.takeIf { it >= 0 } ?: 0
        val group = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }.getOrNull(desiredIndex) ?: return
        if (group.isSelected && group.isTrackSelected(0)) return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
            .build()
    })

    LaunchedEffect(
        playerPreferences.subtitlesEnabled,
        playerPreferences.subtitlePrefLanguages,
        episodeUiState.selectedSubtitleTrack,
        episodeUiState.selectedSource
    ) {
        selectSubtitle(exoPlayer.currentTracks)
    }

    LaunchedEffect(episodeUiState.selectedAudioTrack, episodeUiState.selectedSource) {
        selectAudio(exoPlayer.currentTracks)
    }

    LaunchedEffect(key1 = episodeUiState.selectedSource) {
        episodeUiState.selectedSource?.let { source ->
            // Drop stale text/audio overrides from the previous source; a lingering override pointing at the
            // old source's track group blocks the new source's subtitle from auto-selecting (onTracksChanged
            // re-applies the right one once the new tracks load).
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .build()
            playerViewModel.getDbEpisodeTime { timeSeen ->
                exoPlayer.playWhenReady = true
                val startPos = timeSeen.takeIf { it > 0 } ?: currentTime

                val videoItem = MediaItem.Builder().apply {
                    setUri(source.url)
                    if (source.subtitleTracks.isNotEmpty()) {
                        val subtitlesConfigurations = source.subtitleTracks.filter {
                            it.mimeType !== MimeTypes.TEXT_UNKNOWN
                        }.mapIndexed { index, sub ->
                            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                                .setMimeType(sub.mimeType)
                                .setLanguage(sub.lang.convertToIetfLanguageTag())
                                // Stable id so we can select this exact track via an override.
                                .setId("$SUBTITLE_ID_PREFIX$index")
                                .build()
                        }
                        setSubtitleConfigurations(subtitlesConfigurations)
                    }
                }.build()

                if (source.audioTracks.isNotEmpty()) {
                    // Demuxed HLS: the variant URL is video-only, so merge the separate audio group(s). Each
                    // audio source becomes one audio track group (the video variant contributes none), so
                    // selectAudio can override by index. createMediaSource keeps the side-loaded subtitles.
                    val videoSource = dataSourceFactory.createMediaSource(videoItem)
                    val audioSources = source.audioTracks.map { audio ->
                        dataSourceFactory.createMediaSource(MediaItem.fromUri(audio.url))
                    }
                    val merged = MergingMediaSource(
                        /* adjustPeriodTimeOffsets = */ true,
                        /* clipDurations = */ true,
                        videoSource,
                        *audioSources.toTypedArray(),
                    )
                    exoPlayer.setMediaSource(merged, startPos)
                } else {
                    exoPlayer.setMediaItem(videoItem, startPos)
                }
                exoPlayer.prepare()
                refreshPipUi()
            }
        }
    }

    if (playerPreferences.autoPlay) {
        LaunchedEffect(exoPlayer.isPlaying.not() && playbackState == STATE_ENDED) {
            val autoIdle = exoPlayer.isPlaying.not() && playbackState == STATE_ENDED
            if (currentTime > 0L && totalDuration > 0L) {
                if (autoIdle && hasNextIterator.hasPrevious()) {
                    val next = hasNextIterator.previous()
                    exoPlayer.clearMediaItems()
                    selectEpisode(next)
                }
            }
        }
    }

    if (isPlaying) {
        LaunchedEffect(Unit) {
            while (true) {
                currentTime =
                    exoPlayer.currentPosition.coerceAtLeast(0L).coerceAtMost(totalDuration)
                delay(1.seconds / 30)
            }
        }
    }

    // Subtitles are drawn by the custom PlayerSubtitleView overlay; hide the built-in SubtitleView.
    LaunchedEffect(playerView) {
        playerView?.subtitleView?.visibility = View.GONE
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    ContentLoader(
        modifier = if (!playerPreferences.ignoreCutout) Modifier.safeDrawing() else Modifier,
        isLoading = playerScreenLoading,
        delay = 500
    ) {
        Box(modifier = modifier) {
            if (episodeUiState.availableSources.isNotEmpty()) {
                VideoSelectionDialog(
                    opened = openVideoSelectionDialog,
                    sources = episodeUiState.availableSources,
                    onSelectSource = {
                        updateTime()
                        playerViewModel.selectSource(it)
                    },
                    selectedSource = episodeUiState.selectedSource,
                    onDismissRequest = {
                        openVideoSelectionDialog = false
                        if (!playerInitiatedPause) {
                            exoPlayer.play()
                        }
                    }
                )
                // The track that is actually playing: the manual choice, else the preference-matched one
                // (auto). Used only for highlighting so it never fights the player selection.
                val effectiveSubtitle = remember(
                    episodeUiState.selectedSubtitleTrack,
                    episodeUiState.selectedSource,
                    playerPreferences.subtitlePrefLanguages,
                    playerPreferences.subtitlesEnabled,
                ) {
                    if (!playerPreferences.subtitlesEnabled) null
                    else episodeUiState.selectedSubtitleTrack ?: run {
                        val subs = episodeUiState.selectedSource?.subtitleTracks
                            ?.filter { it.mimeType != MimeTypes.TEXT_UNKNOWN }.orEmpty()
                        playerPreferences.subtitlePrefLanguages.split(",").firstNotNullOfOrNull { pref ->
                            subs.firstOrNull { it.lang.convertToIetfLanguageTag().equals(pref, ignoreCase = true) }
                        }
                    }
                }
                TracksSelectionDialog(
                    opened = openTracksSelectionDialog,
                    subtitleTracks = episodeUiState.selectedSource?.subtitleTracks,
                    selectedSubtitleTrack = effectiveSubtitle,
                    onSubtitleTrackSelected = {
                        playerViewModel.selectedSubtitleTrack(it)
                    },
                    onDismissRequest = {
                        openTracksSelectionDialog = false
                        if (!playerInitiatedPause) {
                            exoPlayer.play()
                        }
                    }
                )
                // The audio that is actually playing: the manual choice, else the first track (the extension
                // puts the preferred/Japanese one first). Used for the dialog highlight.
                val effectiveAudio = episodeUiState.selectedAudioTrack
                    ?: episodeUiState.selectedSource?.audioTracks?.firstOrNull()
                AudioSelectionDialog(
                    opened = openAudioSelectionDialog,
                    audioTracks = episodeUiState.selectedSource?.audioTracks.orEmpty(),
                    selectedAudioTrack = effectiveAudio,
                    onAudioTrackSelected = {
                        playerViewModel.selectedAudioTrack(it)
                    },
                    onDismissRequest = {
                        openAudioSelectionDialog = false
                        if (!playerInitiatedPause) {
                            exoPlayer.play()
                        }
                    }
                )
            }

            EpisodesDialog(
                opened = openEpisodesDialog,
                onDismissRequest = {
                    openEpisodesDialog = false
                    if (!playerInitiatedPause) {
                        exoPlayer.play()
                    }
                },
                onConfirm = {
                    exoPlayer.clearMediaItems()
                    selectEpisode(it)
                },
                displayMode = anime?.displayMode,
                episodes = episodes,
                initialEpisode = currentEpisode
            )

            SettingsDialog(
                opened = openSettingsDialog,
                onDismissRequest = {
                    openSettingsDialog = false
                    if (!playerInitiatedPause) {
                        exoPlayer.play()
                    }
                },
                sourceDatastore = playerViewModel.sourceDataStore,
                sourcePrefsitems = playerViewModel.sourceDataStoreScreen
            )

            LaunchedEffect(Unit) {
                if (playerViewModel.sourceTooltipAuto == true) {
                    openEpisodeTooltipDialog = true
                }
            }

            EpisodeTooltipDialog(
                opened = openEpisodeTooltipDialog,
                onDismissRequest = {
                    openEpisodeTooltipDialog = false
                    if (!playerInitiatedPause) {
                        exoPlayer.play()
                    }
                },
                sourceDatastore = playerViewModel.sourceDataStore,
                tooltipContent = playerViewModel.sourceTooltipContent
                    ?: stringResource(R.string.player_epsiode_tooltip_no_content)
            )

            AndroidView(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                shouldShowControls = shouldShowControls.not()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { _, _ -> }
                    },
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        keepScreenOn = true

                        playerView = this

                        // Hidden — subtitles are drawn by the custom Compose overlay below.
                        subtitleView?.visibility = View.GONE
                    }
                }
            )

            PlayerSubtitleView(
                player = exoPlayer,
                style = SubtitleStyle(
                    textSize = playerPreferences.subtitleTextSize.toFloat(),
                    textColor = Color(playerPreferences.subtitleTextColor),
                    outlineColor = Color(playerPreferences.subtitleEdgeColor),
                    fontWeight = playerPreferences.subtitleFontWeight,
                    outlineFraction = playerPreferences.subtitleOutlineWidth / 100f,
                    letterSpacing = playerPreferences.subtitleLetterSpacing / 100f,
                    italic = playerPreferences.subtitleItalicFormat,
                ),
                modifier = Modifier.fillMaxSize(),
            )



            DisposableEffect(
                Unit
            ) {
                val listener = object : Player.Listener {

                    override fun onRenderedFirstFrame() {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .build()
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        // Apply the preferred/selected subtitle + audio once the tracks exist.
                        selectSubtitle(tracks)
                        selectAudio(tracks)
                    }

                    override fun onEvents(
                        player: Player, events: Player.Events
                    ) {
                        if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                            player.clearMediaItems()
                            player.prepare()
                            UiToasts.showToast(
                                stringRes = R.string.player_screen_source_load_error,
                                args = arrayOf("${episodeUiState.selectedSource?.fullName}")
                            )
                        } else {
                            super.onEvents(player, events)
                            totalDuration = player.duration.coerceAtLeast(0L)
                            currentTime =
                                player.currentPosition.coerceAtLeast(0L).coerceAtMost(totalDuration)
                            bufferedPercentage = player.bufferedPercentage
                            isPlaying = player.isPlaying
                            playbackState = player.playbackState
                        }
                    }
                }
                exoPlayer.addListener(listener)

                val observer = LifecycleEventObserver { _, event ->

                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> {
                            updateTime()
                            if (!lockedControls) {
                                exoPlayer.pause()
                            }
                        }

                        Lifecycle.Event.ON_RESUME -> {
                            if (!playerInitiatedPause) {
                                exoPlayer.play()
                            }
                        }

                        else -> {}
                    }
                }
                val lifecycle = lifecycleOwner.value.lifecycle
                lifecycle.addObserver(observer)

                exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT

                onDispose {
                    updateTime()
                    lifecycle.removeObserver(observer)
                    exoPlayer.removeListener(listener)
                    exoPlayer.release()
                    playerView = null
                }
            }

            ContentLoader(isLoading = isFetchingSources) {}

            val isVideoLoading by remember(exoPlayer.playbackState, exoPlayer.playWhenReady) {
                derivedStateOf {
                    exoPlayer.playbackState == Player.STATE_BUFFERING
                            && exoPlayer.playWhenReady
                }
            }

            if (isVideoLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .wrapContentSize(), strokeWidth = 3.dp
                )
            }

            PlayerControls(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.padding.large),
                isVisible = { shouldShowControls },
                isPlaying = isPlaying,
                title = { anime?.title ?: "" },
                episode = when (anime?.displayMode) {
                    is Anime.DisplayMode.NAME -> currentEpisode?.name
                        ?: "${stringResource(id = R.string.player_screen_episode_label)} $episodeNumber"

                    else -> "${stringResource(id = R.string.player_screen_episode_label)} $episodeNumber"
                },
                onReplay = { exoPlayer.seekTo(currentTime - playerPreferences.doubleTapLength) },
                onSkipOp = { exoPlayer.seekTo(currentTime + 85000) },
                onForward = { exoPlayer.seekTo(currentTime + playerPreferences.doubleTapLength) },
                isIdle = exoPlayer.isPlaying.not() && playbackState == STATE_ENDED,
                onTapYoutube = {
                    shouldShowControls = shouldShowControls.not()
                },
                playerSeekValue = playerPreferences.doubleTapLength,
                onPauseToggle = {
                    when {
                        exoPlayer.isPlaying -> {
                            playerViewModel.setPlayerInitadtedPause(true)
                            exoPlayer.pause()
                        }

                        exoPlayer.isPlaying.not() && playbackState == STATE_ENDED -> {
                            exoPlayer.seekTo(0)
                            exoPlayer.playWhenReady = true
                        }

                        else -> {
                            playerViewModel.setPlayerInitadtedPause(false)
                            exoPlayer.play()
                        }
                    }
                    isPlaying = isPlaying.not()
                },
                onStreamSettings = {
                    exoPlayer.pause()
                    openVideoSelectionDialog = openVideoSelectionDialog.not()
                },
                onTracksSettings = {
                    exoPlayer.pause()
                    openTracksSelectionDialog = openTracksSelectionDialog.not()
                },
                onAudioSettings = {
                    exoPlayer.pause()
                    openAudioSelectionDialog = true
                },
                audioSettingsEnabled = episodeUiState.selectedSource?.audioTracks?.isNotEmpty() == true,
                totalDuration = { totalDuration },
                currentTime = { currentTime },
                bufferedPercentage = { bufferedPercentage },
                onSeekChanged = { timeMs: Float ->
                    exoPlayer.seekTo(timeMs.toLong())
                },
                onBack = {
                    exoPlayer.release()
                    dispatcher.onBackPressed()
                },
                onNext = {
                    val next = hasNextIterator.previous()
                    exoPlayer.clearMediaItems()
                    selectEpisode(next)
                },
                onPrevious = {
                    val previous = hasPreviousIterator.next()
                    exoPlayer.clearMediaItems()
                    selectEpisode(previous)
                },
                hasNext = {
                    hasNextIterator.hasPrevious()
                },
                hasPrevious = {
                    hasPreviousIterator.hasNext()
                },
                videoSettingsEnabled = episodeUiState.availableSources.isNotEmpty(),
                tracksSettingsEnabled = (episodeUiState.selectedSource?.subtitleTracks?.isNotEmpty() == true) ||
                    (episodeUiState.selectedSource?.audioTracks?.isNotEmpty() == true),
                onEpisodesClicked = {
                    exoPlayer.pause()
                    openEpisodesDialog = true
                },
                onPlayerSettings = {
                    exoPlayer.pause()
                    openSettingsDialog = true
                },
                onPipClicked = setPipMode,
                lockedControls = lockedControls,
                onWebViewOpen = onWebViewOpen,
                episodeTooltipEnabled = playerViewModel.sourceSupportTooltip ?: false,
                onShowEpisodeTooltip = {
                    exoPlayer.pause()
                    openEpisodeTooltipDialog = openEpisodeTooltipDialog.not()
                }

            )

        }
    }
}




