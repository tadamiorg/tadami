# extensions-lib

The API Tadami extensions are built against. Its classes have the names and the JVM signatures of the app's own classes (`com.sf.tadami.source.*`, `com.sf.tadami.network.*`...), with bodies that only throw `Exception("Stub!")`. Extensions compile against it and never ship it: at runtime, the app loads them in a class loader that resolves those names to the app's real classes.

## Using it

It's published on [JitPack](https://jitpack.io/#tadamiorg/tadami) from the `lib-X.Y.Z` tags of this repository:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.tadamiorg:tadami:lib-1.6.0")
}
```

- **`compileOnly`, never `implementation`**: bundled in the APK, the stub's classes would shadow the app's, and every call would throw `Stub!`.
- It brings the libraries of its API as `api` dependencies, at the app's versions: OkHttp, Jsoup, RxJava 3, Injekt, kotlinx-serialization-json, kotlinx-coroutines-core, DataStore preferences, Compose UI (for `ImageVector` and `Color`) and androidx.annotation. The app provides them at runtime: don't bundle them either.
- Declare the lib version the extension needs, `major.minor`, as the prefix of its version name (in tadami-extensions-source, `set("apiVersion", "1.6")` gives `1.6.<extVersionCode>`). The app refuses an extension whose lib version is outside the range it supports (`LIB_TOO_OLD`, `LIB_TOO_NEW`) instead of letting it crash on a missing member.
- Build with Kotlin 2.3 or later: the lib is compiled with the app's Kotlin version (2.4), whose metadata older compilers can't read.

## Versions

A tag `lib-X.Y.Z`:

- `X.Y` is the lib version extensions declare, and the API version of the app: an app accepts the extensions from `ExtensionsLoader.API_VERSION_MIN` to `API_VERSION_MAX`. A new member raises it.
- `Z` is for a fix of the lib alone that changes no member, e.g. a wrong signature in the stub.

| Lib | What it adds |
| --- | --- |
| 1.5 | `SAnime.status` is an `SAnimeStatus`, no longer a `String`. The oldest version the app accepts. |
| 1.6 | `SAnime.language`, `NetworkHelper.webViewResolver` (`WebViewResolver`). Needs the app 1.10.0 or later: 1.9.3 accepts up to 1.5. |

`lib-1.6.0` is the first version published as a library: before, extensions compiled against a copy of the stub in tadami-extensions-source. Compared to that copy, it fixes the stub where it had drifted from the app: `Anime` only has its getters (`language` included), `DropDownAction` has `checked`, `OkhttpHeadersSerializer.serialize` returns `Unit`. `App`, `StubSource`, `UiToasts`, `Action` and `DropDownAction` are deprecated: they're part of the app, not of the extension API, and will leave the lib.

## What's in it

| Package | Classes |
| --- | --- |
| `com.sf.tadami.source` | `Source`, `AnimeCatalogueSource`, `ConfigurableSource` (1.7), `AnimesPage` |
| `com.sf.tadami.source.online` | `AnimeHttpSource`, `ParsedAnimeHttpSource` (Jsoup selectors), `ConfigurableParsedHttpAnimeSource` (with preferences), `ConfigurableAnimeHttpSource` (1.7, with preferences, without the selectors) |
| `com.sf.tadami.source.model` | `SAnime`, `SAnimeStatus`, `SEpisode`, `StreamSource`, `Track`, `AnimeFilter`, `AnimeFilterList` |
| `com.sf.tadami.network` | `NetworkHelper` (`client`, `webViewResolver`), `GET`/`POST`/`HEAD`, `PUT`/`DELETE` (1.7), `asJsoup`, `asCancelableObservable`, `await`, `awaitSuccess` and `HttpException` (1.7), `WebViewResolver` |
| `com.sf.tadami.preferences` | `CustomPreferences`, `CustomPreferencesIdentifier`, `SourcePreference` (the preference screen items), `CommonKeys` |
| `com.sf.tadami.utils`, `com.sf.tadami.ui.utils` | DataStore helpers (`editPreference`, `getPreferencesGroup`...), `Lang`, `parallelMap`, `detectSubtitleFormat` |

Every member has a KDoc with the lib version that added it: `@since extensions-lib 1.5` means 1.5 or earlier, the oldest version the app accepts. An extension using a member of a later version must declare that version.

Through Injekt (`Injekt.get<T>()` or `injectLazy()`), the app provides the `NetworkHelper` and a `kotlinx.serialization.json.Json`.

### Suspend methods or Rx fetches

The app calls the suspend methods of the sources, from a background thread: `getSearchAnime`, `getLatestAnime`, `getAnimeDetails`, `getEpisodeList` and `getEpisodeSources`. Each one defaults to its RxJava fetch, the API before lib 1.7: `fetchSearch`, `fetchLatest`, `fetchAnimeDetails`, `fetchEpisodesList` and `fetchEpisodeSources`, which `AnimeHttpSource` implements with its `...Request` and `...Parse` methods. So:

- an extension built against an older lib, which overrides the Rx fetches or the request and parse methods, works as before;
- a new extension (lib 1.7) overrides the suspend methods it needs, and calls `client.newCall(request).awaitSuccess()`. The Rx fetches are deprecated: they're the defaults of the suspend methods, not what the app calls. `fetchSearch` and `fetchLatest` stay abstract in `AnimeCatalogueSource`, and the request and parse methods in `AnimeHttpSource`: a source implementing only the suspend methods gives them a throwing body.

The Rx fetches take the app's `Anime`, the suspend methods an `SAnime`. A source with preferences is a `ConfigurableSource`: extend `ConfigurableParsedHttpAnimeSource` or, without the Jsoup selectors, `ConfigurableAnimeHttpSource`.

`NetworkHelper.cloudflareClient` is deprecated: `client` already solves the Cloudflare challenges.

## Rules for the stub

The app links extensions against its own classes, so the stub must never promise more than the app has:

- each class keeps the fully qualified name and the JVM signatures of the app's class it stands for. A member of the app can stay out of the stub, not the reverse;
- bodies throw `Exception("Stub!")`; the declared types are explicit (an inferred `Nothing` changes the JVM signature);
- no public `inline` function: its stub body would be compiled into the extensions;
- `jvmDefault` stays `ENABLE` in the lib and in `:app`: interface methods with a body are JVM default methods, plus `DefaultImpls` for the extensions compiled without them;
- a change of the app or of the stub must keep working for the extensions already published, built against an older lib;
- a member added to an interface has a body, so the extensions built before it still implement the interface;
- the suspend methods default to the Rx fetches, and `AnimeHttpSource` and `ParsedAnimeHttpSource` never override them with their own requests: the published extensions overriding the Rx fetches would be bypassed.

`ExtensionsApiCompatibilityTest` (`:app`, run with the unit tests) checks it by reading the class files, without loading them. For every public or protected class and member of the stub, and of each released lib version (the baselines `api/<major.minor>.txt`), the app must have:

1. the class, of the same kind, at least as visible, and not final or abstract where the stub's isn't;
2. its supertypes;
3. each method, resolved like the JVM does (the class, its superclasses, then its interfaces), with the same name, descriptor and static-ness, at least as visible, not final where the stub's is overridable, not abstract where the stub's has a body;
4. each field, likewise, not final where the stub's is writable, with the same constant value.

It also checks the stub has no public `inline` function, that `ExtensionsLoader.API_VERSION_MAX` is the lib's `major.minor` (`extensionsLibVersion` in `gradle.properties`), and that no class of the app outside `com.sf.tadami.source` calls the Rx fetches of a source: the app calls the suspend methods, see above.

The release build keeps the same classes: `:app:generateExtensionsApiKeepRules` writes an R8 rule keeping each class of the lib with its public and protected members. Don't add hand-written keep rules for them.

### When the test fails

Each line of the failure is a class or a member extensions link against, with what the app lacks, e.g.:

```
1.6.txt: com/sf/tadami/ui/components/data/DropDownAction method <init>(Ljava/lang/String;Lkotlin/jvm/functions/Function0;Z)V: missing from the app
```

Give the app back that JVM signature rather than changing the extensions:

- a removed or changed member: keep the old one beside the new one, with `@Deprecated(level = DeprecationLevel.HIDDEN)` (hidden from Kotlin callers, still in the class file);
- a parameter added with a default value: the HIDDEN overload with the old parameters, or `@JvmOverloads`;
- a renamed member: `@JvmName` with the old name;
- an abstract member extensions may not implement: `open`, with a body that throws;
- when the stub is the one that's wrong, fix the stub.

A break no published extension can meet (a member no extension calls) can go in `api/known-breaks.txt`: the line's key (the line without its flags, as the failure prints it) followed by the reason. The test fails on an entry that matches nothing.

### Baselines

`api/<major.minor>.txt` freezes what extensions built against that lib version may link against: one line per class, supertype and member. `1.5.txt` and `1.6.txt` come from the stub of tadami-extensions-source at the commits of those versions, `1.6.txt` merged with this lib's.

```
./gradlew :app:testDebugUnitTest --tests '*ExtensionsApiCompatibilityTest*' -Ptadami.updateApiBaseline=1.6
```

adds the current stub to `api/1.6.txt` (only the lib's own `major.minor` is accepted). It only adds lines, so the baseline keeps what earlier stubs of the version had: don't delete lines, list a break in `known-breaks.txt`. The release workflow checks the baseline of the version is committed.

## Releasing it

1. On `develop`: for an API change, raise `extensionsLibVersion` in `gradle.properties` and `ExtensionsLoader.API_VERSION_MAX` together (the test checks they match); for a fix of the lib alone, only the patch of `extensionsLibVersion`. Then update and commit the baseline (above).
2. Release the app first when its `API_VERSION_MAX` changed: extensions built against the new lib need an app that accepts them.
3. Bring `main` up to `develop`, then run Actions > Release extensions-lib > Run workflow, on `main`. It runs the drift test, checks the baseline is committed, builds the lib like JitPack, pushes the tag `lib-X.Y.Z`, has JitPack build it and waits for the artifact. Its summary gives the coordinates, or the JitPack build log when JitPack failed.

When JitPack fails, the tag stays, so the workflow refuses to run again for it. The build log is at `https://jitpack.io/com/github/tadamiorg/tadami/lib-X.Y.Z/build.log`. JitPack keeps serving a failed build: to build the tag again, sign in on [jitpack.io](https://jitpack.io/#tadamiorg/tadami) with GitHub, delete the failed `lib-X.Y.Z` build there, then request the POM (`https://jitpack.io/com/github/tadamiorg/tadami/lib-X.Y.Z/tadami-lib-X.Y.Z.pom`) until it answers 200. For `lib-1.6.0`, the builds on JitPack's "latest" build image failed to resolve the Android Gradle plugin, and the one on its "legacy" image worked. JitPack builds with `jitpack.yml`: JDK 17, and `JITPACK=true`, which leaves `:app` out of the build. The Extensions lib workflow runs the same build on the pushes and pull requests to `develop` that change the lib, the Gradle files or `jitpack.yml`.

### Developing against a local lib

```
./gradlew :extensions-lib:publishToMavenLocal
```

publishes `com.github.tadamiorg:tadami:lib-<extensionsLibVersion>` to `~/.m2`. Builds that list `mavenLocal()` before JitPack then use it instead of the released one: remove it from `~/.m2/repository/com/github/tadamiorg` afterwards.
