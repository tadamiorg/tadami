package com.sf.tadami.data.download.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaCheckTest {

    private val mp4 = byteArrayOf(0, 0, 0, 0x20) + "ftypisom".encodeToByteArray() + ByteArray(16)
    private val mkv = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()) + ByteArray(16)
    private val ts = byteArrayOf(0x47, 0x40, 0x00, 0x10) + ByteArray(16)
    private val adts = byteArrayOf(0xFF.toByte(), 0xF1.toByte(), 0x50, 0x80.toByte()) + ByteArray(16)
    private val id3 = "ID3".encodeToByteArray() + ByteArray(16)

    @Test
    fun pagesAreNotVideos() {
        assertTrue(MediaCheck.looksLikeText("<!DOCTYPE html><html><head>".encodeToByteArray()))
        assertTrue(MediaCheck.looksLikeText("  \n<html lang=\"en\">".encodeToByteArray()))
        assertTrue(MediaCheck.looksLikeText("{\"error\":\"File not found\"}".encodeToByteArray()))
        assertTrue(MediaCheck.looksLikeText(ByteArray(0)))
        assertFalse(MediaCheck.looksLikeText(mp4))
        assertFalse(MediaCheck.looksLikeText(ts))
        assertFalse(MediaCheck.looksLikeText(mkv))

        assertTrue(MediaCheck.isTextContentType("text/html; charset=UTF-8"))
        assertTrue(MediaCheck.isTextContentType("application/json"))
        assertFalse(MediaCheck.isTextContentType("video/mp4"))
        assertFalse(MediaCheck.isTextContentType("application/octet-stream"))
        assertFalse(MediaCheck.isTextContentType(null))
    }

    @Test
    fun playlistsAreRecognized() {
        assertTrue(MediaCheck.isPlaylist("#EXTM3U\n#EXT-X-VERSION:3".encodeToByteArray()))
        assertTrue(MediaCheck.isPlaylist("﻿#EXTM3U".encodeToByteArray()))
        assertFalse(MediaCheck.isPlaylist(ts))
        assertTrue(MediaCheck.isPlaylistContentType("application/vnd.apple.mpegurl"))
        assertTrue(MediaCheck.isPlaylistContentType("audio/x-mpegURL"))
        assertTrue(MediaCheck.isPlaylistUrl("https://cdn/hls/master.m3u8?token=1"))
        assertFalse(MediaCheck.isPlaylistUrl("https://cdn/video.mp4?file=a.m3u8"))
    }

    @Test
    fun theVideoExtensionComesFromTheTypeTheUrlThenTheContent() {
        assertEquals("mkv", MediaCheck.videoExtension("video/x-matroska", "https://h/v", mp4))
        assertEquals("webm", MediaCheck.videoExtension("application/octet-stream", "https://h/v.webm?x=1", mp4))
        assertEquals("mkv", MediaCheck.videoExtension("application/octet-stream", "https://h/download", mkv))
        assertEquals("ts", MediaCheck.videoExtension(null, "https://h/download", ts))
        assertEquals("mp4", MediaCheck.videoExtension(null, "https://h/download", ByteArray(20)))
    }

    @Test
    fun joinedSegmentsGetTheirContainer() {
        assertEquals("mp4", MediaCheck.hlsExtension(fragmentedMp4 = true, audio = false, firstSegment = ByteArray(0)))
        assertEquals("m4a", MediaCheck.hlsExtension(fragmentedMp4 = true, audio = true, firstSegment = ByteArray(0)))
        assertEquals("ts", MediaCheck.hlsExtension(false, false, ts))
        assertEquals("ts", MediaCheck.hlsExtension(false, true, ts))
        assertEquals("aac", MediaCheck.hlsExtension(false, true, adts))
        assertEquals("aac", MediaCheck.hlsExtension(false, true, id3))
        // Unknown: TS, what HLS mostly is
        assertEquals("ts", MediaCheck.hlsExtension(false, false, ByteArray(20)))
    }

    @Test
    fun subtitlesKeepTheirFormat() {
        assertEquals("vtt", MediaCheck.subtitleExtension("text/vtt", "https://h/s", ByteArray(0)))
        assertEquals("ass", MediaCheck.subtitleExtension("text/x-unknown", "https://h/s.ass?t=1", ByteArray(0)))
        assertEquals("srt", MediaCheck.subtitleExtension(null, "https://h/s", "1\n00:00:01,000 --> 00:00:02,000\nHi".encodeToByteArray()))
        assertEquals("vtt", MediaCheck.subtitleExtension(null, "https://h/s", "WEBVTT\n\n".encodeToByteArray()))
        assertEquals("ass", MediaCheck.subtitleExtension(null, "https://h/s", "[Script Info]\n".encodeToByteArray()))

        assertEquals("text/vtt", MediaCheck.subtitleMimeType("vtt"))
        assertEquals("application/x-subrip", MediaCheck.subtitleMimeType("SRT"))
        assertEquals("text/x-ssa", MediaCheck.subtitleMimeType("ass"))
    }

    @Test
    fun placeholdersAreTooSmallOrTooShort() {
        // The 36 kB clip a host served instead of the episode
        assertTrue(MediaCheck.isTooSmall(36L * 1024))
        assertFalse(MediaCheck.isTooSmall(200L * 1024 * 1024))
        assertTrue(MediaCheck.isTooShort(8_000))
        assertFalse(MediaCheck.isTooShort(24 * 60_000))
        // Unknown, as TS files often are: not judged
        assertFalse(MediaCheck.isTooShort(null))
        assertFalse(MediaCheck.isTooShort(0))
    }
}
