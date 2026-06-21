# Tadami TV — Cast Connect integration (phone side)

This branch (`feature/tv-cast`) integrates the phone app with the **`tadami-tv-cast`**
Android TV receiver app (a sibling Gradle project) using **Google Cast Connect**.

## Why there is (almost) no phone code change

The phone is already configured as a Cast Connect sender. In
[`CastOptionsProvider.kt`](app/src/main/java/com/sf/tadami/ui/animeinfos/episode/cast/CastOptionsProvider.kt):

```kotlin
.setReceiverApplicationId(context.getString(R.string.cast_receiver_id)) // "DA2F4B1A"
LaunchOptions.Builder().setAndroidReceiverCompatible(true)              // Cast Connect ON
```

`setAndroidReceiverCompatible(true)` tells the Cast SDK to launch a registered **Android
TV receiver app** when casting to an Android TV / Google TV, instead of (or in addition
to) the web receiver. The phone already sends everything the receiver needs in
`EpisodeActivity.loadRemoteMedia()`:

- `contentUrl` — a direct stream URL, or the local Http4k proxy URL
  `http://<phoneIp>:8000?url=<enc>&headers=<enc-json>` for header-bound `.mp4`
  (header injection stays on the phone — the receiver plays `contentUrl` as-is).
- Subtitle `MediaTrack`s, `MediaMetadata` (title/episode/thumbnail), resume `currentTime`,
  and `customData` (`availableSources`, `selectedSource`, `episodeId`, `seen`, ...).

So **no sender code needs to change** for the receiver to work — only external
registration.

## Required: Google Cast SDK Developer Console registration

Cast Connect to a **sideloaded** (non-Play-Store) Android TV app requires:

1. **App registration** — in the Cast Console, associate the receiver app's package
   name **`com.sf.tadami.tv.cast`** with Cast App ID **`DA2F4B1A`** (the value of
   `cast_receiver_id`) as the Android TV app.
2. **Device registration** — register the test TV's **Google Cast serial number**
   (Settings → Device Preferences → Google Cast → Serial number) as a test device,
   then reboot the TV (allow ~15 min).
3. A **physical** Android TV / Google TV is required — the standard ATV emulator can't
   exercise Cast Connect.

## Debug build caveat

The phone debug build uses a **different** receiver id:
[`app/src/debug/res/values/config.xml`](app/src/debug/res/values/config.xml) →
`cast_receiver_id = "85AB1CC3"`, while release uses `DA2F4B1A`. The receiver app's debug
build also appends `.debug` to its package (`com.sf.tadami.tv.cast.debug`).

For an end-to-end test, align the triple {sender receiver App ID, console-registered ATV
package, installed ATV package}. Either:

- test with the **release** sender (`DA2F4B1A`) + a release receiver build whose package
  (`com.sf.tadami.tv.cast`) is registered; **or**
- register `com.sf.tadami.tv.cast.debug` under `DA2F4B1A` too and point the phone's
  **debug** `config.xml` at `DA2F4B1A` (a debug-only change — never touch release config).

## Test path

1. Install the `tadami-tv-cast` app on the registered TV; reboot.
2. Phone + TV on the same Wi-Fi (the proxy at `http://<phoneIp>:8000` must be reachable).
3. Open an episode → tap the Cast button → pick the TV → the receiver app launches via
   Cast Connect, receives the load, plays, renders subtitles, honors resume.
4. From the phone `CastVideoPlayer`: play/pause, seek, ±, +85s, subtitle change, and
   episode/source switch (phone re-`load`) all drive the TV.
