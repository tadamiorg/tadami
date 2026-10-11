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

CI never uses the signing secrets.

## Release workflow

`.github/workflows/release.yml` runs when a `v*` tag is pushed. It can also be started from Actions > Release > Run workflow with an existing tag. It:

1. checks that the tag is exactly `v<versionName>` (the `versionName` in `app/build.gradle.kts`) and fails otherwise;
2. decodes the keystore from the secrets, runs the release unit tests and builds the signed release APK;
3. verifies the signature with `apksigner` and, when `RELEASE_CERT_SHA256` is set, fails if the APK is signed with another certificate;
4. creates the GitHub release `Tadami vX.Y.Z`, as a draft by default, with generated release notes, a SHA-256 checksum and `tadami-vX.Y.Z.apk` as its only asset;
5. keeps the R8 mapping as the workflow artifact `mapping-vX.Y.Z` (not as a release asset).

The release is created as a **draft**: users don't get it until you publish it. Open it in the repository's Releases page, check the notes and the APK, then Edit > Publish release, keeping "Set as the latest release" checked for a normal version. From then on, the in-app updater offers it. To publish right away instead, set the `RELEASE_DRAFT` variable to `false`. Tags containing a dash (for example `v2.0.0-beta1`) become prereleases, which the updater ignores.

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

1. On `develop`, bump `versionCode` (it must always increase) and `versionName` in `app/build.gradle.kts`, and the version in the README title. Commit and push.
2. Merge `develop` into `main`.
3. Tag `main` and push the tag:

   ```sh
   git checkout main
   git pull
   git tag v1.9.4
   git push origin v1.9.4
   ```

4. Follow the Release workflow in the Actions tab. Once it is green, open the draft release, check it and publish it (see above). The release is then live: the app checks for updates at launch (at most every 3 days), or right away from More > About > Check for updates.

If the workflow fails before publishing, fix the cause and run it again from Actions > Release > Run workflow with the same tag. If the fix needs a new commit, move the tag to it first (`git tag -f v1.9.4` then `git push -f origin v1.9.4`), which also starts the workflow again.

## In-app updater contract

Keep these in mind when changing the workflow or the release process. Already installed versions rely on them:

- the app reads `GET https://api.github.com/repos/tadamiorg/tadami/releases/latest`, which only returns published releases that are neither drafts nor prereleases;
- the tag is compared with the installed `versionName`, so it must be `v<versionName>`;
- the release body is shown as the changelog;
- the APK must be the first (ideally the only) asset: versions up to 1.9.3 download `assets[0]`, newer ones the first `.apk` asset;
- the APK must be signed with the same key as the installed app.

Debug builds never check for updates.
