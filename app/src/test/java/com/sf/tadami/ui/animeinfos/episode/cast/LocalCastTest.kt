package com.sf.tadami.ui.animeinfos.episode.cast

import com.sf.tadami.data.download.EpisodeMetadata
import com.sf.tadami.data.download.MediaFileMetadata
import com.sf.tadami.data.download.SegmentMetadata
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.animeinfos.episode.cast.LocalCast.EpisodeFile
import com.sf.tadami.ui.animeinfos.episode.cast.LocalCast.Plan
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.LocalFile
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.LocalMedia
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.LocalPlaylist
import okhttp3.Headers.Companion.headersOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCastTest {

    private val dir = "content://com.android.externalstorage.documents/tree/primary%3ATadami/document/primary%3ATadami%2FEp"
    private val segments = listOf(
        SegmentMetadata(offset = 0, length = 1_000_000, durationUs = 10_000_000),
        SegmentMetadata(offset = 1_000_000, length = 500_000, durationUs = 5_000_000),
    )
    private val audioSegments = listOf(
        SegmentMetadata(offset = 0, length = 20_000, durationUs = 10_000_000),
        SegmentMetadata(offset = 20_000, length = 10_000, durationUs = 5_000_000),
    )
    private val tsVideo = EpisodeFile("Ep.ts", "$dir.ts", 1_500_000)
    private val audio = EpisodeFile("Ep.audio.Japanese.ts", "$dir.audio.Japanese.ts", 30_000)
    private val subtitle = EpisodeFile("Ep.English.vtt", "$dir.English.vtt", 2_000)

    private fun metadata(video: MediaFileMetadata, audios: List<MediaFileMetadata> = emptyList()) =
        EpisodeMetadata(version = EpisodeMetadata.VERSION, fullName = "Kwik - 720p", video = video, audios = audios)

    private val tsMetadata = metadata(MediaFileMetadata("Ep.ts", "ts", segments))

    private fun online(plan: Plan) = assertTrue(plan.toString(), plan is Plan.Online)

    @Test
    fun aJoinedTsIsCastAsAByteRangePlaylist() {
        assertEquals(
            Plan.Phone(
                media = LocalMedia(
                    key = tsVideo.uri,
                    files = mapOf("video.ts" to LocalFile(tsVideo.uri, "video/mp2t")),
                    playlists = mapOf("video.m3u8" to LocalPlaylist.Media("video.ts", segments)),
                ),
                video = "video.m3u8",
                audios = emptyList(),
                subtitles = emptyList(),
                master = null,
            ),
            LocalCast.plan(tsMetadata, tsVideo, emptyList(), emptyList()),
        )
    }

    @Test
    fun aProgressiveFileIsCastAsItIs() {
        val mp4 = EpisodeFile("Ep.mp4", "$dir.mp4", 9_000_000)
        val plan = LocalCast.plan(metadata(MediaFileMetadata("Ep.mp4", "mp4")), mp4, emptyList(), emptyList())
        assertEquals(mapOf("video.mp4" to LocalFile(mp4.uri, "video/mp4")), (plan as Plan.Phone).media.files)
        assertEquals(emptyMap<String, LocalPlaylist>(), plan.media.playlists)
        assertEquals("video.mp4", plan.video)
        // fMP4 joined from HLS: a fragmented MP4, played from the file too
        val fmp4 = LocalCast.plan(metadata(MediaFileMetadata("Ep.mp4", "mp4", segments)), mp4, emptyList(), emptyList())
        assertEquals("video.mp4", (fmp4 as Plan.Phone).video)
        val webm = EpisodeFile("Ep.webm", "$dir.webm", 9_000_000)
        val webmPlan = LocalCast.plan(metadata(MediaFileMetadata("Ep.webm", "webm")), webm, emptyList(), emptyList())
        assertEquals(LocalFile(webm.uri, "video/webm"), (webmPlan as Plan.Phone).media.files["video.webm"])
    }

    @Test
    fun separateAudioGetsItsOwnPlaylistAndTheMaster() {
        val plan = LocalCast.plan(
            metadata(tsMetadata.video, listOf(MediaFileMetadata(audio.name, "ts", audioSegments))),
            tsVideo,
            listOf(audio to "Japanese"),
            listOf(subtitle to "text/vtt"),
        ) as Plan.Phone
        assertEquals(listOf("audio0.m3u8"), plan.audios)
        assertEquals(LocalFile(audio.uri, "video/mp2t"), plan.media.files["audio0.ts"])
        assertEquals(LocalPlaylist.Media("audio0.ts", audioSegments), plan.media.playlists["audio0.m3u8"])
        assertEquals("master.m3u8", plan.master)
        assertEquals(
            // 800 kb/s of video at most, 16 kb/s of audio
            LocalPlaylist.Master(video = "video.m3u8", audios = listOf("audio0.m3u8" to "Japanese"), bandwidth = 816_000),
            plan.media.playlists["master.m3u8"],
        )
        assertEquals(listOf("sub0.vtt"), plan.subtitles)
        assertEquals(LocalFile(subtitle.uri, "text/vtt"), plan.media.files["sub0.vtt"])
    }

    @Test
    fun olderDownloadsAreCastOnline() {
        // No sidecar
        online(LocalCast.plan(null, tsVideo, emptyList(), emptyList()))
        // Resumed from a version that didn't record the segments
        online(LocalCast.plan(metadata(MediaFileMetadata("Ep.ts", "ts")), tsVideo, emptyList(), emptyList()))
        online(LocalCast.plan(metadata(tsMetadata.video, listOf(MediaFileMetadata(audio.name, "ts"))), tsVideo, listOf(audio to "Japanese"), emptyList()))
    }

    @Test
    fun containersTheReceiverDoesntPlayAreCastOnline() {
        val mkv = EpisodeFile("Ep.mkv", "$dir.mkv", 9_000_000)
        online(LocalCast.plan(metadata(MediaFileMetadata("Ep.mkv", "mkv")), mkv, emptyList(), emptyList()))
        val unknown = EpisodeFile("Ep", "$dir", 9_000_000)
        online(LocalCast.plan(metadata(MediaFileMetadata("Ep", "")), unknown, emptyList(), emptyList()))
        // A progressive TS: no segments to serve as a playlist
        online(LocalCast.plan(metadata(MediaFileMetadata("Ep.ts", "ts")), tsVideo, emptyList(), emptyList()))
        // Separate audio the receivers can't merge: a progressive video, or an audio that isn't TS
        val mp4 = EpisodeFile("Ep.mp4", "$dir.mp4", 9_000_000)
        val m4a = EpisodeFile("Ep.audio.Japanese.m4a", "$dir.m4a", 30_000)
        online(
            LocalCast.plan(
                metadata(MediaFileMetadata("Ep.mp4", "mp4"), listOf(MediaFileMetadata(m4a.name, "m4a", audioSegments))),
                mp4,
                listOf(m4a to "Japanese"),
                emptyList(),
            ),
        )
        val aac = EpisodeFile("Ep.audio.Japanese.aac", "$dir.aac", 30_000)
        online(
            LocalCast.plan(
                metadata(tsMetadata.video, listOf(MediaFileMetadata(aac.name, "aac", audioSegments))),
                tsVideo,
                listOf(aac to "Japanese"),
                emptyList(),
            ),
        )
    }

    @Test
    fun filesThatDontMatchTheSidecarAreCastOnline() {
        // Another video than the sidecar's
        online(LocalCast.plan(tsMetadata, tsVideo.copy(name = "Other.ts"), emptyList(), emptyList()))
        // Shorter or longer than its segments
        online(LocalCast.plan(tsMetadata, tsVideo.copy(length = 1_200_000), emptyList(), emptyList()))
        online(LocalCast.plan(tsMetadata, tsVideo.copy(length = 1_600_000), emptyList(), emptyList()))
        // An audio file missing, or one the sidecar doesn't know
        val withAudio = metadata(tsMetadata.video, listOf(MediaFileMetadata(audio.name, "ts", audioSegments)))
        online(LocalCast.plan(withAudio, tsVideo, emptyList(), emptyList()))
        online(LocalCast.plan(tsMetadata, tsVideo, listOf(audio to "Japanese"), emptyList()))
        online(LocalCast.plan(withAudio, tsVideo, listOf(audio.copy(length = 25_000) to "Japanese"), emptyList()))
    }

    @Test
    fun theCastStreamIsTheDownloadedOneThroughTheProxy() {
        val plan = LocalCast.plan(
            metadata(tsMetadata.video, listOf(MediaFileMetadata(audio.name, "ts", audioSegments))),
            tsVideo,
            listOf(audio to "Japanese"),
            listOf(subtitle to "text/vtt"),
        ) as Plan.Phone
        val local = StreamSource(
            url = tsVideo.uri,
            fullName = "Kwik - 720p",
            quality = "720p",
            server = "Kwik",
            headers = headersOf("Referer", "https://kwik.cx/"),
            subtitleTracks = listOf(Track.SubtitleTrack(subtitle.uri, "English", "text/vtt")),
            audioTracks = listOf(Track.AudioTrack(audio.uri, "Japanese")),
        )
        val prefix = "http://192.168.1.18:8214/local-0123abcd-"
        assertEquals(
            StreamSource(
                url = "${prefix}video.m3u8",
                fullName = "Kwik - 720p",
                quality = "720p",
                server = "Kwik",
                headers = null,
                subtitleTracks = listOf(Track.SubtitleTrack("${prefix}sub0.vtt", "English", "text/vtt")),
                audioTracks = listOf(Track.AudioTrack("${prefix}audio0.m3u8", "Japanese")),
            ),
            LocalCast.castSource(local, plan, prefix),
        )
    }
}
