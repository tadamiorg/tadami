# Tadami TV — Hybrid integration (phone side)

This branch (`feature/tv-hybrid`) integrates the phone app with the **`tadami-tv-hybrid`**
Android TV app, which is **both** a Cast Connect receiver **and** a WebRTC pairing target.
It combines the two single-transport branches:

- **Cast Connect** (primary): already configured on the phone (receiver id `DA2F4B1A` +
  `setAndroidReceiverCompatible(true)`), no sender code needed. See
  [`TADAMI_TV_CAST.md`](TADAMI_TV_CAST.md).
- **WebRTC pairing** (fallback): the full phone-side sender merged from `feature/tv-webrtc`
  (`ui/animeinfos/episode/webrtc/`).

## How the fallback works

The hybrid TV app advertises NSD + shows a pairing code **and** registers as a Cast
receiver at the same time. On the phone:

- The normal **Cast** button (`MediaRouteButton`) casts via Cast Connect when a cast
  route is available — Chromecast's usual path.
- The **"Cast to Tadami TV"** button (added in `EpisodeActivity`) opens the WebRTC connect
  dialog: it discovers the TV over NSD and pairs with the on-screen code. Use this when
  Chromecast is unavailable or fails (AP isolation, no cast route, etc.).

Both transports drive the **same** ExoPlayer + control overlay on the TV, so the
experience is identical once connected.

## Notes

- The Cast path keeps the existing port-8000 proxy for header-bound `.mp4`; the WebRTC
  path sends metadata only and the TV fetches media directly.
- For Cast Connect testing, the hybrid app's package `com.sf.tadami.tv` must be registered
  against `DA2F4B1A` in the Google Cast Console (+ a test-device serial, physical TV).
- The WebRTC path needs no server and works on any same-Wi-Fi pair; on Android 17 grant
  the local-network permission on both devices.
