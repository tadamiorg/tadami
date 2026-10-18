# Building and releasing Tadami

Every secret, variable and option of the CI pipelines, for this repo and the extensions one, is listed in [CI_SETUP.md](CI_SETUP.md).

## Building locally

Use JDK 17. No secret is needed to build:

- `./gradlew :app:assembleDebug` builds a debug APK signed with the default Android debug keystore.
- `./gradlew :app:assembleRelease` builds an unsigned release APK (R8 included).

APKs are written to `app/build/outputs/apk/<buildType>/tadami.apk`.

### Optional: local release signing

Copy `.env.example` to `.env` (gitignored) and fill it in:

| Key | Value |
| --- | --- |
| `SIGNING_KEY` | Path to the release keystore, absolute or relative to `app/`. `TADAMI_KEYSTORE_PATH` works too and wins if both are set. |
| `KEY_STORE_PASSWORD` | Keystore password. |
| `KEY_PASSWORD` | Key password. Defaults to `KEY_STORE_PASSWORD` when empty. |
| `ALIAS` | Key alias. |

Real environment variables take precedence over `.env`. When the keystore file exists, release builds are signed with it, and so are debug builds, so a debug app installed from the maintainer's machine keeps its signature. When it does not, release builds are unsigned and debug builds use the default debug keystore.

## Continuous integration

`.github/workflows/ci.yml` runs on pushes and pull requests to `develop` (and manually):

- **build**: unit tests, debug build and unsigned release build. It uploads the debug APK and the test reports. Lint also runs, but it is informational only: lint errors do not fail the job (download the lint report artifact to see them).
- **instrumented**: the `androidTest` suite on an API 34 emulator, once the build job has passed.

`.github/workflows/extensions-lib.yml` (Extensions lib) runs on pushes and pull requests to `develop` that change `extensions-lib/`, the Gradle files or `jitpack.yml`: it builds the lib and publishes it to the local Maven repository the way JitPack does. The drift test between the lib and the app, `ExtensionsApiCompatibilityTest`, runs with the unit tests.

CI never uses the signing secrets.

## Release workflow

`.github/workflows/release.yml` runs from Actions > Release > Run workflow, on `main` only. It:

1. computes the version from the commits since the last `vX.Y.Z` tag (`.github/scripts/next_version.py`, see "Versions" below) and fails when there are none, or when none of them changes `app/`, `gradle/` or the root Gradle files;
2. decodes the keystore from the secrets, runs the unit tests and builds the signed release APK with that version;
3. checks the APK's `versionName` is that version, verifies the signature with `apksigner` and, when `RELEASE_CERT_SHA256` is set, fails if the APK is signed with another certificate;
4. pushes the tag `vX.Y.Z` on the released commit and creates the GitHub release `Tadami vX.Y.Z`, as a draft by default, with notes built from the commits, a SHA-256 checksum and `tadami-vX.Y.Z.apk` as its only asset;
5. keeps the R8 mapping as the workflow artifact `mapping-vX.Y.Z` (not as a release asset).

The release is created as a **draft**: users don't get it until you publish it. Open it in the repository's Releases page, check the notes and the APK, then Edit > Publish release, keeping "Set as the latest release" checked. From then on, the in-app updater offers it. To publish right away instead, set the `RELEASE_DRAFT` variable to `false`.

### Versions

Nothing in the repo holds the version: the workflow computes it, and passes it to Gradle as `-PappVersionName=X.Y.Z`. From the commits since the last tag, merge commits aside, with the `bump` input left on `auto`:

- a breaking change, `feat!: ...` or `fix(scope)!: ...` or a `BREAKING CHANGE:` line in the body, bumps the major version (1.10.2 to 2.0.0);
- otherwise a `feat` bumps the minor version (1.10.2 to 1.11.0);
- otherwise anything else (`fix`, `chore`, ...) bumps the patch version (1.10.2 to 1.10.3).

Pick `patch`, `minor` or `major` in the `bump` input to force one. The release notes list the breaking changes, the `feat` commits (under "New") and the `fix` and `perf` commits (under "Fixes"), so write their subjects for users.

The version code is derived from the version, `major * 1000000 + minor * 1000 + patch` (1.10.0 is 1010000), so it always increases with the version. Codes up to 52 (1.9.3) were set by hand. Builds outside the workflow use the latest `vX.Y.Z` tag of the checkout (debug builds show it as `1.10.0-DEBUG`), or 0.0.1 without tags.

### Required secrets

Set them in Settings > Secrets and variables > Actions > Secrets:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | The existing release keystore, base64 encoded on a single line: `base64 -w0 release.jks` (PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks"))`). |
| `KEY_STORE_PASSWORD` | Keystore password. |
| `KEY_PASSWORD` | Key password. Optional: defaults to `KEY_STORE_PASSWORD`. |
| `ALIAS` | Key alias. |

Always use the keystore of the previous releases (the file `SIGNING_KEY` points to in the local `.env`). Android refuses to update an app signed with another certificate: users would have to uninstall Tadami, and lose their data, to install the new version.

### Optional variables

Set them in Settings > Secrets and variables > Actions > Variables:

| Variable | Value |
| --- | --- |
| `RELEASE_CERT_SHA256` | SHA-256 digest of the release signing certificate. Recommended: it guards against publishing an APK signed with the wrong key. Get it with `apksigner verify --print-certs tadami.apk` (line `certificate SHA-256 digest`) or `keytool -list -v -keystore release.jks -alias <alias>` (colons and case are ignored). |
| `RELEASE_DRAFT` | `false` to publish the release right away. Unset or any other value: the release is created as a draft, to review and publish by hand. Drafts are not visible to the updater until published. |

## Release flow

1. Bring `main` up to `develop`: `git push origin develop:main` fast-forwards it when `main` has nothing `develop` lacks. Squash-merging a pull request instead gives `main` a commit `develop` doesn't have.
2. Actions > Release > Run workflow, with "Use workflow from" on `main` and `bump` on `auto`. The "Compute the version" step shows the version and the notes.
3. Once the run is green, open the draft release, check it and publish it (see above). The release is then live: the app checks for updates at launch (at most every 3 days), or right away from More > About > Check for updates.

If the run fails before the "Tag the release" step, nothing was created: fix the cause and run it again. If it fails after, the tag exists, so a new run would see no commits since it: delete the tag (`git push --delete origin v1.10.0`) and run it again. Discarding a draft release works the same way: delete the draft and its tag, or the next release skips that version.

## Releasing extensions-lib

The extension API, `extensions-lib/`, is released on its own: the tag `lib-X.Y.Z`, which JitPack builds into `com.github.tadamiorg:tadami:lib-X.Y.Z`. These tags never count as app releases: the app's versions only come from the `vX.Y.Z` tags.

1. On `develop`, set the version: `extensionsLibVersion` in `extensions-lib/gradle.properties`, and for an API change `ExtensionsLoader.API_VERSION_MAX`, to its `major.minor` (the drift test checks they match). Add the stub to the baseline of the version and commit `extensions-lib/api/X.Y.txt`:
   ```
   ./gradlew :app:testDebugUnitTest --tests '*ExtensionsApiCompatibilityTest*' -Ptadami.updateApiBaseline=X.Y
   ```
2. When `API_VERSION_MAX` changed, release the app first: extensions built against the new lib need an app that accepts them.
3. Bring `main` up to `develop`, then Actions > Release extensions-lib > Run workflow, with "Use workflow from" on `main`. It fails when the tag `lib-X.Y.Z` already exists. Otherwise it runs the drift test, checks the baseline is committed, builds the lib the way JitPack does, pushes the annotated tag `lib-X.Y.Z`, then asks JitPack for the artifact and waits for it, up to 20 minutes. The run summary gives the coordinates and the `compileOnly` line.

When JitPack fails or times out, the run fails with the link to the build log, `https://jitpack.io/com/github/tadamiorg/tadami/lib-X.Y.Z/build.log`, and the tag stays. Running the workflow again doesn't help: it refuses the existing tag. Once the cause is fixed on JitPack's side:

1. JitPack keeps serving the failed build of a tag until it's deleted. Sign in on [jitpack.io](https://jitpack.io/#tadamiorg/tadami) with GitHub, open `tadamiorg/tadami` and delete the failed `lib-X.Y.Z` build (only a signed-in maintainer can).
2. Request the POM to build it again, and repeat until it answers 200: `curl -sI https://jitpack.io/com/github/tadamiorg/tadami/lib-X.Y.Z/tadami-lib-X.Y.Z.pom`.

If the lib itself must change, fix it, raise the patch of `extensionsLibVersion` and run the workflow again.

For `lib-1.6.0`, JitPack's builds on its "latest" build image failed to resolve the Android Gradle plugin, and the build on its "legacy" image worked: when a log shows that error, check which image built it.

What the lib must follow, and how to fix a drift test failure, is in [extensions-lib/README.md](../extensions-lib/README.md).

## In-app updater contract

Keep these in mind when changing the workflow or the release process. Already installed versions rely on them:

- the app reads `GET https://api.github.com/repos/tadamiorg/tadami/releases/latest`, which only returns published releases that are neither drafts nor prereleases;
- the tag is compared with the installed `versionName`, so it must be `v<versionName>` (the workflow builds the APK with the version of the tag it creates, and checks it);
- the release body is shown as the changelog;
- the APK must be the first (ideally the only) asset: versions up to 1.9.3 download `assets[0]`, newer ones the first `.apk` asset;
- the APK must be signed with the same key as the installed app.

Debug builds never check for updates.
